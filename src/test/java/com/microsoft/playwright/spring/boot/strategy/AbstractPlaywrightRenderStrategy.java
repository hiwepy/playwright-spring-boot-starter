package com.microsoft.playwright.spring.boot.strategy;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.ResourceType;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import com.microsoft.playwright.spring.boot.util.ImageUtil;
import com.microsoft.playwright.spring.boot.util.TimeUtil;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import hitool.core.lang3.StringUtils;
import hitool.core.lang3.time.DateFormats;
import hitool.core.lang3.uid.Sequence;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.util.Base64Utils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 抽象的 Playwright 处理策略
 */
@Slf4j
public abstract class AbstractPlaywrightRenderStrategy<B extends WkhtmlRenderBO> implements PlaywrightRenderStrategy<B>, InitializingBean, ApplicationEventPublisherAware {

    protected static final int MIN_QUALITY = 0;
    protected static final int MAX_QUALITY = 100;
    protected static final int MIN_SCALE = 0;
    protected static final int MAX_SCALE = 100;
    protected static final int ONE_KB = 1024;

    protected static final String DATE_PATTERN = "yyyyMMddHHmmssS";
    protected static final String REPORT_URLS_PARAM_NAME = "report_urls";
    protected static final DateTimeFormatter FILE_NAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    protected static final DateTimeFormatter DIRECTORY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateFormats.DATE_FORMAT_TWO);

    @Resource
    protected Sequence sequence;
    @Autowired
    protected PlaywrightProperties playwrightProperties;
    @Autowired
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Autowired
    protected BrowserPagePool browserPagePool;
    @Autowired
    protected BrowserContextPool browserContextPool;
    @Resource
    protected ThreadPoolExecutor dtpToImageExecutor;
    @Resource
    protected ThreadPoolExecutor dtpToImageCompressExecutor;
    @Resource
    protected ThreadPoolExecutor dtpToImageZipExecutor;
    @Resource
    protected ThreadPoolExecutor dtpToPdfExecutor;
    @Resource
    protected ThreadPoolExecutor dtpToPdfMergeExecutor;

    protected ApplicationEventPublisher eventPublisher;
    protected Function<BufferTemp, Boolean> customPresentable;

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void customPresentable(Function<BufferTemp, Boolean> function) {
        this.customPresentable = function;
    }

    @Override
    public WkhtmlRenderResultVO render(B renderBO) throws Exception {
        log.info("=================Playwright 渲染 HTML:开始=================");
        String randerId = String.valueOf(getSequence().nextId());
        StopWatch stopWatch = new StopWatch(randerId);
        renderBO.setRanderId(randerId);
        WkhtmlRenderResultVO resultBO = null;
        try {
            // 1、执行参数校验
            if(StringUtils.hasText(renderBO.getParam())){
                String param = new String(Base64Utils.decodeFromString(renderBO.getParam()), Charset.defaultCharset());
                JSONObject jsonObject = JSON.parseObject(param);
                List<String> urls = jsonObject.getList(REPORT_URLS_PARAM_NAME, String.class);
                if(CollectionUtils.isEmpty(urls)){
                    throw new PlaywrightException("report_urls is empty");
                }
                // 网页URL数组
                List<BufferTemp> tempList = Lists.newArrayList();
                for (int i = 0; i < urls.size(); i++) {
                    tempList.add(BufferTemp.builder().index(i).url(Objects.toString(playwrightRenderProperties.getUrlPrefix(), StringUtils.EMPTY) + urls.get(i)).build());
                }
                renderBO.setUrls(tempList);
            }
            renderBO.setCompress(Objects.nonNull(renderBO.getCompress()) ? renderBO.getCompress() : Boolean.FALSE);
            renderBO.setToFile(Objects.nonNull(renderBO.getToFile()) ? renderBO.getToFile() : Boolean.FALSE);
            // 2、执行内容生成逻辑
            List<BufferTemp> fileBuffers;
            try {
                log.info("=================Playwright 渲染 PDF/Image:开始=================");
                stopWatch.start("Playwright 渲染 PDF/Image");
                fileBuffers = this.doGenerate(renderBO);
            } finally {
                stopWatch.stop();
                log.info("=================Playwright 渲染 PDF/Image:结束=================");
            }
            // 2、执行打包内容生成逻辑
            try {
                log.info("=================压缩 Image:开始=================");
                stopWatch.start("Playwright 渲染 PDF/Image");
                fileBuffers = this.doCompress(renderBO, fileBuffers);
            } finally {
                stopWatch.stop();
                log.info("=================Playwright 渲染 PDF/Image:结束=================");
            }
            // 3、执行打包逻辑
            try {
                log.info("=================PDF/Image文件zip压缩:开始=================");
                stopWatch.start("PDF/Image文件zip压缩");
                resultBO = this.doPacking(renderBO, fileBuffers);
            } finally {
                stopWatch.stop();
                log.info("=================PDF/Image文件zip压缩:结束=================");
            }
            return resultBO;
        } catch (Exception e) {
            this.afterException(renderBO, resultBO);
            throw e;
        } finally {
            if(stopWatch.isRunning()){
                stopWatch.stop();
            }
            log.info(stopWatch.prettyPrint());
            log.info("=================Playwright 渲染 HTML:结束=================");
        }
    }

    protected abstract List<BufferTemp> doGenerate(B renderBO) throws IOException;

    protected abstract List<BufferTemp> doCompress(B renderBO, List<BufferTemp> urlTemps) throws IOException;

    protected abstract WkhtmlRenderResultVO doPacking(B renderBO, List<BufferTemp> urlTemps) throws IOException;

    protected void afterException(B renderBO, WkhtmlRenderResultVO resultBO) throws IOException {
        this.cleanTemporary(renderBO, resultBO);
    }

    protected final CompletableFuture<BufferTemp> captureScreenshotAsync(String rendeId, BufferTemp urlTemp, String selector){
        if(playwrightRenderProperties.isIsolated()){
            return this.captureScreenshotAsync1(rendeId, urlTemp, selector);
        }
        return this.captureScreenshotAsync2(rendeId, urlTemp, selector);
    }

    protected CompletableFuture<BufferTemp> captureScreenshotAsync1(String rendeId, BufferTemp urlTemp, String selector){
        if(StringUtils.isBlank(urlTemp.getUrl())){
            return CompletableFuture.completedFuture(urlTemp);
        }
        //HttpServletRequest request = WebUtils.getHttpServletRequest();
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            // Page page = null;
            BrowserContext browserContext = null;
            try {
                browserContext = browserContextPool.borrowObject();
                try(Page page = browserContext.newPage()) {
                    // 跳转到url
                    log.info("Async Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
                    BufferTemp pageScreenshot = this.loadPageWithCallback(page, urlTemp, this.doPageScreenShot(rendeId, selector));
                    log.info("Async Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
                    // 如果许重新截图，则重试截图
                    // AtomicInteger screenshotRetry = new AtomicInteger(0);
                    // 如果截图文件大小小于指定大小，则重试截图
                    /*while ( urlTemp.getFileSize() < lowerLimit && screenshotRetry.incrementAndGet() < playwrightRenderProperties.getRetryLimit()) {
                        log.info("Retry Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
                        pageScreenshot = doPageScreenShot(rendeId, urlTemp, selector);
                        log.info("Retry Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
                    }*/
                    return pageScreenshot;
                }
            } catch (Exception e) {
                log.error("Async Capture screenshot error: ", e);
                throw new PlaywrightException("Async Capture screenshot error", e);
            } finally {
                if(Objects.nonNull(browserContext)){
                    browserContextPool.returnObject(browserContext);
                }
            }
        }, dtpToImageExecutor);
    }

    protected CompletableFuture<BufferTemp> captureScreenshotAsync2(String rendeId, BufferTemp urlTemp, String selector){
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            Page page = null;
            try  {
                page = browserPagePool.borrowObject();
                // 跳转到url
                log.info("Sync Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
                BufferTemp pageScreenshot = this.loadPageWithCallback(page, urlTemp, this.doPageScreenShot(rendeId, selector));
                log.info("Sync Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
                // 如果许重新截图，则重试截图
                // AtomicInteger screenshotRetry = new AtomicInteger(0);
                // 如果截图文件大小小于指定大小，则重试截图
                /*while ( urlTemp.getFileSize() < lowerLimit && screenshotRetry.incrementAndGet() < playwrightRenderProperties.getRetryLimit()) {
                    log.info("Retry Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
                    pageScreenshot = doPageScreenShot(rendeId, urlTemp, selector);
                    log.info("Retry Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
                }*/
                return pageScreenshot;
            } catch (Exception e) {
                log.error("Sync Capture screenshot error: ", e);
                throw new PlaywrightException("Sync Capture screenshot error", e);
            } finally {
                if(Objects.nonNull(page)){
                    browserPagePool.returnObject(page);
                }
            }
        }, dtpToImageExecutor);
    }

    protected BufferTemp captureScreenshotSync(Browser browser, String rendeId, BufferTemp urlTemp, String selector){
        try (Page page = browser.newPage()) {
            // 跳转到url
            log.info("Sync Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
            BufferTemp pageScreenshot = this.loadPageWithCallback(page, urlTemp, this.doPageScreenShot(rendeId, selector));
            log.info("Sync Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
            // 如果许重新截图，则重试截图
            // AtomicInteger screenshotRetry = new AtomicInteger(0);
            // 如果截图文件大小小于指定大小，则重试截图
            /*while ( urlTemp.getFileSize() < lowerLimit && screenshotRetry.incrementAndGet() < playwrightRenderProperties.getRetryLimit()) {
                log.info("Retry Capturing screenshot start for rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
                pageScreenshot = doPageScreenShot(rendeId, urlTemp, selector);
                log.info("Retry Capturing screenshot completed for rendeId: {}, selector: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, selector, urlTemp.getUrl(), pageScreenshot.getName(), pageScreenshot.getFileSize()/ONE_KB);
            }*/
            return pageScreenshot;
        } catch (Exception e) {
            log.error("Sync Capture screenshot error: ", e);
            throw new PlaywrightException("Sync Capture screenshot error", e);
        }
    }


    /**
     * 判断截图/生成PDF是否可用
     * @param urlTemp url信息
     * @return 是否可用
     */
    protected boolean isPresentable(BufferTemp urlTemp){
        // 如果path为空，且buffer为空，则不符合要求
        if(StringUtils.isBlank(urlTemp.getPath()) && Objects.isNull(urlTemp.getBuffer())){
            return false;
        }
        // 如果path不为空，且文件不存在，则不符合要求
        if(StringUtils.isNotBlank(urlTemp.getPath())){
            File screenshotFile = new File(urlTemp.getPath());
            if(!screenshotFile.exists()){
                return false;
            }
        }
        // 如果截图文件大小小于指定大小，则不符合要求
        Long lowerLimit = Math.max(ImageUtil.WHITE_A4_SIZE, ONE_KB * playwrightRenderProperties.getLowerLimit());
        if(urlTemp.getFileSize() < lowerLimit){
            return false;
        }
        // 如果自定义判断方法，则执行自定义判断方法
        if(Objects.nonNull(customPresentable)){
            return customPresentable.apply(urlTemp);
        }
        return true;
    }

    protected BufferTemp loadPageWithCallback(Page page, BufferTemp urlTemp, BiFunction<Page, BufferTemp, BufferTemp> callback) throws Exception {
        urlTemp.setFileSize(0L);
        urlTemp.setNeedReload(false);
        urlTemp.setReload(false);
        urlTemp.setReloadTimeout(playwrightProperties.getPageNavigateOptions().getTimeout());

        page.onLoad(page1 -> {
            if(urlTemp.isReload()){
                log.debug("Reload page for : {}", page1.url());
            } else {
                log.debug("Load page for : {}", page1.url());
            }
        });
        page.onRequest(request -> {
            if(urlTemp.isReload()){
                log.debug("Reload Request url: {}, resource type：{}, method：{}, headers：{}, postData：{}", request.url(), request.resourceType(),
                        request.method(), request.headers(), request.postData());
            } else {
                log.debug("Request url: {}, resource type：{}, method：{}, headers：{}, postData：{}", request.url(), request.resourceType(),
                        request.method(), request.headers(), request.postData());
            }
        });
        page.onRequestFailed(request -> {
            if(urlTemp.isReload()){
                log.error("Reload Request failed: {}, resource type：{}, reason：{}", request.url(), request.resourceType(), request.failure());
            } else {
                log.error("Request failed: {}, resource type：{}, reason：{}", request.url(), request.resourceType(), request.failure());
            }
            // 渲染引擎所感知的请求的资源类型。ResourceType将是以下之一： document, stylesheet, image, media, font, script, texttrack, xhr, fetch, eventsource, websocket, manifest, other.
            ResourceType resourceType = ResourceType.getByName(request.resourceType());
            if (Objects.nonNull(resourceType) && resourceType.isNeedRetry()) {
                urlTemp.setNeedReload(true);
                log.debug("Page need reload for url : {}", page.url());
            }
        });
        page.onResponse(response -> {
            if(urlTemp.isReload()){
                log.debug("Reload Response url: {}, status：{}, headers：{}", response.url(), response.status(), response.headers());
            } else {
                log.debug("Response url: {}, status：{}, headers：{}", response.url(), response.status(), response.headers());
            }
        });
        page.onPageError(exception -> {
            log.error("page error: {}", exception);
            urlTemp.setNeedReload(true);
            log.debug("page need retry for url : {}", page.url());
        });
        page.onCrash(page1 -> {
            log.error("page crash for url : {}", page1.url());
            urlTemp.setNeedReload(true);
            log.debug("page crash and need reload for url : {}", page1.url());
        });
        // 设置页面加载参数, 并跳转到url
        Page.NavigateOptions navigateOptions = playwrightProperties.getPageNavigateOptions().toOptions();
        page.navigate(urlTemp.getUrl(), navigateOptions);
        // 如果设置了加载等待时间，则等待一段时间
        if(playwrightRenderProperties.isLoadWait() && Objects.nonNull(playwrightRenderProperties.getLoadWaitDuration()) && playwrightRenderProperties.getLoadWaitDuration().toMillis() > 0){
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.debug("The page load wait {} milliseconds for : {}", playwrightRenderProperties.getLoadWaitDuration().toMillis(), page.url());
                    Thread.sleep(playwrightRenderProperties.getLoadWaitDuration().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Thread was interrupted", e);
                }
            });
            // 等待异步任务完成
            future.join();
            log.debug("The page load wait completed for : {}", page.url());
        }
        log.debug("The page load completed for : {}", page.url());
        // 执行回调函数（截图、单页生成pdf）
        BufferTemp applyTemp = callback.apply(page, urlTemp);
        // 判断回调处理后的结果是否可用，如果可用则返回，无需在进行重试
        if(this.isPresentable(applyTemp)){
            return applyTemp;
        } else {
            urlTemp.setNeedReload(true);
        }
        // 结果不符合要求，补充重试机制，多次打开页面
        AtomicInteger loadRetry = new AtomicInteger(0);
        while ( playwrightRenderProperties.isReloadAble() && urlTemp.isNeedReload() && loadRetry.incrementAndGet() < playwrightRenderProperties.getReloadLimit()) {
            // 动态调整超时时间
            if(Objects.nonNull(urlTemp.getReloadTimeout())){
                urlTemp.setReloadTimeout(TimeUtil.getRetryTimeout(urlTemp.getReloadTimeout()));
            }
            // 重置重新加载标识
            urlTemp.setNeedReload(false);
            log.debug("The page reloading for : {} , reloadTimes: {}, reloadTimeout: {}", page.url(), loadRetry.get(), urlTemp.getReloadTimeout());
            Page.ReloadOptions reloadOptions = new Page.ReloadOptions()
                    .setTimeout(urlTemp.getReloadTimeout())
                    .setWaitUntil(playwrightProperties.getPageNavigateOptions().getWaitUntil());
            page.reload(reloadOptions);
            // 设置重新加载状态为false
            urlTemp.setReload(false);
            // 如果是重试，则等待一段时间
            if(playwrightRenderProperties.isReloadWait() && Objects.nonNull(playwrightRenderProperties.getReloadWaitDuration()) && playwrightRenderProperties.getReloadWaitDuration().toMillis() > 0){
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        log.debug("The page reload wait {} milliseconds for : {}", playwrightRenderProperties.getReloadWaitDuration().toMillis(), page.url());
                        Thread.sleep(playwrightRenderProperties.getReloadWaitDuration().toMillis());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Thread was interrupted", e);
                    }
                });
                // 等待异步任务完成
                future.join();
                log.debug("The page reload wait completed for : {}", page.url());
            }
            log.debug("The page reload completed for : {} , reloadTimes: {}, reloadTimeout: {}", page.url(), loadRetry.get(), urlTemp.getReloadTimeout());
            // 判断重新加载，再次处理后的结果是否可用，如果可用则返回，无需在进行重试
            if(this.isPresentable(applyTemp)){
                urlTemp.setNeedReload(false);
                return applyTemp;
            } else {
                urlTemp.setNeedReload(true);
            }
        }
        return urlTemp;
    }

    protected BiFunction<Page, BufferTemp, BufferTemp> doPageScreenShot(String rendeId, String selector){
        return (page, urlTemp) -> {
            // 定义截图输出路径
            String fileName = String.format("%s.png", urlTemp.getIndex());
            urlTemp.setName(fileName);
            // 截图
            if(StringUtils.isEmpty(selector)){
                Page.ScreenshotOptions screenshotOptions = playwrightProperties.getPageScreenshotOptions().toOptions();
                if(playwrightRenderProperties.isWriteToFile()){
                    File screenshotFile = new File(playwrightRenderProperties.getTmpDir(), rendeId + File.separator + fileName);
                    log.info("Page screenshot start for rendeId : {}, renderType : {}, to path: {}", rendeId, getRenderType(), screenshotFile.getAbsolutePath());
                    screenshotOptions.setPath(screenshotFile.toPath());
                    page.screenshot(screenshotOptions);
                    urlTemp.setPath(screenshotFile.getAbsolutePath());
                    urlTemp.setFileSize(screenshotFile.length());
                    log.info("Page screenshot success for rendeId : {}, renderType : {}, to path: {}, fileSize: {}KB", rendeId, getRenderType(), screenshotFile.getAbsolutePath(), urlTemp.getFileSize() / ONE_KB);
                } else {
                    log.info("Page screenshot start for rendeId : {}, renderType : {}, to buffer", rendeId, getRenderType());
                    byte[] screenshotBuffer = page.screenshot(screenshotOptions);
                    urlTemp.setBuffer(screenshotBuffer);
                    urlTemp.setFileSize((long) screenshotBuffer.length);
                    log.info("Page screenshot success for rendeId : {}, renderType : {}, to buffer, fileSize: {}KB", rendeId, getRenderType(), urlTemp.getFileSize() / ONE_KB);
                }
            } else {
                // 定位到要截图的元素
                ElementHandle element = page.querySelector(selector);
                if(Objects.nonNull(element)) {
                    if (element.isVisible()) {
                        // 滚动到元素位置
                        element.scrollIntoViewIfNeeded();
                    }
                    ElementHandle.ScreenshotOptions screenshotOptions = playwrightProperties.getElementScreenshotOptions().toOptions();
                    if(playwrightRenderProperties.isWriteToFile()){
                        File screenshotFile = new File(playwrightRenderProperties.getTmpDir(), rendeId + File.separator + fileName);
                        log.info("Page screenshot start for rendeId : {}, renderType : {}, with selector : {}, to path: {}", rendeId, getRenderType(), selector, screenshotFile.getAbsolutePath());
                        screenshotOptions.setPath(screenshotFile.toPath());
                        element.screenshot(screenshotOptions);
                        urlTemp.setPath(screenshotFile.getAbsolutePath());
                        urlTemp.setFileSize(screenshotFile.length());
                        log.info("Page screenshot success for rendeId : {}, renderType : {}, with selector: {}, to path: {}, fileSize: {}KB", rendeId, getRenderType(), selector, screenshotFile.getAbsolutePath(), urlTemp.getFileSize() / ONE_KB);
                    } else {
                        log.info("Page screenshot start for rendeId : {}, renderType : {}, with selector : {}, to buffer", rendeId, getRenderType(), selector);
                        byte[] screenshotBuffer = element.screenshot(screenshotOptions);
                        urlTemp.setBuffer(screenshotBuffer);
                        urlTemp.setFileSize((long) screenshotBuffer.length);
                        log.info("Page screenshot success for rendeId : {}, renderType : {}, with selector: {}, to buffer, fileSize: {}KB", rendeId, getRenderType(), selector,  urlTemp.getFileSize() / ONE_KB);
                    }
                } else {
                    log.error("element not found for selector: {}, url : {}", selector, urlTemp.getUrl());
                }
            }
            return urlTemp;
        };
    }

    protected final CompletableFuture<BufferTemp> pageToPdfFutureAsync(String rendeId, BufferTemp urlTemp) {
        if(playwrightRenderProperties.isIsolated()){
            return this.pageToPdfFutureAsync1(rendeId, urlTemp);
        }
        return this.pageToPdfFutureAsync2(rendeId, urlTemp);
    }

    protected CompletableFuture<BufferTemp> pageToPdfFutureAsync1(String rendeId, BufferTemp urlTemp) {
        if(StringUtils.isBlank(urlTemp.getUrl())){
            return CompletableFuture.completedFuture(urlTemp);
        }
        //HttpServletRequest request = WebUtils.getHttpServletRequest();
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            // Page page = null;
            BrowserContext browserContext = null;
            try {
                browserContext = browserContextPool.borrowObject();
                try(Page page = browserContext.newPage()) {
                    log.info("Async Generate pdf start for rendeId: {}, url : {}", rendeId, urlTemp.getUrl());
                    BufferTemp pageToPdf = this.loadPageWithCallback(page, urlTemp, this.doPageToPdf(rendeId));
                    log.info("Async Generate pdf completed for rendeId: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, urlTemp.getUrl(), pageToPdf.getName(), pageToPdf.getFileSize()/ONE_KB);
                    return pageToPdf;
                } catch (Exception e) {
                    log.error("Async Generate pdf error: ", e);
                    throw new PlaywrightException("Async Generate pdf error", e);
                }
            } catch (Exception e) {
                log.error("Browser Context error: ", e);
                throw new PlaywrightException("Browser Context error", e);
            } finally {
                if(Objects.nonNull(browserContext)){
                    browserContextPool.returnObject(browserContext);
                }
            }
        }, dtpToPdfExecutor);
    }

    protected CompletableFuture<BufferTemp> pageToPdfFutureAsync2(String rendeId, BufferTemp urlTemp) {
        if(StringUtils.isBlank(urlTemp.getUrl())){
            return CompletableFuture.completedFuture(urlTemp);
        }
        //HttpServletRequest request = WebUtils.getHttpServletRequest();
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            Page page = null;
            try  {
                page = browserPagePool.borrowObject();
                log.info("Async Generate pdf start for rendeId: {}, url : {}", rendeId, urlTemp.getUrl());
                BufferTemp pageToPdf = this.loadPageWithCallback(page, urlTemp, this.doPageToPdf(rendeId));
                log.info("Async Generate pdf completed for rendeId: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, urlTemp.getUrl(), pageToPdf.getName(), pageToPdf.getFileSize()/ONE_KB);
                return pageToPdf;
            } catch (Exception e) {
                log.error("Async Generate pdf error: ", e);
                throw new PlaywrightException("Async Generate pdf error", e);
            } finally {
                if(Objects.nonNull(page)){
                    browserPagePool.returnObject(page);
                }
            }
        }, dtpToPdfExecutor);
    }

    protected BufferTemp pageToPdfFutureSync(Browser browser, String rendeId, BufferTemp urlTemp) {
        try (Page page = browser.newPage()) {
            log.info("Sync Generate pdf start for rendeId: {}, url : {}", rendeId, urlTemp.getUrl());
            BufferTemp pageToPdf = this.loadPageWithCallback(page, urlTemp, this.doPageToPdf(rendeId));
            log.info("Sync Generate pdf completed for rendeId: {}, url : {}, pageName: {}, fileSize: {}KB", rendeId, urlTemp.getUrl(), pageToPdf.getName(), pageToPdf.getFileSize()/ONE_KB);
            return pageToPdf;
        } catch (Exception e) {
            log.error("Sync Generate pdf error: ", e);
            throw new PlaywrightException("Sync Generate pdf error", e);
        }
    }

    protected BiFunction<Page, BufferTemp, BufferTemp> doPageToPdf(String rendeId) {
        return (page, urlTemp) -> {
            try {
                // 定义截图输出路径
                String fileName = String.format("%s.pdf", urlTemp.getIndex());
                urlTemp.setName(fileName);
                page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.SCREEN));
                if(playwrightRenderProperties.isWriteToFile()){
                    File pdfFile = new File(playwrightRenderProperties.getTmpDir(), rendeId + File.separator + fileName);
                    log.info("Generate pdf file start for rendeId : {}, renderType : {}, to path: {}", rendeId, getRenderType(), pdfFile.getAbsolutePath());
                    Page.PdfOptions pdfOptions = playwrightProperties.getPagePdfOptions().toOptions();
                    pdfOptions.setPath(pdfFile.toPath());
                    page.pdf(pdfOptions);
                    urlTemp.setPath(pdfFile.getAbsolutePath());
                    urlTemp.setFileSize(pdfFile.length());
                    log.info("Generate pdf file success for rendeId : {}, renderType : {}, to path: {}, fileSize: {}KB", rendeId, getRenderType(), pdfFile.getAbsolutePath(), urlTemp.getFileSize() / ONE_KB);
                } else {
                    log.info("Generate pdf buffer start for rendeId : {}, renderType : {}", rendeId, getRenderType());
                    // 生成PDF
                    Page.PdfOptions pdfOptions = playwrightProperties.getPagePdfOptions().toOptions();
                    byte[] pdfBuffer = page.pdf(pdfOptions);
                    urlTemp.setBuffer(pdfBuffer);
                    urlTemp.setFileSize((long) pdfBuffer.length);
                    log.info("Generate pdf buffer success for rendeId : {}, renderType : {}, fileSize: {}KB", rendeId, getRenderType(), urlTemp.getFileSize() / ONE_KB);
                }
                return urlTemp;
            } catch (Exception e) {
                log.error("Generate PDF error: ", e);
            }
            return urlTemp;
        };
    }

    @Override
    public void cleanTemporary(B renderBO, WkhtmlRenderResultVO resultBO) {
        log.info("clean Temporary");
        try {
            if(StringUtils.hasText(renderBO.getRanderId())){
                File fileDirectory = new File(playwrightRenderProperties.getTmpDir(), renderBO.getRanderId());
                if(fileDirectory.exists()){
                    log.info("delete Temporary Directory  : {}" , fileDirectory.getAbsolutePath());
                    FileUtils.deleteDirectory(fileDirectory);
                }
            }
            if(Objects.nonNull(resultBO) && StringUtils.hasText(resultBO.getFilePath())){
                File rtFile = new File(resultBO.getFilePath());
                log.info("delete Temporary File  : {}" , rtFile.getAbsolutePath());
                if(rtFile.exists()){
                    Files.delete(rtFile.toPath());
                }
            }
        } catch (Exception e){
            log.error("Failed to delete file", e);
        }
    }

    protected ApplicationEventPublisher getEventPublisher() {
        return eventPublisher;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    protected Sequence getSequence() {
        return sequence;
    }


}
