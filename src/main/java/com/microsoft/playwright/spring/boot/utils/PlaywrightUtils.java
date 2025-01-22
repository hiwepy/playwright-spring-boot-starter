package com.microsoft.playwright.spring.boot.utils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class PlaywrightUtils {

    private static final String TOKEN_SPLITTER = ";";
    private static final Map<PlaywrightProperties.BrowserTypeEnum, Browser> BROWSER_CACHE_MAP = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    public static Browser getBrowser(Playwright playwright, PlaywrightProperties.BrowserTypeEnum browserType, BrowserType.LaunchOptions launchOptions) {
        Browser browser = BROWSER_CACHE_MAP.get(browserType);
        if (Objects.nonNull(browser)) {
            if (browser.isConnected()) {
                return browser;
            } else {
                // 如果浏览器已断开连接，从缓存中移除
                BROWSER_CACHE_MAP.remove(browserType);
            }
        }
        // 使用双重检查锁定模式创建浏览器实例
        synchronized (lock) {
            browser = BROWSER_CACHE_MAP.get(browserType);
            if (Objects.isNull(browser) || !browser.isConnected()) {
                BrowserType browserTypeObj = browserType.getBrowserType(playwright);
                browser = browserTypeObj.launch(launchOptions);
                BROWSER_CACHE_MAP.put(browserType, browser);
            }
        }
        return browser;
    }

    public static Browser getBrowser(Playwright playwright, PlaywrightProperties playwrightProperties) {
        // Browser Type
        PlaywrightProperties.BrowserTypeEnum browserType = Objects.nonNull(playwrightProperties.getBrowserType()) ? playwrightProperties.getBrowserType() : PlaywrightProperties.BrowserTypeEnum.chromium;
        // Get Browser Launch Options
        BrowserType.LaunchOptions launchOptions = Objects.nonNull(playwrightProperties.getLaunchOptions()) ? playwrightProperties.getLaunchOptions().toOptions() : new BrowserType.LaunchOptions().setHeadless(true);
        // Get Browser
        return getBrowser(playwright, browserType, launchOptions);
    }

    public static void cleanupBrowser(Browser browser) {
        if (Objects.isNull(browser)) {
            return;
        }
        browser.contexts().forEach(context -> {
            List<Page> pages = context.pages();
            if (Objects.nonNull(pages) && !pages.isEmpty()) {
                pages.forEach(PlaywrightUtils::closePage);
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

    public static void waitForPageLoad(Page page) {
        page.waitForLoadState(LoadState.LOAD);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public static byte[] takeScreenshot(Page page, String selector) {
        if (StringUtils.hasText(selector)) {
            return page.locator(selector).screenshot();
        }
        return page.screenshot();
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
