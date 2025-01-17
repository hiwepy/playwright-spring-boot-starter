package com.microsoft.playwright.spring.boot.strategy;

import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.util.PdfUtil;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式
 */
@Slf4j
@Component
public class WkhtmlToPdfBufferRenderStrategy extends WkhtmlToImageBufferRenderStrategy {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_PDF_BUFFER;
    }

    @Override
    public WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        mergeScreenshotsToPDF(renderBO, screenshots)
                .thenAccept(bufferTemp -> {
                    resultBO.setFileBuffer(bufferTemp.getBuffer());
                    resultBO.setFileName(bufferTemp.getName());
                }).join();
        return resultBO;
    }

    /**
     * 定义一个图片合并为PDF方法
     *
     * @param renderBO
     * @param screenshots
     * @return
     * @throws IOException
     */
    protected CompletableFuture<BufferTemp> mergeScreenshotsToPDF(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) {
        return CompletableFuture.supplyAsync(() -> {
            String pdfFileName = "document_" + renderBO.getRanderId() + ".pdf";
            log.info("Merging screenshots to PDF: {}", pdfFileName);
            // 请求数+1
            // 创建空白文档
            try (PDDocument pdDocument = new PDDocument()) {
                PdfUtil.addPages(renderBO, screenshots, pdDocument);
                try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                    pdDocument.save(outputStream);
                    return BufferTemp.builder().index(0).name(pdfFileName).buffer(outputStream.toByteArray()).build();
                }
            } catch (Exception e) {
                throw new TaskRuntimeException("Merging screenshots to PDF failed", e);
            }
        }, dtpToPdfMergeExecutor);
    }


}
