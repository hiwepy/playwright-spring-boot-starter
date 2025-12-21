package com.microsoft.playwright.spring.boot.playwright.page.supplier;

import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class PageScreenshotMergeToZipOutputStreamSupplier implements Supplier<PageScreenshotTemp> {

    /**
     * 默认编码，使用平台相关编码
     */
    private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

    @Getter
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Getter
    protected WkhtmlRenderBO renderBO;
    @Getter
    protected List<PageScreenshotTemp> screenshots;

    public PageScreenshotMergeToZipOutputStreamSupplier(PlaywrightRenderProperties playwrightRenderProperties,
                                                        WkhtmlRenderBO renderBO,
                                                        List<PageScreenshotTemp> screenshots) {
        this.playwrightRenderProperties = playwrightRenderProperties;
        this.renderBO = renderBO;
        this.screenshots = screenshots;
    }

    @Override
    public PageScreenshotTemp get() {
        if(screenshots.size() == 1){
            PageScreenshotTemp screenshot = screenshots.get(0);
            String imageFileName = renderBO.getTaskId() + "." + FilenameUtils.getExtension(screenshot.getName());
            screenshot.setName(imageFileName);
            return screenshot;
        }
        String zipFileName = renderBO.getTaskId() + ".zip";
        log.info("Merging screenshots to ZIP: {}", zipFileName);
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
    }
}