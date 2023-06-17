package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Media;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
public class PlaywrightApplication_Test1 implements CommandLineRunner {

    protected static final String BASE_DIR = "D://tmp";
    @Autowired
    private BrowserContextPool browserContextPool;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PlaywrightApplication_Test1.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            FileUtils.forceMkdir( new File(BASE_DIR));
            // 截图测试
            captureScreenshot(UUID.randomUUID().toString(), BufferTemp.builder().url("https://www.baidu.com").index(1).build(), null).whenComplete((pdf, throwable) -> {
                try(ByteArrayInputStream input = new ByteArrayInputStream(pdf.getBuffer());) {
                    IOUtils.copy(input, new FileOutputStream(new File(BASE_DIR, "test.png")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            }).join();

            // 生成pdf测试
            pageToPdf(UUID.randomUUID().toString(), BufferTemp.builder().url("https://www.baidu.com").index(1).build()).whenComplete((pdf, throwable) -> {
                try(ByteArrayInputStream input = new ByteArrayInputStream(pdf.getBuffer());) {
                    IOUtils.copy(input, new FileOutputStream(new File(BASE_DIR, "test.pdf")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            }).join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 定义一个浏览器内容截图方法
     * @param rendeId
     * @param urlTemps
     * @param selector
     * @return
     */
    protected List<BufferTemp> captureScreenshots(String rendeId, List<BufferTemp> urlTemps, String selector) {
        log.info("Capturing screenshots for urls: ", urlTemps.stream().map(BufferTemp::getUrl).collect(Collectors.toList()));
        // 1、使用CompletableFuture异步处理
        List<CompletableFuture<BufferTemp>> futureList = urlTemps.stream()
                .map(urlTemp -> captureScreenshot(rendeId, urlTemp, selector))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));
        CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(future -> future.join())
                        .collect(Collectors.toList()));
        return resultFuture.join();
    }

    /**
     * 定义一个浏览器页面截图方法
     * @param rendeId
     * @param urlTemp
     * @param selector
     * @return
     */
    protected CompletableFuture<BufferTemp> captureScreenshot(String rendeId, BufferTemp urlTemp, String selector){
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            log.info("Capturing screenshot : rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
            Page page = null;
            try {
                // 从池中获取一个浏览器页面
                page = browserContextPool.borrowObject().newPage();
                //page = browserPagePool.borrowObject();
                // 设置页面加载参数, 并跳转到url
                page.navigate(urlTemp.getUrl(), new Page.NavigateOptions()
                        .setTimeout(60 * 1000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                // 定义截图输出路径
                String fileName = String.format("%s.png", urlTemp.getIndex());
                log.info("screenshot start for {} : {}", rendeId, fileName);
                // 截图
                byte[] screenshotBuffer;
                if(StringUtils.isEmpty(selector)){
                    Page.ScreenshotOptions options = new Page.ScreenshotOptions()
                            .setFullPage(true)
                            .setOmitBackground(true)
                            .setTimeout(30 * 1000)
                            .setType(ScreenshotType.PNG);
                    screenshotBuffer = page.screenshot(options);
                } else {

                    // 定位到要截图的元素
                    ElementHandle element = page.querySelector(selector);

                    ElementHandle.ScreenshotOptions options = new ElementHandle.ScreenshotOptions()
                            .setOmitBackground(true)
                            .setTimeout(30 * 1000)
                            .setType(ScreenshotType.PNG);

                    // 截取指定元素的屏幕截图
                    screenshotBuffer = element.screenshot(options);
                }
                log.info("screenshot success for {} : {}", rendeId, fileName);
                urlTemp.setBuffer(screenshotBuffer);
                urlTemp.setName(fileName);
                browserContextPool.returnObject(page.context());
                //browserPagePool.returnObject(page);
                return urlTemp;
            } catch (Exception e) {
                throw new RuntimeException("Capture screenshot error: {}", e);
            } finally {
                try {
                    if (Objects.nonNull(page) && !page.isClosed()){
                        page.close();
                    }
                } catch (Exception e) {
                    // ignore error
                }
            }
        });

    }

    /**
     * 定义一个浏览器内容截图方法
     * @param rendeId
     * @param urlTemps
     * @return
     */
    protected List<BufferTemp> pageToPdfs(String rendeId, List<BufferTemp> urlTemps) {
        log.info("Capturing screenshots for urls: ", urlTemps.stream().map(BufferTemp::getUrl).collect(Collectors.toList()));
        // 1、使用CompletableFuture异步处理
        List<CompletableFuture<BufferTemp>> futureList = urlTemps.stream()
                .map(urlTemp -> pageToPdf(rendeId, urlTemp))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));
        CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(future -> future.join())
                        .collect(Collectors.toList()));
        return resultFuture.join();
    }

    protected CompletableFuture<BufferTemp> pageToPdf(String rendeId, BufferTemp urlTemp) {
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            log.info("Generate PDF for url: %s", urlTemp.getUrl());
            Page page = null;
            try {
                // 从池中获取一个浏览器页面
                page = browserContextPool.borrowObject().newPage();
                // 设置页面加载参数, 并跳转到url
                page.navigate(urlTemp.getUrl(), new Page.NavigateOptions()
                        .setTimeout(60 * 1000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                // 定义截图输出路径
                String fileName = String.format("%s.pdf", urlTemp.getIndex());
                log.info("Generate pdf buffer start for : {}", fileName);
                page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.SCREEN));
                // 生成PDF
                Page.PdfOptions pdfOptions = new Page.PdfOptions()
                        .setScale(1.0)
                        .setPageRanges("1-1")
                        .setFormat("A3")
                        .setPrintBackground(true);
                byte[] pdfBuffer = page.pdf(pdfOptions);
                log.info("Generate pdf buffer success for : {}", fileName);
                urlTemp.setBuffer(pdfBuffer);
                urlTemp.setName(fileName);
                // 释放页面对象
                browserContextPool.returnObject(page.context());
                return urlTemp;
            } catch (Exception e) {
                throw new RuntimeException("Generate PDF error: {}", e);
            } finally {
                try {
                    if (Objects.nonNull(page) && !page.isClosed()){
                        page.close();
                    }
                } catch (Exception e) {
                    // ignore error
                }
            }
        });
    }
}
