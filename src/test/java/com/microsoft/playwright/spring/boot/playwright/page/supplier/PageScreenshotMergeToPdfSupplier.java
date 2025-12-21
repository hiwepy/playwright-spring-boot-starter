package com.microsoft.playwright.spring.boot.playwright.page.supplier;

import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.playwright.page.checker.PageScreenshotChecker;
import com.microsoft.playwright.spring.boot.playwright.util.PdfUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Slf4j
public class PageScreenshotMergeToPdfSupplier implements Supplier<PageScreenshotTemp> {

    @Getter
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Getter
    protected WkhtmlRenderBO renderBO;
    @Getter
    protected List<PageScreenshotTemp> screenshots;
    @Getter
    protected List<PageScreenshotChecker> pageScreenshotCheckers;
    @Getter
    protected BiFunction<PDDocument, List<PageScreenshotTemp>, PageScreenshotTemp> callback;

    public PageScreenshotMergeToPdfSupplier(PlaywrightRenderProperties playwrightRenderProperties,
                                            WkhtmlRenderBO renderBO,
                                            List<PageScreenshotTemp> screenshots,
                                            List<PageScreenshotChecker> pageScreenshotCheckers,
                                            BiFunction<PDDocument, List<PageScreenshotTemp>, PageScreenshotTemp> callback) {
        this.playwrightRenderProperties = playwrightRenderProperties;
        this.renderBO = renderBO;
        this.screenshots = screenshots;
        this.pageScreenshotCheckers = pageScreenshotCheckers;
        this.callback = callback;
    }

    @Override
    public PageScreenshotTemp get() {

        String pdfFileName = "document_" + renderBO.getTaskId() + ".pdf";
        log.info("Merging screenshots to PDF: {}", pdfFileName);

        // 声明在 try 块外部，以便在 finally 中关闭
        PDDocument sourceDocument = null;
        try {

            // 如果存在源文件URL，则尝试从远程加载源文件
            if (StringUtils.isNotBlank(renderBO.getFileUrl())) {
                PDDocument remotePdf = PdfUtil.loadRemotePdf(renderBO.getFileUrl(),
                        playwrightRenderProperties.getPdf().getConnectTimeout().toMillis(),
                        playwrightRenderProperties.getPdf().getReadTimeout().toMillis());
                if (Objects.nonNull(remotePdf)) {
                    sourceDocument = remotePdf;
                }
            }
            // 如果源文件不存在，则尝试从本地加载源文件（防止远程 PDF 加载失败）
            if (Objects.isNull(sourceDocument)) {
                // 创建目标 PDF 文档
                sourceDocument = new PDDocument();
            }

            // 添加新的页面
            PdfUtil.addPages(renderBO, screenshots, sourceDocument,
                    this.getPageScreenshotCheckers(),
                    renderBO.isSkipFail(),
                    renderBO.getMaxSingleColorPercent(),
                    renderBO.getMaxSimilarity());

            return callback.apply(sourceDocument, screenshots);

        } catch (Exception e) {
            throw new TaskRuntimeException("Failed to merge PDF File : " + pdfFileName, e);
        } finally {
            // 确保资源被正确关闭
            if (sourceDocument != null) {
                try {
                    sourceDocument.close();
                } catch (IOException e) {
                    log.error("Error closing target PDF document", e);
                }
            }
        }
    }
}