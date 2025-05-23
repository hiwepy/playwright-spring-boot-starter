package com.microsoft.playwright.spring.boot.initializer;

import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class BrowserContextInitializer implements Runnable {

	private final BrowserContextPool browserContextPool;
	private final PlaywrightProperties playwrightProperties;

	public BrowserContextInitializer(BrowserContextPool browserContextPool, PlaywrightProperties playwrightProperties) {
		this.browserContextPool = browserContextPool;
		this.playwrightProperties = playwrightProperties;
	}

	@Override
	public void run() {
		if(Objects.nonNull(browserContextPool) && Objects.nonNull(playwrightProperties)){
			try {
				log.info("Browser Context Pool Start initialize ...");
				// 1、触发浏览器安装
				// PLAYWRIGHT_CHROMIUM_DOWNLOAD_HOST
				// PLAYWRIGHT_FIREFOX_DOWNLOAD_HOST
				// PLAYWRIGHT_WEBKIT_DOWNLOAD_HOST
				// PLAYWRIGHT_DOWNLOAD_CONNECTION_TIMEOUT
				System.setProperty("PLAYWRIGHT_DOWNLOAD_HOST", playwrightProperties.getDownloadHost());
				browserContextPool.preparePool();
				log.info("Browser Context Pool is initialize completed.");
			} catch (Exception e) {
				log.error("Browser Context Pool initialize error", e);
			}
		}
	}

}
