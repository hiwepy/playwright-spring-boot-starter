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

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author ： <a href="https://github.com/hiwepy">hiwepy</a>
 */
@ConfigurationProperties(PlaywrightProperties.PREFIX)
@Data
public class PlaywrightProperties {

	public static final String PREFIX = "playwright";

	private BrowserType browser = BrowserType.CHROMIUM;

	private ConnectOptions connectOptions = new ConnectOptions();

	private LaunchOptions launchOptions = new LaunchOptions();

	private NewContextOptions newContextOptions = new NewContextOptions();

	private ObjectPoolConfig browserPool = new ObjectPoolConfig();

	public enum BrowserType {
		CHROMIUM,
		FIREFOX,
		WEBKIT
	}

	@Data
	public static class Cookie {

		public String name;
		public String value;

	}


	@Data
	public static class ConnectOptions {
		/**
		 * Additional HTTP headers to be sent with web socket connect request. Optional.
		 */
		public Map<String, String> headers;
		/**
		 * Slows down Playwright operations by the specified amount of milliseconds. Useful so that you can see what is going on.
		 * Defaults to 0.
		 */
		public Double slowMo;
		/**
		 * Maximum time in milliseconds to wait for the connection to be established. Defaults to {@code 0} (no timeout).
		 */
		public Double timeout;
	}

	@Data
	public static class LaunchOptions {

		/**
		 * Additional arguments to pass to the browser instance. The list of Chromium flags can be found <a
		 * href="http://peter.sh/experiments/chromium-command-line-switches/">here</a>.
		 */
		public List<String> args;
		/**
		 * Browser distribution channel.  Supported values are "chrome", "chrome-beta", "chrome-dev", "chrome-canary", "msedge",
		 * "msedge-beta", "msedge-dev", "msedge-canary". Read more about using <a
		 * href="https://playwright.dev/java/docs/browsers#google-chrome--microsoft-edge">Google Chrome and Microsoft Edge</a>.
		 */
		public String channel;
		/**
		 * Enable Chromium sandboxing. Defaults to {@code false}.
		 */
		public Boolean chromiumSandbox;
		/**
		 * **Chromium-only** Whether to auto-open a Developer Tools panel for each tab. If this option is {@code true}, the {@code
		 * headless} option will be set {@code false}.
		 */
		public Boolean devtools;
		/**
		 * If specified, accepted downloads are downloaded into this directory. Otherwise, temporary directory is created and is
		 * deleted when browser is closed. In either case, the downloads are deleted when the browser context they were created in
		 * is closed.
		 */
		public Path downloadsPath;
		/**
		 * Specify environment variables that will be visible to the browser. Defaults to {@code process.env}.
		 */
		public Map<String, String> env;
		/**
		 * Path to a browser executable to run instead of the bundled one. If {@code executablePath} is a relative path, then it is
		 * resolved relative to the current working directory. Note that Playwright only works with the bundled Chromium, Firefox
		 * or WebKit, use at your own risk.
		 */
		public Path executablePath;
		/**
		 * Firefox user preferences. Learn more about the Firefox user preferences at <a
		 * href="https://support.mozilla.org/en-US/kb/about-config-editor-firefox">{@code about:config}</a>.
		 */
		public Map<String, Object> firefoxUserPrefs;
		/**
		 * Close the browser process on SIGHUP. Defaults to {@code true}.
		 */
		public Boolean handleSighup;
		/**
		 * Close the browser process on Ctrl-C. Defaults to {@code true}.
		 */
		public Boolean handleSigint;
		/**
		 * Close the browser process on SIGTERM. Defaults to {@code true}.
		 */
		public Boolean handleSigterm;
		/**
		 * Whether to run browser in headless mode. More details for <a
		 * href="https://developers.google.com/web/updates/2017/04/headless-chrome">Chromium</a> and <a
		 * href="https://developer.mozilla.org/en-US/docs/Mozilla/Firefox/Headless_mode">Firefox</a>. Defaults to {@code true}
		 * unless the {@code devtools} option is {@code true}.
		 */
		public Boolean headless;
		/**
		 * If {@code true}, Playwright does not pass its own configurations args and only uses the ones from {@code args}.
		 * Dangerous option; use with care. Defaults to {@code false}.
		 */
		public Boolean ignoreAllDefaultArgs;
		/**
		 * If {@code true}, Playwright does not pass its own configurations args and only uses the ones from {@code args}.
		 * Dangerous option; use with care.
		 */
		public List<String> ignoreDefaultArgs;
		/**
		 * Network proxy settings.
		 */
		public Proxy proxy;
		/**
		 * Slows down Playwright operations by the specified amount of milliseconds. Useful so that you can see what is going on.
		 */
		public Double slowMo;
		/**
		 * Maximum time in milliseconds to wait for the browser instance to start. Defaults to {@code 30000} (30 seconds). Pass
		 * {@code 0} to disable timeout.
		 */
		public Double timeout;
		/**
		 * If specified, traces are saved into this directory.
		 */
		public Path tracesDir;
	}

	@Data
	public static class NewContextOptions {
		/**
		 * Whether to automatically download all the attachments. Defaults to {@code true} where all the downloads are accepted.
		 */
		public Boolean acceptDownloads;
		/**
		 * When using {@link Page#navigate Page.navigate()}, {@link Page#route Page.route()}, {@link Page#waitForURL
		 * Page.waitForURL()}, {@link Page#waitForRequest Page.waitForRequest()}, or {@link Page#waitForResponse
		 * Page.waitForResponse()} it takes the base URL in consideration by using the <a
		 * href="https://developer.mozilla.org/en-US/docs/Web/API/URL/URL">{@code URL()}</a> constructor for building the
		 * corresponding URL. Examples:
		 * <ul>
		 * <li> baseURL: {@code http://localhost:3000} and navigating to {@code /bar.html} results in {@code
		 * http://localhost:3000/bar.html}</li>
		 * <li> baseURL: {@code http://localhost:3000/foo/} and navigating to {@code ./bar.html} results in {@code
		 * http://localhost:3000/foo/bar.html}</li>
		 * <li> baseURL: {@code http://localhost:3000/foo} (without trailing slash) and navigating to {@code ./bar.html} results in
		 * {@code http://localhost:3000/bar.html}</li>
		 * </ul>
		 */
		public String baseURL;
		/**
		 * Toggles bypassing page's Content-Security-Policy.
		 */
		public Boolean bypassCSP;
		/**
		 * Emulates {@code "prefers-colors-scheme"} media feature, supported values are {@code "light"}, {@code "dark"}, {@code
		 * "no-preference"}. See {@link Page#emulateMedia Page.emulateMedia()} for more details. Passing {@code null} resets
		 * emulation to system defaults. Defaults to {@code "light"}.
		 */
		public ColorScheme colorScheme;
		/**
		 * Specify device scale factor (can be thought of as dpr). Defaults to {@code 1}. Learn more about <a
		 * href="https://playwright.dev/java/docs/emulation#devices">emulating devices with device scale factor</a>.
		 */
		public Double deviceScaleFactor;
		/**
		 * An object containing additional HTTP headers to be sent with every request.
		 */
		public Map<String, String> extraHttpHeaders;
		/**
		 * Emulates {@code "forced-colors"} media feature, supported values are {@code "active"}, {@code "none"}. See {@link
		 * Page#emulateMedia Page.emulateMedia()} for more details. Passing {@code null} resets emulation to system defaults.
		 * Defaults to {@code "none"}.
		 */
		public ForcedColors forcedColors;
		public Geolocation geolocation;
		/**
		 * Specifies if viewport supports touch events. Defaults to false. Learn more about <a
		 * href="https://playwright.dev/java/docs/emulation#devices">mobile emulation</a>.
		 */
		public Boolean hasTouch;
		/**
		 * Credentials for <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication">HTTP authentication</a>. If
		 * no origin is specified, the username and password are sent to any servers upon unauthorized responses.
		 */
		public HttpCredentials httpCredentials;
		/**
		 * Whether to ignore HTTPS errors when sending network requests. Defaults to {@code false}.
		 */
		public Boolean ignoreHttpsErrors;
		/**
		 * Whether the {@code meta viewport} tag is taken into account and touch events are enabled. isMobile is a part of device,
		 * so you don't actually need to set it manually. Defaults to {@code false} and is not supported in Firefox. Learn more
		 * about <a href="https://playwright.dev/java/docs/emulation#isMobile">mobile emulation</a>.
		 */
		public Boolean isMobile;
		/**
		 * Whether or not to enable JavaScript in the context. Defaults to {@code true}. Learn more about <a
		 * href="https://playwright.dev/java/docs/emulation#javascript-enabled">disabling JavaScript</a>.
		 */
		public Boolean javaScriptEnabled;
		/**
		 * Specify user locale, for example {@code en-GB}, {@code de-DE}, etc. Locale will affect {@code navigator.language} value,
		 * {@code Accept-Language} request header value as well as number and date formatting rules. Learn more about emulation in
		 * our <a href="https://playwright.dev/java/docs/emulation#locale--timezone">emulation guide</a>.
		 */
		public String locale;
		/**
		 * Whether to emulate network being offline. Defaults to {@code false}. Learn more about <a
		 * href="https://playwright.dev/java/docs/emulation#offline">network emulation</a>.
		 */
		public Boolean offline;
		/**
		 * A list of permissions to grant to all pages in this context. See {@link BrowserContext#grantPermissions
		 * BrowserContext.grantPermissions()} for more details.
		 */
		public List<String> permissions;
		/**
		 * Network proxy settings to use with this context.
		 *
		 * <p> <strong>NOTE:</strong> For Chromium on Windows the browser needs to be launched with the global proxy for this option to work. If all contexts
		 * override the proxy, global proxy will be never used and can be any string, for example {@code launch({ proxy: { server:
		 * 'http://per-context' } })}.
		 */
		public Proxy proxy;
		/**
		 * Optional setting to control resource content management. If {@code omit} is specified, content is not persisted. If
		 * {@code attach} is specified, resources are persisted as separate files and all of these files are archived along with
		 * the HAR file. Defaults to {@code embed}, which stores content inline the HAR file as per HAR specification.
		 */
		public HarContentPolicy recordHarContent;
		/**
		 * When set to {@code minimal}, only record information necessary for routing from HAR. This omits sizes, timing, page,
		 * cookies, security and other types of HAR information that are not used when replaying from HAR. Defaults to {@code
		 * full}.
		 */
		public HarMode recordHarMode;
		/**
		 * Optional setting to control whether to omit request content from the HAR. Defaults to {@code false}.
		 */
		public Boolean recordHarOmitContent;
		/**
		 * Enables <a href="http://www.softwareishard.com/blog/har-12-spec">HAR</a> recording for all pages into the specified HAR
		 * file on the filesystem. If not specified, the HAR is not recorded. Make sure to call {@link BrowserContext#close
		 * BrowserContext.close()} for the HAR to be saved.
		 */
		public Path recordHarPath;
		public String recordHarUrlFilter;
		/**
		 * Enables video recording for all pages into the specified directory. If not specified videos are not recorded. Make sure
		 * to call {@link BrowserContext#close BrowserContext.close()} for videos to be saved.
		 */
		public Path recordVideoDir;
		/**
		 * Dimensions of the recorded videos. If not specified the size will be equal to {@code viewport} scaled down to fit into
		 * 800x800. If {@code viewport} is not configured explicitly the video size defaults to 800x450. Actual picture of each
		 * page will be scaled down if necessary to fit the specified size.
		 */
		public RecordVideoSize recordVideoSize;
		/**
		 * Emulates {@code "prefers-reduced-motion"} media feature, supported values are {@code "reduce"}, {@code "no-preference"}.
		 * See {@link Page#emulateMedia Page.emulateMedia()} for more details. Passing {@code null} resets emulation to system
		 * defaults. Defaults to {@code "no-preference"}.
		 */
		public ReducedMotion reducedMotion;
		/**
		 * Emulates consistent window screen size available inside web page via {@code window.screen}. Is only used when the {@code
		 * viewport} is set.
		 */
		public ScreenSize screenSize;
		/**
		 * Whether to allow sites to register Service workers. Defaults to {@code "allow"}.
		 * <ul>
		 * <li> {@code "allow"}: <a href="https://developer.mozilla.org/en-US/docs/Web/API/Service_Worker_API">Service Workers</a> can
		 * be registered.</li>
		 * <li> {@code "block"}: Playwright will block all registration of Service Workers.</li>
		 * </ul>
		 */
		public ServiceWorkerPolicy serviceWorkers;
		/**
		 * Populates context with given storage state. This option can be used to initialize context with logged-in information
		 * obtained via {@link BrowserContext#storageState BrowserContext.storageState()}.
		 */
		public String storageState;
		/**
		 * Populates context with given storage state. This option can be used to initialize context with logged-in information
		 * obtained via {@link BrowserContext#storageState BrowserContext.storageState()}. Path to the file with saved storage
		 * state.
		 */
		public Path storageStatePath;
		/**
		 * If set to true, enables strict selectors mode for this context. In the strict selectors mode all operations on selectors
		 * that imply single target DOM element will throw when more than one element matches the selector. This option does not
		 * affect any Locator APIs (Locators are always strict). See {@code Locator} to learn more about the strict mode.
		 */
		public Boolean strictSelectors;
		/**
		 * Changes the timezone of the context. See <a
		 * href="https://cs.chromium.org/chromium/src/third_party/icu/source/data/misc/metaZones.txt?rcl=faee8bc70570192d82d2978a71e2a615788597d1">ICU's
		 * metaZones.txt</a> for a list of supported timezone IDs.
		 */
		public String timezoneId;
		/**
		 * Specific user agent to use in this context.
		 */
		public String userAgent;
		/**
		 * Emulates consistent viewport for each page. Defaults to an 1280x720 viewport.  Use {@code null} to disable the
		 * consistent viewport emulation. Learn more about <a href="https://playwright.dev/java/docs/emulation#viewport">viewport
		 * emulation</a>.
		 *
		 * <p> <strong>NOTE:</strong> The {@code null} value opts out from the default presets, makes viewport depend on the host window size defined by the
		 * operating system. It makes the execution of the tests non-deterministic.
		 */
		public ViewportSize viewportSize;

	}

	@Data
	public static class ObjectPoolConfig {

		private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

		private Duration durationBetweenEvictionRuns = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS;

		private Duration evictorShutdownTimeoutDuration = GenericObjectPoolConfig.DEFAULT_EVICTOR_SHUTDOWN_TIMEOUT;

		private String evictionPolicyClassName = GenericObjectPoolConfig.DEFAULT_EVICTION_POLICY_CLASS_NAME;

		private boolean fairness = GenericObjectPoolConfig.DEFAULT_FAIRNESS;

		private boolean lifo = GenericObjectPoolConfig.DEFAULT_LIFO;

		private Duration maxWaitDuration = GenericObjectPoolConfig.DEFAULT_MAX_WAIT;

		private int maxTotal = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

		private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

		private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

		private Duration minEvictableIdleDuration = GenericObjectPoolConfig. DEFAULT_MIN_EVICTABLE_IDLE_DURATION;

		private Duration softMinEvictableIdleDuration = GenericObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_DURATION;

		private int numTestsPerEvictionRun = GenericObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

		private boolean testOnCreate = GenericObjectPoolConfig.DEFAULT_TEST_ON_CREATE;

		private boolean testOnBorrow = GenericObjectPoolConfig.DEFAULT_TEST_ON_BORROW;

		private boolean testOnReturn = GenericObjectPoolConfig.DEFAULT_TEST_ON_RETURN;

		private boolean testWhileIdle = GenericObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE;

	}

}
