package com.microsoft.playwright.spring.boot.playwright.util;

import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.checker.PageScreenshotChecker;
import com.microsoft.playwright.spring.boot.playwright.enums.PDPageSize;
import com.microsoft.playwright.spring.boot.playwright.enums.RenderState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

@Slf4j
public class PdfUtil {

    public static PDDocumentInformation information(WkhtmlRenderBO renderBO){
        PDDocumentInformation information = new PDDocumentInformation();
        information.setAuthor(StringUtils.defaultString(renderBO.getAuthor()));
        information.setKeywords(StringUtils.defaultString(renderBO.getKeywords()));
        information.setSubject(StringUtils.defaultString(renderBO.getSubject())) ;
        information.setTitle(StringUtils.defaultString(renderBO.getAuthor()));
        information.setProducer("Playwright PDF Generater");
        information.setCreator(StringUtils.defaultString(renderBO.getCreator()));
        information.setCreationDate(Calendar.getInstance());
        information.setModificationDate(Calendar.getInstance());
        return information;
    }

    /**
     * 添加页面
     * @param renderBO 渲染参数
     * @param screenshots 截图列表
     * @param pdDocument PDF文档对象
     * @throws IOException IO异常
     */
    public static void addPages(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots, PDDocument pdDocument) throws IOException {
        addPages(renderBO, screenshots, pdDocument, false);
    }

    /**
     * 添加页面
     * @param renderBO 渲染参数
     * @param screenshots 截图列表
     * @param pdDocument PDF文档对象
     * @param skipFail 是否跳过失败
     * @throws IOException IO异常
     */
    public static void addPages(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots, PDDocument pdDocument, boolean skipFail) throws IOException {
        addPages(renderBO, screenshots, pdDocument, null, skipFail, 75f, 75f);
    }

    /**
     * 添加页面
     * @param renderBO 渲染参数
     * @param screenshots 截图列表
     * @param pdDocument PDF文档对象
     * @param skipFail 是否跳过失败
     * @param maxSingleColorPercent 最大单色占比
     * @throws IOException IO异常
     */
    public static void addPages(WkhtmlRenderBO renderBO, List<PageScreenshotTemp> screenshots, PDDocument pdDocument, boolean skipFail, float maxSingleColorPercent) throws IOException {
        addPages(renderBO, screenshots, pdDocument, null, skipFail, maxSingleColorPercent, 75f);
    }

    /**
     * 添加页面
     * @param renderBO 渲染参数
     * @param screenshots 截图列表
     * @param pdDocument PDF文档对象
     * @param skipFail 是否跳过失败
     * @param maxSingleColorPercent 最大单色占比
     * @param maxSimilarity 最大相似度
     * @throws IOException IO异常
     */
    public static void addPages(WkhtmlRenderBO renderBO,
                                List<PageScreenshotTemp> screenshots,
                                PDDocument pdDocument,
                                boolean skipFail,
                                float maxSingleColorPercent,
                                float maxSimilarity) throws IOException {
        addPages(renderBO, screenshots, pdDocument, null, skipFail, maxSingleColorPercent, maxSimilarity);
    }

    /**
     * 添加页面
     * @param renderBO 渲染参数
     * @param screenshots 截图列表
     * @param pdDocument PDF文档对象
     * @param pageScreenshotCheckers 页面截图检查器列表
     * @param skipFail 是否跳过失败
     * @param maxSingleColorPercent 最大单色占比
     * @param maxSimilarity 最大相似度
     * @throws IOException IO异常
     */
    public static void addPages(WkhtmlRenderBO renderBO,
                                List<PageScreenshotTemp> screenshots,
                                PDDocument pdDocument,
                                List<PageScreenshotChecker> pageScreenshotCheckers,
                                boolean skipFail,
                                float maxSingleColorPercent,
                                float maxSimilarity) throws IOException {
        // 设置PDF属性
        pdDocument.setDocumentInformation(PdfUtil.information(renderBO));
        PDPageSize pdPageSize = PDPageSize.getByName(renderBO.getPageSize());
        pdPageSize = Objects.isNull(pdPageSize) ? PDPageSize.A4 : pdPageSize;

        // 循环添加页面
        BufferedImage pdfImage = null;
        BufferedImage emptyImage = null;
        PDImageXObject pdImageObject = null;
        PDPage pdPage;
        try {
            for (PageScreenshotTemp screenshot : screenshots) {
                // 从缓存加载图片
                if(Objects.nonNull(screenshot.getBuffer())){
                    log.info("Load Input Stream From Screenshot Buffer, size: {}", screenshot.getBuffer().length);
                    try(ByteArrayInputStream bis = new ByteArrayInputStream(screenshot.getBuffer())) {
                        // 从图片流中读取图片
                        pdfImage = ImageIO.read(bis);
                    } catch (Exception ex){
                        screenshot.setRenderState(RenderState.FAIL);
                        screenshot.setFailedReason(String.format("从缓存加载页面截图失败，失败原因：%s", ex.getMessage()));
                        log.error("Read Image From Buffer Error: {}", ex.getMessage());
                        if (skipFail){
                            continue;
                        }
                    }
                }
                // 从路径加载图片
                else if(StringUtils.isNotBlank(screenshot.getPath())){
                    log.info("Load Image From Screenshot Path: {}", screenshot.getPath());
                    File imgFile = new File(screenshot.getPath());
                    if(!imgFile.exists()){
                        log.warn("Screenshot Image File {} Not Exists, skip it.", screenshot.getPath());
                        screenshot.setRenderState(RenderState.CHECK_FAIL);
                        screenshot.setFailedReason(String.format("页面截图图片文件 %s 不存在，无法添加到PDF中。", screenshot.getPath()));
                        if (skipFail){
                            continue;
                        }
                    }
                    try {
                        // 从图片流中读取图片
                        pdfImage = ImageIO.read(imgFile);
                    } catch (Exception ex){
                        screenshot.setRenderState(RenderState.FAIL);
                        screenshot.setFailedReason(String.format("从路径 %s, 加载页面截图失败，失败原因：%s", screenshot.getPath(), ex.getMessage()));
                        log.error("Read Image From File Error: {}", ex.getMessage());
                        if (skipFail){
                            continue;
                        }
                    }
                }
                // 如果图片为空，使用空白图片
                else {
                    screenshot.setRenderState(RenderState.CHECK_FAIL);
                    screenshot.setFailedReason("截图图片为空，使用空白图片。");
                    if (skipFail){
                        continue;
                    }
                }

                // 如果图片不为空，且存在检查器，则进行图片添加到PDF对象前的检查
                if(!CollectionUtils.isEmpty(pageScreenshotCheckers)){
                    boolean isFit = true;
                    for(PageScreenshotChecker checker : pageScreenshotCheckers){
                        if(Objects.nonNull(checker) && !checker.beforePdfPageAdd(screenshot, pdfImage, pdPageSize, maxSingleColorPercent, maxSimilarity)){
                            log.warn("Screenshot Image is not fit for PDF, .");
                            if(Objects.isNull(screenshot.getRenderState())){
                                screenshot.setRenderState(RenderState.CHECK_FAIL);
                                screenshot.setFailedReason("截图图片检查未通过，无法添加到PDF中。");
                                isFit = false;
                                break;
                            }
                        }
                    }
                }

                // 如果图片为空，并且不跳过异常的图片，则使用空白图片
                if(Objects.isNull(pdfImage) && !skipFail) {
                    log.warn("Screenshot Image is null, use empty image.");
                    if (Objects.isNull(emptyImage)) {
                        switch (pdPageSize) {
                            case A4:
                                emptyImage = ImageUtil.WHITE_A4;
                                break;
                            case A3:
                                emptyImage = ImageUtil.WHITE_A3;
                                break;
                            default:
                                emptyImage = ImageUtil.getWhiteImage(Float.valueOf(pdPageSize.getRectangle().getWidth()).intValue(), Float.valueOf(pdPageSize.getRectangle().getHeight()).intValue());
                        }
                    }
                    pdfImage = emptyImage;
                }
                // 创建图片对象
                pdImageObject = LosslessFactory.createFromImage(pdDocument, pdfImage);
                // 创建页面对象
                pdPage = new PDPage(pdPageSize.getRectangle());
                // 将页面添加到文档
                pdDocument.addPage(pdPage);
                // 创建内容流对象
                try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, renderBO.getCompress())) {
                    // 计算缩放比例，使图像适应页面的大小
                    float scaleFactor = Math.min(pdPageSize.getRectangle().getWidth() / pdfImage.getWidth(), pdPageSize.getRectangle().getWidth() / pdfImage.getHeight());
                    // 计算缩放后的图像尺寸
                    float scaledWidth = pdfImage.getWidth() * pdPageSize.getRectangle().getWidth() / pdfImage.getWidth();
                    float scaledHeight = pdfImage.getHeight() * pdPageSize.getRectangle().getHeight() / pdfImage.getHeight();
                    log.info("scaleFactor:{}, scaledWidth:{} , scaledHeight:{}  ", scaleFactor, scaledWidth, scaledHeight);
                    // 绘制图片
                    contentStream.drawImage(pdImageObject, 0, 0, scaledWidth, scaledHeight);
                }

                // 释放当前页面的图片资源
                if (pdImageObject != null) {
                    pdImageObject = null;
                }
                if (pdfImage != null && pdfImage != emptyImage) {
                    pdfImage.flush();
                    pdfImage = null;
                }
            }
        } finally {
            // 释放所有资源
            if (pdImageObject != null) {
                pdImageObject = null;
            }
            if (pdfImage != null && pdfImage != emptyImage) {
                pdfImage.flush();
            }
        }
    }
    public static void main(String[] args) {
        String userDir = "/home/admin";
        StringJoiner joiner = new StringJoiner(File.separator);
        joiner.add(File.separator).add(userDir).add("static").add("v3");
        log.info("staticFile:{}", joiner);
        System.out.println("staticFile: " + joiner);

    }
}
