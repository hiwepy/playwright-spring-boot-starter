package com.microsoft.playwright.spring.boot.options;


import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.*;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
@Data
public class BrowserLaunchPersistentOptions {

    /**
     * Whether to automatically download all the attachments. Defaults to {@code true} where all the downloads are accepted.
     */
    public Boolean acceptDownloads;
    /**
     * Additional arguments to pass to the browser instance. The list of Chromium flags can be found <a
     * href="http://peter.sh/experiments/chromium-command-line-switches/">here</a>.
     */
    public List<String> args;
    /**
     * When using {@link Page#navigate Page.navigate()}, {@link Page#route Page.route()}, {@link Page#waitForURL
     * Page.waitForURL()}, {@link Page#waitForRequest Page.waitForRequest()}, or {@link Page#waitForResponse
     * Page.waitForResponse()} it takes the base URL in consideration by using the <a
     * href="https://developer.mozilla.org/en-US/docs/Web/API/URL/URL">{@code URL()}</a> constructor for building the
     * corresponding URL. Unset by default. Examples:
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
     * Toggles bypassing page's Content-Security-Policy. Defaults to {@code false}.
     */
    public Boolean bypassCSP;
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
     * An object containing additional HTTP headers to be sent with every request. Defaults to none.
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
     * Specifies if viewport supports touch events. Defaults to false. Learn more about <a
     * href="https://playwright.dev/java/docs/emulation#devices">mobile emulation</a>.
     */
    public Boolean hasTouch;
    /**
     * Whether to run browser in headless mode. More details for <a
     * href="https://developers.google.com/web/updates/2017/04/headless-chrome">Chromium</a> and <a
     * href="https://developer.mozilla.org/en-US/docs/Mozilla/Firefox/Headless_mode">Firefox</a>. Defaults to {@code true}
     * unless the {@code devtools} option is {@code true}.
     */
    public Boolean headless;
    /**
     * Credentials for <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication">HTTP authentication</a>. If
     * no origin is specified, the username and password are sent to any servers upon unauthorized responses.
     */
    public HttpCredentials httpCredentials;
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
     * {@code Accept-Language} request header value as well as number and date formatting rules. Defaults to the system default
     * locale. Learn more about emulation in our <a
     * href="https://playwright.dev/java/docs/emulation#locale--timezone">emulation guide</a>.
     */
    public String locale;
    /**
     * Whether to emulate network being offline. Defaults to {@code false}. Learn more about <a
     * href="https://playwright.dev/java/docs/emulation#offline">network emulation</a>.
     */
    public Boolean offline;
    /**
     * A list of permissions to grant to all pages in this context. See {@link BrowserContext#grantPermissions
     * BrowserContext.grantPermissions()} for more details. Defaults to none.
     */
    public List<String> permissions;
    /**
     * Network proxy settings.
     */
    public Proxy proxy;
    /**
     * Optional setting to control rethis content management. If {@code omit} is specified, content is not persisted. If
     * {@code attach} is specified, rethiss are persisted as separate files and all of these files are archived along with
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
     * Slows down Playwright operations by the specified amount of milliseconds. Useful so that you can see what is going on.
     */
    public Double slowMo;
    /**
     * If set to true, enables strict selectors mode for this context. In the strict selectors mode all operations on selectors
     * that imply single target DOM element will throw when more than one element matches the selector. This option does not
     * affect any Locator APIs (Locators are always strict). Defaults to {@code false}. See {@code Locator} to learn more about
     * the strict mode.
     */
    public Boolean strictSelectors;
    /**
     * Maximum time in milliseconds to wait for the browser instance to start. Defaults to {@code 30000} (30 seconds). Pass
     * {@code 0} to disable timeout.
     */
    public Double timeout;
    /**
     * Changes the timezone of the context. See <a
     * href="https://cs.chromium.org/chromium/src/third_party/icu/this/data/misc/metaZones.txt?rcl=faee8bc70570192d82d2978a71e2a615788597d1">ICU's
     * metaZones.txt</a> for a list of supported timezone IDs. Defaults to the system timezone.
     */
    public String timezoneId;
    /**
     * If specified, traces are saved into this directory.
     */
    public Path tracesDir;
    /**
     * Specific user agent to use in this context.
     */
    public String userAgent;
    /**
     * Path to a User Data Directory, which stores browser session data like cookies and local storage. More details for <a
     *    * href="https://chromium.googlethis.com/chromium/src/+/master/docs/user_data_dir.md#introduction">Chromium</a> and <a
     *    * href="https://developer.mozilla.org/en-US/docs/Mozilla/Command_Line_Options#User_Profile">Firefox</a>. Note that
     *    * Chromium's user data directory is the **parent** directory of the "Profile Path" seen at {@code chrome://version}. Pass
     *    * an empty string to use a temporary directory instead.
     */
    public String userDataRootDir = "/tmp";
    /**
     * Emulates consistent viewport for each page. Defaults to an 1280x720 viewport.  Use {@code null} to disable the
     * consistent viewport emulation. Learn more about <a href="https://playwright.dev/java/docs/emulation#viewport">viewport
     * emulation</a>.
     *
     * <p> <strong>NOTE:</strong> The {@code null} value opts out from the default presets, makes viewport depend on the host window size defined by the
     * operating system. It makes the execution of the tests non-deterministic.
     */
    public ViewportSize viewportSize;

    public BrowserType.LaunchPersistentContextOptions toOptions(){
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions();
        map.from(this.getAcceptDownloads()).whenNonNull().to(options::setAcceptDownloads);
        map.from(this.getArgs()).when(args -> !CollectionUtils.isEmpty(args)).to(options::setArgs);
        map.from(this.getBaseURL()).whenHasText().to(options::setBaseURL);
        map.from(this.getBypassCSP()).whenNonNull().to(options::setBypassCSP);
        map.from(this.getChannel()).whenHasText().to(options::setChannel);
        map.from(this.getChromiumSandbox()).whenNonNull().to(options::setChromiumSandbox);
        map.from(this.getColorScheme()).whenNonNull().to(options::setColorScheme);
        map.from(this.getDeviceScaleFactor()).whenNonNull().to(options::setDeviceScaleFactor);
        map.from(this.getDevtools()).whenNonNull().to(options::setDevtools);
        map.from(this.getDownloadsPath()).whenNonNull().to(options::setDownloadsPath);
        map.from(this.getEnv()).when(env -> !CollectionUtils.isEmpty(env)).to(options::setEnv);
        map.from(this.getExecutablePath()).whenNonNull().to(options::setExecutablePath);
        map.from(this.getExtraHttpHeaders()).whenNonNull().to(options::setExtraHTTPHeaders);
        map.from(this.getForcedColors()).whenNonNull().to(options::setForcedColors);
        map.from(this.getGeolocation()).whenNonNull().to(options::setGeolocation);
        map.from(this.getHandleSighup()).whenNonNull().to(options::setHandleSIGHUP);
        map.from(this.getHandleSigint()).whenNonNull().to(options::setHandleSIGINT);
        map.from(this.getHandleSigterm()).whenNonNull().to(options::setHandleSIGTERM);
        map.from(this.getHasTouch()).whenNonNull().to(options::setHasTouch);
        map.from(this.getHeadless()).whenNonNull().to(options::setHeadless);
        map.from(this.getHttpCredentials()).whenNonNull().to(options::setHttpCredentials);
        map.from(this.getIgnoreAllDefaultArgs()).whenNonNull().to(options::setIgnoreAllDefaultArgs);
        map.from(this.getIgnoreDefaultArgs()).when(ignoreDefaultArgs -> !CollectionUtils.isEmpty(ignoreDefaultArgs)).to(options::setIgnoreDefaultArgs);
        map.from(this.getIgnoreHttpsErrors()).whenNonNull().to(options::setIgnoreHTTPSErrors);
        map.from(this.getIsMobile()).whenNonNull().to(options::setIsMobile);
        map.from(this.getJavaScriptEnabled()).whenNonNull().to(options::setJavaScriptEnabled);
        map.from(this.getLocale()).whenHasText().to(options::setLocale);
        map.from(this.getOffline()).whenNonNull().to(options::setOffline);
        map.from(this.getPermissions()).when(permissions -> !CollectionUtils.isEmpty(permissions)).to(options::setPermissions);
        map.from(this.getProxy()).whenNonNull().to(options::setProxy);
        map.from(this.getRecordHarMode()).whenNonNull().to(options::setRecordHarMode);
        map.from(this.getRecordHarContent()).whenNonNull().to(options::setRecordHarContent);
        map.from(this.getRecordHarOmitContent()).whenNonNull().to(options::setRecordHarOmitContent);
        map.from(this.getRecordHarPath()).whenNonNull().to(options::setRecordHarPath);
        map.from(this.getRecordHarUrlFilter()).whenHasText().to(options::setRecordHarUrlFilter);
        map.from(this.getRecordVideoDir()).whenNonNull().to(options::setRecordVideoDir);
        map.from(this.getRecordVideoSize()).whenNonNull().to(options::setRecordVideoSize);
        map.from(this.getReducedMotion()).whenNonNull().to(options::setReducedMotion);
        map.from(this.getScreenSize()).whenNonNull().to(options::setScreenSize);
        map.from(this.getServiceWorkers()).whenNonNull().to(options::setServiceWorkers);
        map.from(this.getSlowMo()).whenNonNull().to(options::setSlowMo);
        map.from(this.getStrictSelectors()).whenNonNull().to(options::setStrictSelectors);
        map.from(this.getTimeout()).whenNonNull().to(options::setTimeout);
        map.from(this.getTimezoneId()).whenHasText().to(options::setTimezoneId);
        map.from(this.getTracesDir()).whenNonNull().to(options::setTracesDir);
        map.from(this.getUserAgent()).whenHasText().to(options::setUserAgent);
        map.from(this.getViewportSize()).whenNonNull().to(options::setViewportSize);
        return options;
    }

}
