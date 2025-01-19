package com.microsoft.playwright.spring.boot.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class PlaywrightUtil {

    private static final String TOKEN_SPLITTER = ";";

    private static Playwright playwright;

    public static synchronized Playwright getInstance() {
        if(Objects.isNull(playwright)){
            log.info("Create Playwright Instance .");
            playwright = Playwright.create();
            log.info("Playwright instance created.");
        }
        return playwright;
    }

    public static synchronized void close(Function<Playwright, ?> function) {
        if(Objects.nonNull(playwright)){
            playwright.close();
            playwright = null;
            log.info("Playwright instance closed.");
        }
    }

    private static Map<PlaywrightProperties.BrowserType, Browser> BROWSER_CACHE_MAP = new ConcurrentHashMap<>();

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

    public static Browser  launchBrowser(Playwright playwright, PlaywrightProperties.BrowserType browserType, BrowserType.LaunchOptions launchOptions) {
        BrowserType browserTypeObj = getBrowserType(playwright, browserType);
        Browser browser = BROWSER_CACHE_MAP.get(browserType);
        if (Objects.nonNull(browser) && !browser.isClosed()) {
            return browser;
        }
        BROWSER_CACHE_MAP.computeIfAbsent(browserType, k -> browserTypeObj.launch(launchOptions));
        BROWSER_CACHE_MAP.put(browserType, browserTypeObj.launch(launchOptions));
        return getBrowserType(playwright, browserType).launch(launchOptions);
    }

    public static void cleanupBrowser(Browser browser) {
        if (Objects.isNull(browser)) {
            return;
        }
        browser.contexts().forEach(context -> {
            List<Page> pages = context.pages();
            if (Objects.nonNull(pages) && !pages.isEmpty()) {
                pages.forEach(PlaywrightUtil::closePage);
            }
            context.clearCookies();
        });
    }

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

    public static void closePage(Page page) {
        try {
            if (Objects.nonNull(page) && !page.isClosed()){
                page.close();
                log.debug("Close page Instance Success.");
            }
        } catch (Exception e) {
            log.error("Close Page Error.", e);
            // ignore error
        }
    }

}
