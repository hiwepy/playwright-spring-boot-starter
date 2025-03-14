package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.*;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.DisposableBean;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BrowserContextPooledObjectFactory implements PooledObjectFactory<BrowserContext>, DisposableBean {

    /**
     * Playwright管理容器
     */
    private static final Map<BrowserContext, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private final PlaywrightProperties playwrightProperties;

    public BrowserContextPooledObjectFactory(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
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
        try {
            // Cleanup browser context
            PlaywrightUtil.cleanupBrowserContext(browserContext);
            // 2. 关闭上下文
            browserContext.close();
            // 3. 清理 Playwright 实例（如果监听器没有处理）
            Playwright playwright = PLAYWRIGHT_MAP.remove(browserContext);
            if (playwright != null) {
                playwright.close();
                log.info("Cleaned up Playwright instance in destroyObject");
            } 
        } catch (Exception e) {
            log.error("Error destroying browser context", e);
            throw e;
        }
    }



    /**
     * 创建池中物（playwright）
     * @return a new instance that can be served by the pool
     */
    @Override
    public PooledObject<BrowserContext> makeObject() {
        log.info("Create Playwright Instance .");
        Playwright playwright = Playwright.create();
        log.info("Create Playwright Instance '{}' Success.", playwright);
        // Browser Type
        PlaywrightProperties.BrowserTypeEnum browserTypeEnum = Objects.nonNull(playwrightProperties.getBrowserType()) ? playwrightProperties.getBrowserType() : PlaywrightProperties.BrowserTypeEnum.chromium;
        // Get Browser Launch Options
        BrowserType.LaunchOptions launchOptions = Objects.nonNull(playwrightProperties.getLaunchOptions()) ? playwrightProperties.getLaunchOptions().toOptions() : new BrowserType.LaunchOptions().setHeadless(true);
        // Get Browser
        log.info("Create Browser Instance .");
        BrowserType browserType = browserTypeEnum.getBrowserType(playwright);
        Browser browser = browserType.launch(launchOptions);
        // 添加断开连接监听器
        browser.onDisconnected((b) -> {
            log.info("Browser disconnected, cleaning up resources...");
            b.contexts().forEach(context -> {
                try {
                    // 获取相关的 BrowserContext
                    if (Objects.nonNull(context)) {
                        // 清理浏览器上下文
                        PlaywrightUtil.cleanupBrowserContext(context);
                        context.close();
                        // 从 Map 中移除并关闭 Playwright
                        Playwright pw = PLAYWRIGHT_MAP.remove(context);
                        if (Objects.nonNull(pw)) {
                            pw.close();
                            log.info("Cleaned up Playwright instance on browser disconnect");
                        }
                    }
                } catch (Exception e) {
                    log.error("Error cleaning up resources on browser disconnect", e);
                }
            });
        });
        log.info("Create Browser Instance {} Success.", browser);
        // Get Browser New Context Options
        Browser.NewContextOptions newContextOptions = Objects.nonNull(playwrightProperties.getNewContextOptions()) ? playwrightProperties.getNewContextOptions().toOptions() : new Browser.NewContextOptions().setScreenSize(1920, 1080);
        // Create Browser Context
        BrowserContext browserContext = browser.newContext(newContextOptions);
        log.info("Create BrowserContext Instance '{}', browserType : {} , Success.", browserContext, browserType);
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
            browserContext.pages().forEach(PlaywrightUtil::closePage);
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
        log.info("Validate BrowserContext Instance '{}'.", browserContext);
        boolean isValidated = Objects.nonNull(browserContext) && browserContext.browser().isConnected();
        log.info("Validate BrowserContext : {}, isValidated : {}", browserContext, isValidated);
        return isValidated;
    }

    @Override
    public void destroy() throws Exception {
        PLAYWRIGHT_MAP.forEach((browserContext, playwright) -> {
            // Cleanup browser context
            PlaywrightUtil.cleanupBrowserContext(browserContext);
            browserContext.close();
            if (playwright != null) {
                playwright.close();
                log.info("Destroy browserContext of Playwright Instance '{}' Success.", playwright);
            }
        });

    }

}
