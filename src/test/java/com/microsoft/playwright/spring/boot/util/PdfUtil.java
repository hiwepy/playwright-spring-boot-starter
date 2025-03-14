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
import java.util.*;

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

    public static void setDocumentInformation(PDDocument pdDocument, WkhtmlRenderBO renderBO) {
        PDDocumentInformation information = information(renderBO);
        pdDocument.setDocumentInformation(information);
    }

    public static void addPages(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots, PDDocument pdDocument) throws IOException {
        // 设置PDF属性
        pdDocument.setDocumentInformation(PdfUtil.information(renderBO));
        PDPageSize pdPageSize = PDPageSize.getByName(renderBO.getPageSize());
        pdPageSize = Objects.isNull(pdPageSize) ? PDPageSize.A4 : pdPageSize;

        // 循环添加页面
        BufferedImage awtImage;
        PDImageXObject pdImageObject;
        PDPage pdPage;
        for (BufferTemp screenshot : screenshots) {
            // 获取图片对象
            awtImage = ImageUtil.getBufferedImage(screenshot);
            BufferedImage emptyImage = null;
            if(Objects.isNull(awtImage)){
                emptyImage =  ImageUtil.getWhiteImage(pdPageSize);
            }
            // 创建图片对象
            pdImageObject = LosslessFactory.createFromImage(pdDocument, Objects.nonNull(awtImage) ? awtImage : emptyImage);
            // 创建页面对象
            pdPage = new PDPage(pdPageSize.getRectangle());
            // 将页面添加到文档
            pdDocument.addPage(pdPage);
            // 创建内容流对象
            try (PDPageContentStream contentStream = new PDPageContentStream(pdDocument, pdPage, PDPageContentStream.AppendMode.APPEND, renderBO.getCompress())) {
                // 计算缩放后的图像尺寸
                BufferedImage usedImage = Objects.nonNull(awtImage) ? awtImage : emptyImage;
                float scaledWidth = usedImage.getWidth() * pdPageSize.getRectangle().getWidth() / usedImage.getWidth();
                float scaledHeight = usedImage.getHeight() * pdPageSize.getRectangle().getHeight() / usedImage.getHeight();
                log.info(" scaledWidth:{} , scaledHeight:{}  ", scaledWidth, scaledHeight);
                // 绘制图片
                contentStream.drawImage(pdImageObject, 0, 0, scaledWidth, scaledHeight);
            } finally {
                // 确保清理图片资源
                if (awtImage != null) {
                    awtImage.flush();
                }
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
