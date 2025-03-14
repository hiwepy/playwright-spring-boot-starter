package com.microsoft.playwright.spring.boot.strategy;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.util.PdfUtil;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式
 */
@Slf4j
@Component
public class WkhtmlToPdfMergerBufferRenderStrategy extends AbstractPlaywrightRenderStrategy<WkhtmlRenderBO> {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_PDF_MERGER_BUFFER;
    }

    @Override
    protected List<BufferTemp> doGenerate(WkhtmlRenderBO renderBO) throws IOException {
        // 2、生成截图
        return this.pageToPdfs(renderBO);
    }

    /**
     * 定义一个浏览器内容截图方法
     * @param renderBO 渲染参数
     * @return 截图缓存
     */
    protected List<BufferTemp> pageToPdfs(WkhtmlRenderBO renderBO) {
        log.info("Generate PDF for urls: {}", renderBO.getUrls().stream().map(BufferTemp::getUrl).collect(Collectors.toList()));
        if(renderBO.isAsync()) {
            // 1、使用CompletableFuture异步处理
            List<CompletableFuture<BufferTemp>> futureList = renderBO.getUrls().stream()
                    .map(urlTemp -> pageToPdfFutureAsync(renderBO.getRanderId(), urlTemp))
                    .collect(Collectors.toList());
            // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
            CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
            CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                    .thenApply(v -> futureList.stream().map(CompletableFuture::join).filter(urlTemp -> Objects.nonNull(urlTemp.getBuffer())).collect(Collectors.toList()));
            return resultFuture.join();
        } else {
            Playwright playwright = PlaywrightUtil.getInstance();
            try(Browser browser = PlaywrightUtil.getBrowser(playwright, playwrightProperties)) {
                List<BufferTemp> tempRtList = new ArrayList<>();
                for (BufferTemp urlTemp : renderBO.getUrls()) {
                    tempRtList.add(pageToPdfFutureSync(browser, renderBO.getRanderId(), urlTemp));
                }
                return tempRtList.stream().filter(urlTemp -> Objects.nonNull(urlTemp.getBuffer())).collect(Collectors.toList());
            }
        }
    }

    @Override
    protected List<BufferTemp> doCompress(WkhtmlRenderBO renderBO, List<BufferTemp> pdfs) {
        // 1、获取压缩质量，如果压缩质量不在范围内，则不压缩
        Integer quality = renderBO.getQuality();
        if((quality > MAX_QUALITY || quality < MIN_QUALITY)){
            log.info("Compressing pdf ignore.");
            return pdfs;
        }
        // 2、异步压缩pdf
        return pdfs;
    }

    @Override
    protected WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<BufferTemp> pdfs) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        mergePdfsToPDF(renderBO, pdfs).thenAccept(bufferTemp -> {
            resultBO.setFileBuffer(bufferTemp.getBuffer());
            resultBO.setFilePath(bufferTemp.getPath());
            resultBO.setFileName(bufferTemp.getName());
        }).join();
        return resultBO;
    }

    /**
     * 定义一个图片合并为PDF方法
     * @param renderBO 渲染参数
     * @param pdfs 截图缓存
     */
    protected CompletableFuture<BufferTemp> mergePdfsToPDF(WkhtmlRenderBO renderBO, List<BufferTemp> pdfs) {
        if (pdfs.size() == 1) {
            BufferTemp bufferTemp = pdfs.get(0);
            String pdfFileName = "document_" + renderBO.getRanderId() + ".pdf";
            bufferTemp.setName(pdfFileName);
            return CompletableFuture.completedFuture(bufferTemp);
        }
        return CompletableFuture.supplyAsync(() -> {
            String pdfFileName = "document_" + renderBO.getRanderId() + ".pdf";
            log.info("Merging pdf buffers to PDF: {}", pdfFileName);
            // 如果有多个文件，则合并pdf文件
            try{
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
                for (BufferTemp buffer : pdfs) {
                    /*try (ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.getBuffer())) {
                        mergePdf.addSource(inputStream);
                    }*/
                    mergePdf.addSource(new RandomAccessReadBuffer(buffer.getBuffer()));
                    log.info("Merging pdf buffer {} to PDF {} succeed.", buffer.getName(), pdfFileName);
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
/*
                String outPdfFilePath = baseDir + File.separator + pdfFileName;
                PdfUtil.scaleToA4(pdfFilePath, outPdfFilePath);*/
                // 返回合并后的pdf文件
                return BufferTemp.builder().index(0).name(pdfFileName).path(pdfFilePath).build();
            } catch (Exception e) {
                throw new TaskRuntimeException("Failed to merge pdf buffer : " + pdfFileName, e);
            }
        }, dtpToPdfMergeExecutor);
    }

}
