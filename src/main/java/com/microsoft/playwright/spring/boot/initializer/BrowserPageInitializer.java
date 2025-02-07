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
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class BrowserPageInitializer implements Runnable {

	private final BrowserPagePool browserPagePool;
	private final PlaywrightProperties playwrightProperties;

	public BrowserPageInitializer(BrowserPagePool browserPagePool, PlaywrightProperties playwrightProperties) {
		this.browserPagePool = browserPagePool;
		this.playwrightProperties = playwrightProperties;
	}

	@Override
	public void run() {
		if(Objects.nonNull(browserPagePool) && Objects.nonNull(playwrightProperties)){
			try {
				log.info("Browser Page Pool Start initialize ...");
				// 1、触发浏览器安装
				System.setProperty("PLAYWRIGHT_DOWNLOAD_HOST", playwrightProperties.getDownloadHost());
				browserPagePool.preparePool();
				log.info("Browser Page Pool is initialize completed.");
			} catch (Exception e) {
				log.error("Browser Page Pool initialize error", e);
			}
		}
	}

}
