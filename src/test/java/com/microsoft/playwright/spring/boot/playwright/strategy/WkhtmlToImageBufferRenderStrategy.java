package com.microsoft.playwright.spring.boot.playwright.strategy;


import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.enums.RenderType;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.playwright.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 使用 Playwright 渲染引擎将 HTML 渲染为各种图像格式
 * @author wandl
 */
@Slf4j
public class WkhtmlToImageBufferRenderStrategy extends AbstractPlaywrightRenderStrategy<WkhtmlRenderBO> {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_IMAGE_BUFFER;
    }

    @Override
    protected List<PageScreenshotTemp> doGenerate(WkhtmlRenderBO renderBO) throws IOException {
        log.info("Capturing screenshots for urls: {}", renderBO.getUrls().stream().map(PageScreenshotTemp::getUrl).collect(Collectors.toList()));
        if(renderBO.isAsync()){
            return this.captureScreenshotAsync(renderBO);
        } else {
            return this.captureScreenshotSync(renderBO);
        }
    }

    @Override
    protected List<PageScreenshotTemp> doCompress(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots) {
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
    protected List<PageScreenshotTemp> compressScreenshots(List<PageScreenshotTemp> screenshots, Integer quality) {
        if((quality > MAX_QUALITY || quality < MIN_QUALITY)){
            log.info("Compressing screenshot ignore.");
            return screenshots;
        }
        // 1、使用CompletableFuture异步处理
        List<CompletableFuture<PageScreenshotTemp>> futureList = screenshots.stream()
                .map(screenshot -> compressScreenshot(screenshot, quality))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        CompletableFuture<List<PageScreenshotTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(CompletableFuture::join).collect(Collectors.toList()));
        return resultFuture.join();
    }

    /**
     * 定义一个图片压缩方法
     * @param screenshot
     * @param quality
     * @return
     */
    protected CompletableFuture<PageScreenshotTemp> compressScreenshot(PageScreenshotTemp screenshot, Integer quality) {
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
    public WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        mergeScreenshotsToZip(renderBO.getTaskId(), screenshots).thenAccept(bufferTemp -> {
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
    protected CompletableFuture<PageScreenshotTemp> mergeScreenshotsToZip(String rendeId, List<PageScreenshotTemp> screenshots) {
        if(screenshots.size() == 1){
            PageScreenshotTemp screenshot = screenshots.get(0);
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
                for (PageScreenshotTemp screenshot : screenshots) {
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
                return new PageScreenshotTemp().setIndex(0).setName(zipFileName).setBuffer(zipOutput.toByteArray());
            } catch (Exception e) {
                //metrics.playwright_zip_total_requset_error_count.inc(1);
                throw new TaskRuntimeException("Merging screenshots to ZIP error: {}", e);
            }
        }, dtpToImageZipExecutor);
    }

}
