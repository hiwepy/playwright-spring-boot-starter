package com.microsoft.playwright.spring.boot.playwright.page.checker;

import java.util.stream.IntStream;

public class FastImageComparator {

    // 使用并行流加速处理
    private static final boolean USE_PARALLEL = true;

    public static double compare(ImagePixelCache cache1, ImagePixelCache cache2) {
        // 尺寸校验
        if (cache1.getWidth() != cache2.getWidth() || cache1.getHeight() != cache2.getHeight()) {
            throw new IllegalArgumentException("图片尺寸不一致");
        }

        int totalPixels = cache1.getWidth() * cache1.getHeight();
        if (totalPixels == 0) {
            return 100.0; // 空图片视为完全相似
        }

        // 计算各通道差异
        double rDiff = channelDiff(cache1.getRedChannel(), cache2.getRedChannel(), totalPixels);
        double gDiff = channelDiff(cache1.getGreenChannel(), cache2.getGreenChannel(), totalPixels);
        double bDiff = channelDiff(cache1.getBlueChannel(), cache2.getBlueChannel(), totalPixels);

        // 使用加权平均计算差异
        double avgDiff = (rDiff * 0.299 + gDiff * 0.587 + bDiff * 0.114);

        // 转换为相似度百分比 (0-100)
        double similarity =  (avgDiff / 255.0) * 100.0;
        
        // 确保结果在有效范围内
        return Math.max(0.0, Math.min(100.0, similarity));
    }

    private static double channelDiff(byte[] channel1, byte[] channel2, int length) {
        if (USE_PARALLEL && length > 10000) {
            // 大图片使用并行计算
            return IntStream.range(0, length).parallel()
                    .mapToDouble(i -> Math.abs((channel1[i] & 0xFF) - (channel2[i] & 0xFF)))
                    .average().orElse(0);
        } else {
            // 小图片顺序计算
            double sum = 0;
            for (int i = 0; i < length; i++) {
                sum += Math.abs((channel1[i] & 0xFF) - (channel2[i] & 0xFF));
            }
            return sum / length;
        }
    }
}
