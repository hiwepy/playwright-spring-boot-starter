package com.microsoft.playwright.spring.boot.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 */
@Slf4j
public class PlaywrightUtil {

    private static final String TOKEN_SPLITTER = ";";
    private static ThreadLocal<Browser> browserInstance = new TransmittableThreadLocal<Browser>();
    private static ThreadLocal<Playwright> playwrightInstance = new TransmittableThreadLocal<>();

    public static synchronized Playwright getInstance() {
        Playwright playwright = playwrightInstance.get();
        if(Objects.isNull(playwright)){
            log.info("Create Playwright Instance .");
            playwright = Playwright.create();
            playwrightInstance.set(playwright);
            log.info("Playwright instance created.");
        }
        return playwright;
    }

    public static Browser getBrowser(Playwright playwright, PlaywrightProperties.BrowserTypeEnum browserTypeEnum, BrowserType.LaunchOptions launchOptions) {
        log.info("Get Browser Instance .");
        Browser browser = browserInstance.get();
        if (Objects.nonNull(browser) && !browser.isConnected()) {
            return browser;
        }
        BrowserType browserType = browserTypeEnum.getBrowserType(playwright);
        browser = browserType.launch(launchOptions);
        log.info("Create Browser Instance .");
        browserInstance.set(browser);
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
