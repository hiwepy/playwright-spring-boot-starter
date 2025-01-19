package com.microsoft.playwright.spring.boot.initializer;

import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;

public class PlaywrighInitializer implements Runnable {




    @Override
    public void run() {
        // Create Playwright Instance .
        Playwright playwright = PlaywrightUtil.getInstance();
        // Lanuch Browser
        PlaywrightUtil.launchBrowser(Playwright);
    }

}
