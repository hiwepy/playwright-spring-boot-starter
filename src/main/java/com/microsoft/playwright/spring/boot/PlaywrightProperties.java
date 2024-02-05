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
package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.spring.boot.options.*;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPoolConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * @author ： <a href="https://github.com/hiwepy">hiwepy</a>
 */
@ConfigurationProperties(PlaywrightProperties.PREFIX)
@Data
public class PlaywrightProperties {

	public static final String PREFIX = "playwright";
	public static final String PLAYWRIGHT_DOWNLOAD_HOST = "https://npm.taobao.org/mirrors";
	private BrowserType browserType = BrowserType.chromium;

	private String downloadHost = PLAYWRIGHT_DOWNLOAD_HOST;

	/**
	 * Browser mode. Defaults to {@link BrowserMode#incognito incognito}.
	 */
	public BrowserMode browserMode = BrowserMode.incognito;
	/**
	 * Browser Pool Config
	 */
	private BrowserContextPoolConfig browserPool = new BrowserContextPoolConfig();
	/**
	 * Connect Options
	 */
	private BrowserConnectOptions connectOptions = new BrowserConnectOptions();
	/**
	 * Launch Options
	 */
	private BrowserLaunchOptions launchOptions = new BrowserLaunchOptions();
	/**
	 * Launch Persistent Options
	 */
	private BrowserLaunchPersistentOptions launchPersistentOptions = new BrowserLaunchPersistentOptions();
	/**
	 * New Context Options
	 */
	private BrowserNewContextOptions newContextOptions = new BrowserNewContextOptions();
	/**
	 * Page Navigate Options
	 */
	private PageNavigateOptions pageNavigateOptions = new PageNavigateOptions();
	/**
	 * Page Screenshot Options
	 */
	private PageScreenshotOptions pageScreenshotOptions = new PageScreenshotOptions();
	/**
	 * Page Set Content Options
	 */
	private ElementScreenshotOptions elementScreenshotOptions = new ElementScreenshotOptions();

	public enum BrowserType {
		chromium,
		firefox,
		webkit
	}

	public enum BrowserMode {

		/**
		 * Returns the incognito mode browser instance.
		 */
		incognito,
		/**
		 * Returns the persistent browser context instance.
		 */
		persistent
	}

}
