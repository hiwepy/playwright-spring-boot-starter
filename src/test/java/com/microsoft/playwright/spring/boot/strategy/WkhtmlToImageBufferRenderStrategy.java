package com.microsoft.playwright.spring.boot.strategy;


import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtils;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 使用 Playwright 渲染引擎将 HTML 渲染为各种图像格式
 */
@Slf4j
@Component
public class WkhtmlToImageBufferRenderStrategy extends AbstractPlaywrightRenderStrategy<WkhtmlRenderBO> {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_IMAGE_BUFFER;
    }

    @Override
    protected List<BufferTemp> doGenerate(WkhtmlRenderBO renderBO) throws IOException {
        // 2、生成截图
        return this.captureScreenshots(renderBO);
    }

    /**
     * 定义一个浏览器内容截图方法
     * @param renderBO
     * @return
     */
    protected List<BufferTemp> captureScreenshots(WkhtmlRenderBO renderBO) {
        log.info("Capturing screenshots for urls: {}", renderBO.getUrls().stream().map(BufferTemp::getUrl).collect(Collectors.toList()));
        if(renderBO.isAsync()){
            // 1、使用CompletableFuture异步处理
            List<CompletableFuture<BufferTemp>> futureList = renderBO.getUrls().stream()
                    .map(urlTemp -> captureScreenshotAsync(renderBO.getRanderId(), urlTemp, renderBO.getSelector()))
                    .collect(Collectors.toList());
            // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
            CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                    .thenApply(v -> futureList.stream().map(CompletableFuture::join).filter(urlTemp -> Objects.nonNull(urlTemp.getBuffer())).collect(Collectors.toList()));
            return resultFuture.join();
        } else {
            try(Playwright playwright = Playwright.create();
                Browser browser = PlaywrightUtils.getBrowser(playwright, playwrightProperties)) {
                List<BufferTemp> futureList = new ArrayList<>();
                for (BufferTemp urlTemp : renderBO.getUrls()) {
                    futureList.add(captureScreenshotSync(browser, renderBO.getRanderId(), urlTemp, renderBO.getSelector()));
                }
                return futureList.stream().filter(urlTemp -> StringUtils.isNotBlank(urlTemp.getPath())).collect(Collectors.toList());
            }
        }
    }

    @Override
    protected List<BufferTemp> doCompress(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) {
        // 1、获取压缩质量，如果压缩质量不在范围内，则不压缩
        Integer quality = renderBO.getQuality();
        if((quality > MAX_QUALITY || quality <= MIN_QUALITY)){
            log.info("Compressing screenshot ignore.");
            return screenshots;
        }
        // 2、异步压缩图片
        return compressScreenshots(screenshots, quality);
    }

    /**
     * 定义一个图片压缩方法
     * @param screenshots
     * @param quality
     * @return
     */
    protected List<BufferTemp> compressScreenshots(List<BufferTemp> screenshots, Integer quality) {
        if((quality > MAX_QUALITY || quality < MIN_QUALITY)){
            log.info("Compressing screenshot ignore.");
            return screenshots;
        }
        // 1、使用CompletableFuture异步处理
        List<CompletableFuture<BufferTemp>> futureList = screenshots.stream()
                .map(screenshot -> compressScreenshot(screenshot, quality))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));
        CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        return resultFuture.join();
    }

    /**
     * 定义一个图片压缩方法
     * @param screenshot
     * @param quality
     * @return
     */
    protected CompletableFuture<BufferTemp> compressScreenshot(BufferTemp screenshot, Integer quality) {
        // 判断压缩质量和压缩比例是否在范围内
        if(quality < MAX_QUALITY && quality > MIN_QUALITY){
            return CompletableFuture.supplyAsync(() -> {
                log.info("Compressing screenshot buffer: {}", screenshot.getName());
                // 使用Thumbnails进行图片压缩
                try(ByteArrayInputStream input = new ByteArrayInputStream(screenshot.getBuffer());
                    ByteArrayOutputStream output = new ByteArrayOutputStream() ){
                    // 从图片流中读取图片
                    Thumbnails.of(input)
                            .allowOverwrite(true)
                            .scale(1f)
                            .outputQuality(quality / 100f)
                            .toOutputStream(output);
                    screenshot.setBuffer(output.toByteArray());
                    log.info("Compressing screenshot buffer success : {}", screenshot.getName());
                } catch (Exception e) {
                    throw new TaskRuntimeException("Compressing screenshot buffer error: " , e);
                }
                return screenshot ;
            }, dtpToImageCompressExecutor);
        }
        log.info("Compressing screenshot ignore: {}", screenshot.getName());
        return CompletableFuture.completedFuture(screenshot);
    }

    @Override
    public WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        mergeScreenshotsToZip(renderBO.getRanderId(), screenshots).thenAccept(bufferTemp -> {
            resultBO.setFileBuffer(bufferTemp.getBuffer());
            resultBO.setFileName(bufferTemp.getName());
        }).join();
        return resultBO;
    }

    /**
     * 默认编码，使用平台相关编码
     */
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();
    /**
     * 定义一个图片合并为Zip方法
     * @param rendeId
     * @param screenshots
     * @return
     */
    protected CompletableFuture<BufferTemp> mergeScreenshotsToZip(String rendeId, List<BufferTemp> screenshots) {
        if(screenshots.size() == 1){
            BufferTemp screenshot = screenshots.get(0);
            String imageFileName = rendeId + "." + FilenameUtils.getExtension(screenshot.getName());
            screenshot.setName(imageFileName);
            return CompletableFuture.completedFuture(screenshot);
        }
        return CompletableFuture.supplyAsync(() -> {
            String zipFileName = rendeId + ".zip";
            log.info("Merging screenshots to ZIP: {}", zipFileName);
            // 请求数+1
            //metrics.playwright_zip_total_requset_count.inc(1);
            try {
                ByteArrayOutputStream zipOutput = new ByteArrayOutputStream();
                ZipOutputStream zipOutputStream = new ZipOutputStream(zipOutput, DEFAULT_CHARSET);
                // 将所有截图写入ZIP文件
                for (BufferTemp screenshot : screenshots) {
                    try (ByteArrayInputStream bufferInput = new ByteArrayInputStream(screenshot.getBuffer())){
                        String fileName = screenshot.getName();
                        log.info("Merging screenshot to ZIP: {}", fileName);
                        // 创建 ZipEntry 对象
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOutputStream.putNextEntry(zipEntry);
                        // 将截图缓存写入 ZipOutputStream
                        IOUtils.copy(bufferInput, zipOutputStream);
                        // 关闭当前 ZipEntry
                        zipOutputStream.closeEntry();
                    }
                    zipOutputStream.flush();
                }
                IOUtils.closeQuietly(zipOutputStream);
                IOUtils.closeQuietly(zipOutput);
                //metrics.playwright_zip_total_requset_success_count.inc(1);
                return BufferTemp.builder().index(0).name(zipFileName).buffer(zipOutput.toByteArray()).build();
            } catch (Exception e) {
                //metrics.playwright_zip_total_requset_error_count.inc(1);
                throw new TaskRuntimeException("Merging screenshots to ZIP error: {}", e);
            }
        }, dtpToImageZipExecutor);
    }

}
