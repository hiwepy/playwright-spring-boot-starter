package com.microsoft.playwright.spring.boot.util;

import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.PDPageSize;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

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

    public static void addPages(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots, PDDocument pdDocument) throws IOException {
        // 设置PDF属性
        pdDocument.setDocumentInformation(PdfUtil.information(renderBO));
        PDPageSize pdPageSize = PDPageSize.getByName(renderBO.getPageSize());
        pdPageSize = Objects.isNull(pdPageSize) ? PDPageSize.A4 : pdPageSize;

        // 循环添加页面
        BufferedImage awtImage = null;
        BufferedImage emptyImage = null;
        PDImageXObject pdImageObject;
        PDPage pdPage;
        for (BufferTemp screenshot : screenshots) {
            // 从图片流中读取图片
            if(Objects.nonNull(screenshot.getBuffer())){
                log.info("Load Input Stream From Screenshot Buffer, size: {}", screenshot.getBuffer().length);
                try(ByteArrayInputStream bis = new ByteArrayInputStream(screenshot.getBuffer())) {
                    // 从图片流中读取图片
                    awtImage = ImageIO.read(bis);
                } catch (Exception ex){
                    log.error("Read Image From Buffer Error: {}", ex.getMessage());
                }
            } else if(StringUtils.isNotBlank(screenshot.getPath())){
                log.info("Load Image From Screenshot Path: {}", screenshot.getPath());
                File imgFile = new File(screenshot.getPath());
                if(!imgFile.exists()){
                    log.warn("Screenshot Image File {} Not Exists, skip it.", screenshot.getPath());
                    continue;
                }
                try {
                    // 从图片流中读取图片
                    awtImage = ImageIO.read(imgFile);
                } catch (Exception ex){
                    log.error("Read Image From File Error: {}", ex.getMessage());
                }
            }
            if(Objects.isNull(awtImage)){
                log.warn("Screenshot Image is null, use empty image.");
                if(Objects.isNull(emptyImage)){
                    emptyImage = PDPageSize.A4.compareTo(pdPageSize) == 0 ? ImageUtil.WHITE_A4 : ImageUtil.getWhiteImage(Float.valueOf(pdPageSize.getRectangle().getWidth()).intValue(), Float.valueOf(pdPageSize.getRectangle().getHeight()).intValue());
                }
                awtImage = emptyImage;
            }
            // 创建图片对象
            pdImageObject = LosslessFactory.createFromImage(pdDocument, awtImage);
            // 创建页面对象
            pdPage = new PDPage(pdPageSize.getRectangle());
            // 将页面添加到文档
            pdDocument.addPage(pdPage);
            // 创建内容流对象
            try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, renderBO.getCompress())) {
                // 计算缩放比例，使图像适应页面的大小
                float scaleFactor = Math.min(pdPageSize.getRectangle().getWidth() / awtImage.getWidth(), pdPageSize.getRectangle().getWidth() / awtImage.getHeight());
                // 计算缩放后的图像尺寸
                float scaledWidth = awtImage.getWidth() * pdPageSize.getRectangle().getWidth() / awtImage.getWidth();
                float scaledHeight = awtImage.getHeight() * pdPageSize.getRectangle().getHeight() / awtImage.getHeight();
                log.info("scaleFactor:{}, scaledWidth:{} , scaledHeight:{}  ", scaleFactor, scaledWidth, scaledHeight);
                // 绘制图片
                contentStream.drawImage(pdImageObject, 0, 0, scaledWidth, scaledHeight);
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
