package com.microsoft.playwright.spring.boot.utils;


import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class PlaywrightManager {

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

}
