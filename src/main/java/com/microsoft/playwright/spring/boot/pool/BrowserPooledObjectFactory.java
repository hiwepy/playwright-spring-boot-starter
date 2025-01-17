package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.*;
import com.microsoft.playwright.spring.boot.utils.PlaywrightManager;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.List;
import java.util.Objects;

@Slf4j
public class BrowserPooledObjectFactory implements PooledObjectFactory<Browser>, AutoCloseable {

    /**
     * 浏览器类型
     */
    private PlaywrightProperties.BrowserType browserType = PlaywrightProperties.BrowserType.chromium;
    /**
     * 无痕模式启动浏览器参数
     */
    private final BrowserType.LaunchOptions launchOptions;

    public BrowserPooledObjectFactory(PlaywrightProperties.BrowserType browserType,
                                      BrowserType.LaunchOptions launchOptions) {
        if (Objects.nonNull(browserType)) {
            this.browserType = browserType;
        }
        if (Objects.nonNull(launchOptions)) {
            this.launchOptions = launchOptions;
        } else {
            this.launchOptions = new BrowserType.LaunchOptions().setHeadless(true);
        }
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception if there is a problem activating {@code obj}
     */
    @Override
    public void activateObject(PooledObject<Browser> p) throws Exception {
        Browser browser = p.getObject();
        log.info("Activate Browser Instance '{}'.", browser);
        if(Objects.nonNull(browser)){
            browser.contexts().forEach(BrowserContext::clearCookies);
        }
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception if there is a problem destroying {@code obj}
     */
    @Override
    public void destroyObject(PooledObject<Browser> p) throws Exception {
        Browser browser = p.getObject();
        if (Objects.isNull(browser)) {
            log.warn("Destroy Browser Instance Error, Browser is null.");
            return;
        }
        try {
            log.info("Destroy Browser Instance '{}'.", browser);
            cleanupBrowser(browser);
            browser.close();
        } catch (Exception e) {
            log.error("Destroy Browser Instance '{}' Error.", browser, e);
        }
    }

    public void cleanupBrowser(Browser browser) {
        if (Objects.isNull(browser)) {
            return;
        }
        browser.contexts().forEach(context -> {
            List<Page> pages = context.pages();
            if (Objects.nonNull(pages) && !pages.isEmpty()) {
                pages.forEach(PlaywrightUtil::closePage);
            }
            context.clearCookies();
        });
    }

    /**
     * 创建池中物（Browser）
     * @return a new instance that can be served by the pool
     * @throws Exception if there is a problem creating a new instance
     */
    @Override
    public PooledObject<Browser> makeObject() throws Exception {
        // Get playwright instance
        Playwright playwright = PlaywrightManager.getInstance();
        // create browser instance with playwright
        Browser browser = PlaywrightUtil.getBrowserType(playwright, browserType)
                    .launch(launchOptions);
        log.info("Create Browser Instance '{}', browserType : {} , Success.", browser, browserType);
        return new DefaultPooledObject<>(browser);
    }

    /**
     * 归还一个池中物（Browser）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception if there is a problem passivating {@code obj}
     */
    @Override
    public void passivateObject(PooledObject<Browser> p) throws Exception {
        Browser browser = p.getObject();
        log.info("Return Browser Instance '{}'.", browser);
        if(Objects.nonNull(browser)){
            cleanupBrowser(browser);
            log.info("Return Browser Instance : clear cookies success");
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
    public boolean validateObject(PooledObject<Browser> p) {
        Browser browser = p.getObject();
        boolean isValidated = Objects.nonNull(browser) && browser.isConnected();
        log.info("Validate Browser : {}, isValidated : {}", browser, isValidated);
        return isValidated;
    }

    @Override
    public void close() throws Exception {
        // Cleanup browser
        PlaywrightManager.getInstance().close();
        log.info("Destroy Browser of Playwright Instance Success.");
    }

}
