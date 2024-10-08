package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.*;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BrowserContextPooledObjectFactory implements PooledObjectFactory<BrowserContext>, AutoCloseable {

    /**
     * Playwright管理容器
     */
    private static final Map<BrowserContext, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private static final Map<BrowserContext, File> USER_DATA_DIR_MAP = new ConcurrentHashMap<>();
    /**
     * 浏览器类型
     */
    private PlaywrightProperties.BrowserType browserType = PlaywrightProperties.BrowserType.chromium;
    /**
     * 无痕模式启动浏览器参数
     */
    private BrowserType.LaunchOptions launchOptions;
    /**
     * 创建新的浏览器上下文参数
     */
    private Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions().setScreenSize(1920, 1080);
    /**
     * 非无痕模式启动浏览器参数
     */
    private BrowserType.LaunchPersistentContextOptions launchPersistentOptions;
    private String userDataRootDir;
    public BrowserContextPooledObjectFactory(PlaywrightProperties.BrowserType browserType,
                                             BrowserType.LaunchOptions launchOptions,
                                             Browser.NewContextOptions newContextOptions) {
        if (Objects.nonNull(browserType)) {
            this.browserType = browserType;
        }
        if (Objects.nonNull(launchOptions)) {
            this.launchOptions = launchOptions;
        } else {
            this.launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
        }
        if (Objects.nonNull(newContextOptions)) {
            this.newContextOptions = newContextOptions;
        }
    }

    public BrowserContextPooledObjectFactory(PlaywrightProperties.BrowserType browserType,
                                             BrowserType.LaunchPersistentContextOptions launchPersistentOptions,
                                             String userDataRootDir) {
        if (Objects.nonNull(browserType)) {
            this.browserType = browserType;
        }
        if (Objects.nonNull(launchPersistentOptions)) {
            this.launchPersistentOptions = launchPersistentOptions;
        } else {
            this.launchPersistentOptions = new BrowserType.LaunchPersistentContextOptions().setHeadless(true);
        }
        if (StringUtils.hasText(userDataRootDir)) {
            this.userDataRootDir = userDataRootDir;
        } else {
            this.userDataRootDir = System.getProperty("java.io.tmpdir");
        }
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception if there is a problem activating {@code obj}
     */
    @Override
    public void activateObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        log.info("Activate BrowserContext Instance '{}'.", browserContext);
        if(Objects.nonNull(browserContext)){
            browserContext.clearCookies();
        }
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception if there is a problem destroying {@code obj}
     */
    @Override
    public void destroyObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        if (Objects.isNull(browserContext)) {
            return;
        }
        // Cleanup browser context
        cleanupBrowserContext(browserContext);
        log.info("Destroy BrowserContext Instance '{}'.", browserContext);
        Playwright playwright = PLAYWRIGHT_MAP.remove(browserContext);
        if (playwright != null) {
            playwright.close();
            log.info("Destroy browserContext of Playwright Instance '{}' Success.", playwright);
        }
    }

    public void cleanupBrowserContext(BrowserContext browserContext) {
        if (Objects.isNull(browserContext)) {
            return;
        }
        log.info("Cleanup BrowserContext Cookies '{}'.", browserContext);
        browserContext.clearCookies();
        log.info("Cleanup BrowserContext Permissions '{}'.", browserContext);
        browserContext.clearPermissions();
        File userDataDir = USER_DATA_DIR_MAP.remove(browserContext);
        if (Objects.nonNull(userDataDir) && userDataDir.exists()) {
            log.info("Cleanup BrowserContext user data directory '{}'.", userDataDir);
            try {
                FileUtils.deleteDirectory(userDataDir);
                log.info("Deleted user data directory: {}", userDataDir);
            } catch (IOException e) {
                log.error("Failed to delete user data directory: {}", userDataDir, e);
            }
        }
        List<Page> pages = browserContext.pages();
        if (!pages.isEmpty()) {
            for (Page page : pages) {
                if (page.isClosed()) {
                    continue;
                }
                log.info("Destroy page of BrowserContext Instance '{}'.", browserContext);
            }
        }
    }

    /**
     * 创建池中物（playwright）
     * @return a new instance that can be served by the pool
     * @throws Exception if there is a problem creating a new instance
     */
    @Override
    public PooledObject<BrowserContext> makeObject() throws Exception {
        Playwright playwright = Playwright.create();
        log.info("Create Playwright Instance '{}' Success.", playwright);
        BrowserContext browserContext = null;
        if (Objects.nonNull(launchPersistentOptions)) {
            File userDataDir = new File(userDataRootDir, String.valueOf(System.currentTimeMillis()));
            if(!userDataDir.exists()){
                userDataDir.mkdirs();
            }
            browserContext = PlaywrightUtil.getBrowserType(playwright, browserType)
                    .launchPersistentContext(userDataDir.toPath() , launchPersistentOptions);
            USER_DATA_DIR_MAP.put(browserContext, userDataDir);
            log.info("Create Persistent BrowserContext Instance '{}', browserType : {} , Success.", browserContext, browserType);
        } else {
            browserContext = PlaywrightUtil.getBrowserType(playwright, browserType)
                    .launch(launchOptions)
                    .newContext(newContextOptions);
            log.info("Create BrowserContext Instance '{}', browserType : {} , Success.", browserContext, browserType);
        }
        PLAYWRIGHT_MAP.put(browserContext, playwright);
        return new DefaultPooledObject<>(browserContext);
    }

    /**
     * 归还一个池中物（playwright）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception if there is a problem passivating {@code obj}
     */
    @Override
    public void passivateObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        log.info("Return BrowserContext Instance '{}'.", browserContext);
        if(Objects.nonNull(browserContext)){
            browserContext.clearCookies();
            browserContext.pages().forEach(page -> {
                PlaywrightUtil.closePage(page);
            });
            log.info("Return BrowserContext Instance : clear cookies success");
        }
    }

    /**
     * 检测对象是否"有效";Pool中不能保存无效的"对象",因此"后台检测线程"会周期性的检测Pool中"对象"的有效性,如果对象无效则会导致此对象从Pool中移除,并destroy;此外在调用者从Pool获取一个"对象"时,也会检测"对象"的有效性,确保不能讲"无效"的对象输出给调用者;当调用者使用完毕将"对象归还"到Pool时,仍然会检测对象的有效性.所谓有效性,就是此"对象"的状态是否符合预期,是否可以对调用者直接使用;如果对象是Socket,那么它的有效性就是socket的通道是否畅通/阻塞是否超时等.
     * 这里若要检测，需要在PoolConfig中配置检测项目。
     * true：检测正常，符合预期；false：异常，销毁对象
     * @param p a {@code PooledObject} wrapping the instance to be validated
     *
     * @return {@code false} if this object is not currently valid and should be dropped from the pool, {@code true} otherwise.
     */
    @Override
    public boolean validateObject(PooledObject<BrowserContext> p) {
        BrowserContext browserContext = p.getObject();
        boolean isValidated;
        if (Objects.nonNull(launchOptions)) {
            isValidated = Objects.nonNull(browserContext) && browserContext.browser().isConnected();
        } else {
            isValidated = Objects.nonNull(browserContext);
        }
        log.info("Validate BrowserContext : {}, isValidated : {}", browserContext, isValidated);
        return isValidated;
    }

    @Override
    public void close() throws Exception {
        PLAYWRIGHT_MAP.forEach((browserContext, playwright) -> {
            // Cleanup browser context
            cleanupBrowserContext(browserContext);
            if (playwright != null) {
                playwright.close();
                log.info("Destroy browserContext of Playwright Instance '{}' Success.", playwright);
            }
        });

    }

}
