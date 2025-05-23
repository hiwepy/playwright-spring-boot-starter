package com.microsoft.playwright.spring.boot.playwright.page.checker;

import com.github.benmanes.caffeine.cache.*;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.enums.PDPageSize;
import com.microsoft.playwright.spring.boot.playwright.enums.RenderState;
import com.microsoft.playwright.spring.boot.playwright.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class DefaultPageScreenshotChecker implements PageScreenshotChecker {

    private final LoadingCache<String, Optional<BufferedImage>> IMAGE_CACHES;

    public DefaultPageScreenshotChecker(Duration expireAfterWrite, int initialCapacity, int maximumSize){
        IMAGE_CACHES = Caffeine.newBuilder()
                // 设置写缓存后1个小时过期
                .expireAfterWrite(expireAfterWrite)
                // 设置缓存容器的初始容量为10
                .initialCapacity(initialCapacity)
                // 设置缓存最大容量为100，超过100之后就会按照LRU最近虽少使用算法来移除缓存项
                .maximumSize(maximumSize)
                // 设置要统计缓存的命中率
                .recordStats()
                // 设置缓存的移除通知
                .removalListener(new RemovalListener<String, Optional<BufferedImage>>() {

                    @Override
                    public void onRemoval(String imageUrl,Optional<BufferedImage> value, @NonNull RemovalCause cause) {
                        log.info("The BufferedImage cache of {} was removed, cause is {}", imageUrl, cause);
                    }

                })
                // build方法中可以指定CacheLoader，在缓存不存在时通过CacheLoader的实现自动加载缓存
                .build(new CacheLoader<String, Optional<BufferedImage>>() {

                    @Override
                    public Optional<BufferedImage> load(String imageUrl) throws Exception {
                        if (StringUtils.isBlank(imageUrl)) {
                            return Optional.empty();
                        }
                        try{
                            URL url = new URL(imageUrl);
                            try (InputStream in = url.openStream()) {
                                BufferedImage background = ImageIO.read(in);
                                return Optional.of(background);
                            }
                        } catch (Exception e) {
                            log.error("Image URL {} load error: {}", imageUrl, e.getMessage());
                        }
                        return Optional.empty();
                    }
                });
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public boolean beforePdfPageAdd(PageScreenshotTemp screenshot, BufferedImage pdfImage, PDPageSize pdfPageSize, float maxSingleColorPercent, float maxSimilarity) {
        // 1、检查PDF图片是否为空
        if(Objects.isNull(pdfImage)){
            log.warn("PDF Image is null");
            screenshot.setRenderState(RenderState.CHECK_FAIL);
            screenshot.setFailedReason("截图图片为空，无法添加到PDF中。");
            return Boolean.FALSE;
        }
        // 2、检查PDF图片是否是白色图片
        if (ImageUtil.isWhiteImageOutPercent(pdfImage, maxSingleColorPercent)) {
            screenshot.setRenderState(RenderState.CHECK_FAIL);
            screenshot.setFailedReason("截图图片检查未通过，白色内容占比 >= 95% 。");
            return Boolean.FALSE;
        }
        // 3、如果没有背景值则无需检查
        if (StringUtils.isBlank(screenshot.getBgUrl())) {
            return Boolean.TRUE;
        }
        // 4、如果背景值不为空则检查背景值与截图的相似度
        Optional<BufferedImage> optional = IMAGE_CACHES.get(screenshot.getBgUrl());
        if (Objects.isNull(optional) || !optional.isPresent()) {
            log.warn("Background Image Pixels {} not found", screenshot.getBgUrl());
            return Boolean.TRUE;
        }
        // 5、如果背景值存在则检查背景值与截图的相似度
        try {
            BufferedImage background = ImageUtil.scaleTo(optional.get(), pdfPageSize, 100);
            pdfImage = ImageUtil.scaleTo(optional.get(), pdfPageSize, 100);
            double similarity = FastImageComparator.compare(new ImagePixelCache(background), new ImagePixelCache(pdfImage));
            if (similarity > maxSimilarity){
                screenshot.setRenderState(RenderState.CHECK_FAIL);
                screenshot.setFailedReason("截图图片检查未通过，背景图片相似度过高。");
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("Background Image Pixels {} load error: {}", screenshot.getBgUrl(), e.getMessage());
            screenshot.setRenderState(RenderState.CHECK_FAIL);
            screenshot.setFailedReason("截图图片为空，无法添加到PDF中。");
            return Boolean.FALSE;
        }
    }

}
