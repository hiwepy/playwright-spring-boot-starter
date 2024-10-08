package com.microsoft.playwright.spring.boot.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.PropertyMapper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class PlaywrightUtil {

    protected static final PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

    private static ThreadLocal<String> ttl = new TransmittableThreadLocal<String>();

    public static void setUerDataDir(String userDataDir) {
        ttl.set(userDataDir);
    }

    public static String getUerDataDir() {
        return ttl.get();
    }

    private static final String TOKEN_SPLITTER = ";";

    /**
     * 获取cookie
     *
     * @param page page
     * @return cookie
     */
    public static String getCookies(Page page) {
        return cookieToString(page.context().cookies());
    }

    /**
     * 获取cookie
     *
     * @param cookies cookies
     * @return cookie
     */
    public static String cookieToString(List<Cookie> cookies) {
        return cookies.stream().map(cookie -> cookie.name + "=" + cookie.value).collect(Collectors.joining(TOKEN_SPLITTER));
    }

    /**
     * 清空localStorage
     *
     * @param page page
     */
    public static void clearLocalStorage(Page page) {
        page.evaluate("window.localStorage.clear();");
    }

    /**
     * 滑动滑块
     *
     * @param page page
     * @param slideElementPath slideElementPath
     * @param slideLength slideLength
     * @param steps steps
     */
    public static void slide(Page page, String slideElementPath, int slideLength, int steps) {
        slide(page, page.waitForSelector(slideElementPath, new Page.WaitForSelectorOptions().setTimeout(TimeUnit.SECONDS.toMillis(5))), slideLength, steps);
    }

    /**
     * 滑动滑块
     *
     * @param page page
     * @param elementHandle elementHandle
     * @param slideLength slideLength
     * @param steps steps
     */
    public static void slide(Page page, ElementHandle elementHandle, int slideLength, int steps) {
        Mouse mouse = page.mouse();
        mouse.move(elementHandle.boundingBox().x, elementHandle.boundingBox().y);
        mouse.down(new Mouse.DownOptions().setButton(MouseButton.LEFT));
        mouse.move(elementHandle.boundingBox().x + slideLength, elementHandle.boundingBox().y, new Mouse.MoveOptions().setSteps(steps));
        mouse.up();
    }

    public static BrowserType getBrowserType(Playwright playwright, PlaywrightProperties.BrowserType browserType) {
        switch (browserType) {
            case chromium:
                return playwright.chromium();
            case webkit:
                return playwright.webkit();
            case firefox:
                return playwright.firefox();
            default:
                throw new IllegalArgumentException("browserType is not supported");
        }
    }

    public static void closePage(Page page) {
        try {
            if (Objects.nonNull(page) && !page.isClosed()){
                page.close();
            }
        } catch (Exception e) {
            log.error("Close Page Error.", e);
            // ignore error
        }
    }

}
