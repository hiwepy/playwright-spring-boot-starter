package com.microsoft.playwright.spring.boot.playwright.strategy;

import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.enums.RenderType;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.playwright.util.PdfUtil;
import com.microsoft.playwright.spring.boot.playwright.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式
 * @author wandl
 */
@Slf4j
public class WkhtmlToPdfFileRenderStrategy extends WkhtmlToImageFileRenderStrategy {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_PDF_FILE;
    }

    @Override
    public WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        mergeScreenshotsToPDF(renderBO, screenshots).thenAccept(bufferTemp -> {
            resultBO.setFileName(bufferTemp.getName());
            resultBO.setFilePath(bufferTemp.getPath());
        }).join();
        return resultBO;
    }

    /**
     * 定义一个图片合并为PDF方法
     * @param renderBO
     * @param screenshots
     * @return
     * @throws IOException
     */
    protected CompletableFuture<PageScreenshotTemp> mergeScreenshotsToPDF(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots) {
        return CompletableFuture.supplyAsync(() -> {
            String pdfFileName = "document_" + renderBO.getTaskId() + ".pdf";
            log.info("Merging screenshots to PDF: {}", pdfFileName);
            File pdfFile = new File(playwrightRenderProperties.getTmpDir(), pdfFileName);
            // 请求数+1
            // 创建空白文档
            try (PDDocument pdDocument = new PDDocument()) {
                PdfUtil.addPages(renderBO, screenshots, pdDocument,
                        this.getPageScreenshotCheckers(),
                        renderBO.isSkipFail(),
                        renderBO.getMaxSingleColorPercent(),
                        renderBO.getMaxSimilarity());
                // 保存文档到文件
                pdDocument.save(pdfFile);
                return new PageScreenshotTemp().setIndex(0).setName(pdfFileName).setPath(pdfFile.getAbsolutePath());
            } catch (Exception e) {
                throw new TaskRuntimeException("Failed to marge PDF File : " + pdfFileName, e);
            }
        }, dtpToPdfMergeExecutor);
    }


}
