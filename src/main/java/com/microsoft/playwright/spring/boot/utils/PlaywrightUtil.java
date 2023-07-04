package com.microsoft.playwright.spring.boot.utils;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.CollectionUtils;

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

    public static void copyProperties(PlaywrightProperties.LaunchOptions source, BrowserType.LaunchOptions options) {
        if (Objects.isNull(source) || Objects.isNull(options)) {
            return;
        }
        map.from(source.getArgs()).when(args -> !CollectionUtils.isEmpty(args)).to(options::setArgs);
        map.from(source.getChannel()).whenHasText().to(options::setChannel);
        map.from(source.getChromiumSandbox()).whenNonNull().to(options::setChromiumSandbox);
        map.from(source.getDevtools()).whenNonNull().to(options::setDevtools);
        map.from(source.getDownloadsPath()).whenNonNull().to(options::setDownloadsPath);
        map.from(source.getEnv()).when(env -> !CollectionUtils.isEmpty(env)).to(options::setEnv);
        map.from(source.getExecutablePath()).whenNonNull().to(options::setExecutablePath);
        map.from(source.getFirefoxUserPrefs()).when(firefoxUserPrefs -> !CollectionUtils.isEmpty(firefoxUserPrefs)).to(options::setFirefoxUserPrefs);
        map.from(source.getHandleSighup()).whenNonNull().to(options::setHandleSIGHUP);
        map.from(source.getHandleSigint()).whenNonNull().to(options::setHandleSIGINT);
        map.from(source.getHandleSigterm()).whenNonNull().to(options::setHandleSIGTERM);
        map.from(source.getHeadless()).whenNonNull().to(options::setHeadless);
        map.from(source.getIgnoreAllDefaultArgs()).whenNonNull().to(options::setIgnoreAllDefaultArgs);
        map.from(source.getIgnoreDefaultArgs()).when(ignoreDefaultArgs -> !CollectionUtils.isEmpty(ignoreDefaultArgs)).to(options::setIgnoreDefaultArgs);
        map.from(source.getProxy()).whenNonNull().to(options::setProxy);
        map.from(source.getSlowMo()).whenNonNull().to(options::setSlowMo);
        map.from(source.getTimeout()).whenNonNull().to(options::setTimeout);
        map.from(source.getTracesDir()).whenNonNull().to(options::setTracesDir);
    }

    public static void copyProperties(PlaywrightProperties.LaunchPersistentOptions source, BrowserType.LaunchPersistentContextOptions options) {
        if (Objects.isNull(source) || Objects.isNull(options)) {
            return;
        }
        map.from(source.getAcceptDownloads()).whenNonNull().to(options::setAcceptDownloads);
        map.from(source.getArgs()).when(args -> !CollectionUtils.isEmpty(args)).to(options::setArgs);
        map.from(source.getBaseURL()).whenHasText().to(options::setBaseURL);
        map.from(source.getBypassCSP()).whenNonNull().to(options::setBypassCSP);
        map.from(source.getChannel()).whenHasText().to(options::setChannel);
        map.from(source.getChromiumSandbox()).whenNonNull().to(options::setChromiumSandbox);
        map.from(source.getColorScheme()).whenNonNull().to(options::setColorScheme);
        map.from(source.getDeviceScaleFactor()).whenNonNull().to(options::setDeviceScaleFactor);
        map.from(source.getDevtools()).whenNonNull().to(options::setDevtools);
        map.from(source.getDownloadsPath()).whenNonNull().to(options::setDownloadsPath);
        map.from(source.getEnv()).when(env -> !CollectionUtils.isEmpty(env)).to(options::setEnv);
        map.from(source.getExecutablePath()).whenNonNull().to(options::setExecutablePath);
        map.from(source.getExtraHttpHeaders()).whenNonNull().to(options::setExtraHTTPHeaders);
        map.from(source.getForcedColors()).whenNonNull().to(options::setForcedColors);
        map.from(source.getGeolocation()).whenNonNull().to(options::setGeolocation);
        map.from(source.getHandleSighup()).whenNonNull().to(options::setHandleSIGHUP);
        map.from(source.getHandleSigint()).whenNonNull().to(options::setHandleSIGINT);
        map.from(source.getHandleSigterm()).whenNonNull().to(options::setHandleSIGTERM);
        map.from(source.getHasTouch()).whenNonNull().to(options::setHasTouch);
        map.from(source.getHeadless()).whenNonNull().to(options::setHeadless);
        map.from(source.getHttpCredentials()).whenNonNull().to(options::setHttpCredentials);
        map.from(source.getIgnoreAllDefaultArgs()).whenNonNull().to(options::setIgnoreAllDefaultArgs);
        map.from(source.getIgnoreDefaultArgs()).when(ignoreDefaultArgs -> !CollectionUtils.isEmpty(ignoreDefaultArgs)).to(options::setIgnoreDefaultArgs);
        map.from(source.getIgnoreHttpsErrors()).whenNonNull().to(options::setIgnoreHTTPSErrors);
        map.from(source.getIsMobile()).whenNonNull().to(options::setIsMobile);
        map.from(source.getJavaScriptEnabled()).whenNonNull().to(options::setJavaScriptEnabled);
        map.from(source.getLocale()).whenHasText().to(options::setLocale);
        map.from(source.getOffline()).whenNonNull().to(options::setOffline);
        map.from(source.getPermissions()).when(permissions -> !CollectionUtils.isEmpty(permissions)).to(options::setPermissions);
        map.from(source.getProxy()).whenNonNull().to(options::setProxy);
        map.from(source.getRecordHarMode()).whenNonNull().to(options::setRecordHarMode);
        map.from(source.getRecordHarContent()).whenNonNull().to(options::setRecordHarContent);
        map.from(source.getRecordHarOmitContent()).whenNonNull().to(options::setRecordHarOmitContent);
        map.from(source.getRecordHarPath()).whenNonNull().to(options::setRecordHarPath);
        map.from(source.getRecordHarUrlFilter()).whenHasText().to(options::setRecordHarUrlFilter);
        map.from(source.getRecordVideoDir()).whenNonNull().to(options::setRecordVideoDir);
        map.from(source.getRecordVideoSize()).whenNonNull().to(options::setRecordVideoSize);
        map.from(source.getReducedMotion()).whenNonNull().to(options::setReducedMotion);
        map.from(source.getScreenSize()).whenNonNull().to(options::setScreenSize);
        map.from(source.getServiceWorkers()).whenNonNull().to(options::setServiceWorkers);
        map.from(source.getSlowMo()).whenNonNull().to(options::setSlowMo);
        map.from(source.getStrictSelectors()).whenNonNull().to(options::setStrictSelectors);
        map.from(source.getTimeout()).whenNonNull().to(options::setTimeout);
        map.from(source.getTimezoneId()).whenHasText().to(options::setTimezoneId);
        map.from(source.getTracesDir()).whenNonNull().to(options::setTracesDir);
        map.from(source.getUserAgent()).whenHasText().to(options::setUserAgent);
        map.from(source.getViewportSize()).whenNonNull().to(options::setViewportSize);
    }

    public static void copyProperties(PlaywrightProperties.ConnectOptions source, BrowserType.ConnectOptions options) {
        if (Objects.isNull(source) || Objects.isNull(options)) {
            return;
        }
        map.from(source.getHeaders()).whenNonNull().to(options::setHeaders);
        map.from(source.getSlowMo()).whenNonNull().to(options::setSlowMo);
        map.from(source.getTimeout()).whenNonNull().to(options::setTimeout);
    }

    public static void copyProperties(PlaywrightProperties.NewContextOptions source, Browser.NewContextOptions options) {
        if (Objects.isNull(source) || Objects.isNull(options)) {
            return;
        }
        map.from(source.getAcceptDownloads()).whenNonNull().to(options::setAcceptDownloads);
        map.from(source.getBaseURL()).whenHasText().to(options::setBaseURL);
        map.from(source.getBypassCSP()).whenNonNull().to(options::setBypassCSP);
        map.from(source.getColorScheme()).to(options::setColorScheme);
        map.from(source.getDeviceScaleFactor()).whenNonNull().to(options::setDeviceScaleFactor);
        map.from(source.getExtraHttpHeaders()).whenNonNull().to(options::setExtraHTTPHeaders);
        map.from(source.getForcedColors()).whenNonNull().to(options::setForcedColors);
        map.from(source.getGeolocation()).whenNonNull().to(options::setGeolocation);
        map.from(source.getHasTouch()).whenNonNull().to(options::setHasTouch);
        map.from(source.getHttpCredentials()).whenNonNull().to(options::setHttpCredentials);
        map.from(source.getIgnoreHttpsErrors()).whenNonNull().to(options::setIgnoreHTTPSErrors);
        map.from(source.getIsMobile()).whenNonNull().to(options::setIsMobile);
        map.from(source.getJavaScriptEnabled()).whenNonNull().to(options::setJavaScriptEnabled);
        map.from(source.getLocale()).whenHasText().to(options::setLocale);
        map.from(source.getOffline()).whenNonNull().to(options::setOffline);
        map.from(source.getPermissions()).when(permissions -> !CollectionUtils.isEmpty(permissions)).to(options::setPermissions);
        map.from(source.getProxy()).whenNonNull().to(options::setProxy);
        map.from(source.getRecordHarMode()).whenNonNull().to(options::setRecordHarMode);
        map.from(source.getRecordHarContent()).whenNonNull().to(options::setRecordHarContent);
        map.from(source.getRecordHarOmitContent()).whenNonNull().to(options::setRecordHarOmitContent);
        map.from(source.getRecordHarPath()).whenNonNull().to(options::setRecordHarPath);
        map.from(source.getRecordHarUrlFilter()).whenHasText().to(options::setRecordHarUrlFilter);
        map.from(source.getRecordVideoDir()).whenNonNull().to(options::setRecordVideoDir);
        map.from(source.getRecordVideoSize()).whenNonNull().to(options::setRecordVideoSize);
        map.from(source.getReducedMotion()).whenNonNull().to(options::setReducedMotion);
        map.from(source.getScreenSize()).whenNonNull().to(options::setScreenSize);
        map.from(source.getServiceWorkers()).whenNonNull().to(options::setServiceWorkers);
        map.from(source.getStorageState()).whenHasText().to(options::setStorageState);
        map.from(source.getStorageStatePath()).whenNonNull().to(options::setStorageStatePath);
        map.from(source.getStrictSelectors()).whenNonNull().to(options::setStrictSelectors);
        map.from(source.getTimezoneId()).whenHasText().to(options::setTimezoneId);
        map.from(source.getUserAgent()).whenHasText().to(options::setUserAgent);
        map.from(source.getViewportSize()).whenNonNull().to(options::setViewportSize);
    }

    public static void copyProperties(PlaywrightProperties.ObjectPoolConfig source, GenericObjectPoolConfig poolConfig){
        if (Objects.isNull(source) || Objects.isNull(poolConfig)){
            return;
        }
        map.from(source.isBlockWhenExhausted()).to(poolConfig::setBlockWhenExhausted);
        map.from(source.getDurationBetweenEvictionRuns()).whenNonNull().to(poolConfig::setTimeBetweenEvictionRuns);
        map.from(source.getEvictionPolicyClassName()).whenHasText().to(poolConfig::setEvictionPolicyClassName);
        map.from(source.getEvictorShutdownTimeoutDuration()).whenNonNull().to(poolConfig::setEvictorShutdownTimeout);
        map.from(source.isFairness()).to(poolConfig::setFairness);
        map.from(source.isLifo()).to(poolConfig::setLifo);
        map.from(source.getMaxWaitDuration()).whenNonNull().to(poolConfig::setMaxWait);
        map.from(source.getMaxIdle()).to(poolConfig::setMaxIdle);
        map.from(source.getMaxTotal()).to(poolConfig::setMaxTotal);
        map.from(source.getMinEvictableIdleDuration()).whenNonNull().to(poolConfig::setMinEvictableIdleTime);
        map.from(source.getMinIdle()).to(poolConfig::setMinIdle);
        map.from(source.getNumTestsPerEvictionRun()).to(poolConfig::setNumTestsPerEvictionRun);
        map.from(source.getSoftMinEvictableIdleDuration()).whenNonNull().to(poolConfig::setSoftMinEvictableIdleTime);
        map.from(source.isTestOnBorrow()).to(poolConfig::setTestOnBorrow);
        map.from(source.isTestOnCreate()).to(poolConfig::setTestOnCreate);
        map.from(source.isTestOnReturn()).to(poolConfig::setTestOnReturn);
        map.from(source.isTestWhileIdle()).to(poolConfig::setTestWhileIdle);
    }

    /**
     * 获取cookie
     *
     * @param page
     * @return
     */
    public static String getCookies(Page page) {
        return cookieToString(page.context().cookies());
    }

    /**
     * 获取cookie
     *
     * @param cookies
     * @return
     */
    public static String cookieToString(List<Cookie> cookies) {
        return cookies.stream().map(cookie -> cookie.name + "=" + cookie.value).collect(Collectors.joining(TOKEN_SPLITTER));
    }

    /**
     * 清空localStorage
     *
     * @param page
     */
    public static void clearLocalStorage(Page page) {
        page.evaluate("window.localStorage.clear();");
    }

    /**
     * 滑动滑块
     *
     * @param page
     * @param slideElementPath
     * @param slideLength
     * @param steps
     */
    public static void slide(Page page, String slideElementPath, int slideLength, int steps) {
        slide(page, page.waitForSelector(slideElementPath, new Page.WaitForSelectorOptions().setTimeout(TimeUnit.SECONDS.toMillis(5))), slideLength, steps);
    }

    /**
     * 滑动滑块
     *
     * @param page
     * @param elementHandle
     * @param slideLength
     * @param steps
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
