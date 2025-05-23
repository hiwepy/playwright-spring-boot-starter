package com.microsoft.playwright.spring.boot.playwright.page.checker;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;

@Slf4j
public class PixelComparator {

    public static double compareImages(BufferedImage img1, BufferedImage img2) {
        // 确保图片尺寸相同
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException("图片尺寸必须相同");
        }

        int width = img1.getWidth();
        int height = img1.getHeight();
        long diff = 0;

        // 遍历每个像素
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                // 分解RGB分量
                int r1 = (rgb1 >> 16) & 0xff;
                int g1 = (rgb1 >> 8) & 0xff;
                int b1 = rgb1 & 0xff;

                int r2 = (rgb2 >> 16) & 0xff;
                int g2 = (rgb2 >> 8) & 0xff;
                int b2 = rgb2 & 0xff;

                // 计算差异
                diff += Math.abs(r1 - r2);
                diff += Math.abs(g1 - g2);
                diff += Math.abs(b1 - b2);
            }
        }

        // 计算平均差异
        double totalPixels = width * height * 3;
        double avgDiff = diff / totalPixels;

        // 转换为相似度百分比 (0-100)
        return 100 - (avgDiff / 255) * 100;
    }

}
