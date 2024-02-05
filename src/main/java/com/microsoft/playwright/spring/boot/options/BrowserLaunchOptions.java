package com.microsoft.playwright.spring.boot.options;

import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.options.Proxy;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.CollectionUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Accessors(chain = true)
@Data
public class BrowserLaunchOptions {

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

    public BrowserType.LaunchOptions toOptions() {
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        map.from(this.getArgs()).when(args -> !CollectionUtils.isEmpty(args)).to(options::setArgs);
        map.from(this.getChannel()).whenHasText().to(options::setChannel);
        map.from(this.getChromiumSandbox()).whenNonNull().to(options::setChromiumSandbox);
        map.from(this.getDevtools()).whenNonNull().to(options::setDevtools);
        map.from(this.getDownloadsPath()).whenNonNull().to(options::setDownloadsPath);
        map.from(this.getEnv()).when(env -> !CollectionUtils.isEmpty(env)).to(options::setEnv);
        map.from(this.getExecutablePath()).whenNonNull().to(options::setExecutablePath);
        map.from(this.getFirefoxUserPrefs()).when(firefoxUserPrefs -> !CollectionUtils.isEmpty(firefoxUserPrefs)).to(options::setFirefoxUserPrefs);
        map.from(this.getHandleSighup()).whenNonNull().to(options::setHandleSIGHUP);
        map.from(this.getHandleSigint()).whenNonNull().to(options::setHandleSIGINT);
        map.from(this.getHandleSigterm()).whenNonNull().to(options::setHandleSIGTERM);
        map.from(this.getHeadless()).whenNonNull().to(options::setHeadless);
        map.from(this.getIgnoreAllDefaultArgs()).whenNonNull().to(options::setIgnoreAllDefaultArgs);
        map.from(this.getIgnoreDefaultArgs()).when(ignoreDefaultArgs -> !CollectionUtils.isEmpty(ignoreDefaultArgs)).to(options::setIgnoreDefaultArgs);
        map.from(this.getProxy()).whenNonNull().to(options::setProxy);
        map.from(this.getSlowMo()).whenNonNull().to(options::setSlowMo);
        map.from(this.getTimeout()).whenNonNull().to(options::setTimeout);
        map.from(this.getTracesDir()).whenNonNull().to(options::setTracesDir);
        return options;
    }
}
