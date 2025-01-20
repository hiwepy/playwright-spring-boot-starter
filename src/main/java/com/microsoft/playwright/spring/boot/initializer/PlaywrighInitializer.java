package com.microsoft.playwright.spring.boot.initializer;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;

public class PlaywrighInitializer implements Runnable {

    private final PlaywrightProperties playwrightProperties;

    public PlaywrighInitializer(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
    }


    @Override
    public void run() {
        // Create Playwright Instance .
        Playwright playwright = PlaywrightUtil.getInstance();
        // Lanuch Browser
        BrowserType.LaunchOptions options = playwrightProperties.getLaunchOptions().toOptions();
        PlaywrightUtil.getBrowser(playwright, playwrightProperties.getBrowserType(), options);
    }

}
