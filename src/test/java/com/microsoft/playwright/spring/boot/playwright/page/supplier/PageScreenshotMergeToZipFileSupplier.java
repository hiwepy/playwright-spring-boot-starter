package com.microsoft.playwright.spring.boot.playwright.page.supplier;

import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class PageScreenshotMergeToZipFileSupplier implements Supplier<PageScreenshotTemp> {

    @Getter
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Getter
    protected WkhtmlRenderBO renderBO;
    @Getter
    protected List<PageScreenshotTemp> screenshots;

    public PageScreenshotMergeToZipFileSupplier(PlaywrightRenderProperties playwrightRenderProperties,
                                                WkhtmlRenderBO renderBO,
                                                List<PageScreenshotTemp> screenshots) {
        this.playwrightRenderProperties = playwrightRenderProperties;
        this.renderBO = renderBO;
        this.screenshots = screenshots;
    }

    @Override
    public PageScreenshotTemp get() {
        String zipFileName = renderBO.getTaskId() + ".zip";
        log.info("Merging screenshots to ZIP: {}", zipFileName);
        File zipFile = new File(playwrightRenderProperties.getTmpDir(), zipFileName);
        try {
            ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()));
            // 将所有截图写入ZIP文件
            for (PageScreenshotTemp screenshot : screenshots) {
                // 读取图片文件并写入 ZipOutputStream
                try (FileInputStream fileInput = new FileInputStream(screenshot.getPath())) {
                    String fileName = screenshot.getName();
                    log.info("Merging screenshot to ZIP: {}", fileName);
                    // 创建 ZipEntry 对象
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);
                    // 将截图文件写入 ZipOutputStream
                    IOUtils.copy(fileInput, zipOutputStream);
                    // 关闭当前 ZipEntry
                    zipOutputStream.closeEntry();
                }
                zipOutputStream.flush();
            }
            IOUtils.closeQuietly(zipOutputStream);
            //metrics.playwright_zip_total_requset_success_count.inc(1);
            return new PageScreenshotTemp().setIndex(0).setName(zipFileName).setPath(zipFile.getAbsolutePath());
        } catch (Exception e) {
            throw new TaskRuntimeException("Failed to pack ZIP File : " + zipFileName, e);
        }
    }
}