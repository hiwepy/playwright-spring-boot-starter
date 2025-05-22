package com.microsoft.playwright.spring.boot.playwright.strategy;

import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.enums.RenderType;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.playwright.util.PdfUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式
 * @author wandl
 */
@Slf4j
public class WkhtmlToPdfMergerFileRenderStrategy extends WkhtmlToPdfMergerBufferRenderStrategy {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_PDF_MERGER_FILE;
    }

    @Override
    protected List<PageScreenshotTemp> doGenerate(WkhtmlRenderBO renderBO) {
        log.info("Generate PDF for urls: {}", renderBO.getUrls().stream().map(PageScreenshotTemp::getUrl).collect(Collectors.toList()));
        if(renderBO.isAsync()){
            return this.pageToPdfFutureAsync(renderBO);
        } else {
            return this.pageToPdfFutureSync(renderBO);
        }
    }

    /**
     * 定义一个图片合并为PDF方法
     * @param renderBO
     * @param pdfs
     * @throws IOException
     */
    @Override
    protected CompletableFuture<PageScreenshotTemp> mergePdfsToPDF(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> pdfs) {
        if(pdfs.size() == 1){
            PageScreenshotTemp pageScreenshotTemp = pdfs.get(0);
            String pdfFileName = "document_" + renderBO.getTaskId() + ".pdf";
            pageScreenshotTemp.setName(pdfFileName);
            return CompletableFuture.completedFuture(pageScreenshotTemp);
        }
        return CompletableFuture.supplyAsync(() -> {
            String pdfFileName = "document_" + renderBO.getTaskId() + ".pdf";
            log.info("Merging pdf files to PDF: {}", pdfFileName);
            // 如果有多个文件，则合并pdf文件
            try {
                /**
                 * org.apache.pdfbox.util.PDFMergerUtility：pdf合并工具类
                 * https://blog.csdn.net/qq_38998209/article/details/127983909
                 */
                PDFMergerUtility mergePdf = new PDFMergerUtility();
                // 设置PDF属性
                mergePdf.setDestinationDocumentInformation(PdfUtil.information(renderBO));
                // 设置合并模式为压缩资源模式
                mergePdf.setDocumentMergeMode(PDFMergerUtility.DocumentMergeMode.OPTIMIZE_RESOURCES_MODE);
                // 合并pdf文件路径
                for (PageScreenshotTemp buffer : pdfs) {
                    mergePdf.addSource(new File(buffer.getPath()));
                    log.info("Merging pdf file {} to PDF {} succeed.", buffer.getPath(), pdfFileName);
                }
                // 设置合并生成pdf文件名称
                String pdfFilePath = playwrightRenderProperties.getTmpDir() + File.separator + pdfFileName;
                mergePdf.setDestinationFileName(pdfFilePath);
                // 使用主内存进行PDF合并处理
                //mergePdf.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
                // 或者使用磁盘临时文件进行处理
                //mergePdf.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
                // 或者结合使用主内存和磁盘临时文件进行处理（这里设置8MB）
                //mergePdf.mergeDocuments(MemoryUsageSetting.setupMixed(8 * 1024 * 1024));
                // Since v3.0.2
                mergePdf.mergeDocuments(null);
                // 返回合并后的pdf文件
                return new PageScreenshotTemp().setIndex(0).setName(pdfFileName).setPath(pdfFilePath);
            } catch (Exception e) {
                throw new TaskRuntimeException("Merge pdf files to PDF error: ", e);
            }
        }, dtpToPdfMergeExecutor);
    }

}
