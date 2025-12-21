package com.microsoft.playwright.spring.boot.playwright.page.supplier;

import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.playwright.page.checker.PageScreenshotChecker;
import com.microsoft.playwright.spring.boot.playwright.util.PdfUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Slf4j
public class PagePdfMergeToPdfSupplier implements Supplier<PageScreenshotTemp> {

    @Getter
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Getter
    protected WkhtmlRenderBO renderBO;
    @Getter
    protected List<PageScreenshotTemp> pdfs;
    @Getter
    protected List<PageScreenshotChecker> pageScreenshotCheckers;
    @Getter
    protected BiFunction<PDFMergerUtility, List<PageScreenshotTemp>, PageScreenshotTemp> biFunction;

    public PagePdfMergeToPdfSupplier(PlaywrightRenderProperties playwrightRenderProperties,
                                     WkhtmlRenderBO renderBO,
                                     List<PageScreenshotTemp> pdfs,
                                     List<PageScreenshotChecker> pageScreenshotCheckers,
                                     BiFunction<PDFMergerUtility, List<PageScreenshotTemp>, PageScreenshotTemp> biFunction) {
        this.playwrightRenderProperties = playwrightRenderProperties;
        this.renderBO = renderBO;
        this.pdfs = pdfs;
        this.pageScreenshotCheckers = pageScreenshotCheckers;
        this.biFunction = biFunction;
    }

    @Override
    public PageScreenshotTemp get() {

        String pdfFileName = "document_" + renderBO.getTaskId() + ".pdf";
        log.info("Merging pdf buffers/files to PDF: {}", pdfFileName);
        // 单个文件直接返回
        if (pdfs.size() == 1) {
            PageScreenshotTemp pageScreenshotTemp = pdfs.get(0);
            pageScreenshotTemp.setName(pdfFileName);
            return pageScreenshotTemp;
        }
        // 如果有多个文件，则合并pdf文件
        try{
            /*
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
                if (Objects.nonNull(buffer.getBuffer())) {
                    mergePdf.addSource(new RandomAccessReadBuffer(buffer.getBuffer()));
                    log.info("Merging pdf buffer {} to PDF {} succeed.", buffer.getName(), pdfFileName);
                } else if (Objects.nonNull(buffer.getPath())) {
                    // 如果是文件路径，则直接添加文件
                    mergePdf.addSource(new File(buffer.getPath()));
                    log.info("Merging pdf file {} to PDF {} succeed.", buffer.getPath(), pdfFileName);
                }
            }
            return biFunction.apply(mergePdf, pdfs);
        } catch (Exception e) {
            throw new TaskRuntimeException("Failed to merge PDF File : " + pdfFileName, e);
        }
    }

}