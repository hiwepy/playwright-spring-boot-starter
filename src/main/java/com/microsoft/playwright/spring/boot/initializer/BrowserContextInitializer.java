/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
