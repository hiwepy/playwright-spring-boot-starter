package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class BrowserPagePooledObjectFactory implements PooledObjectFactory<Page>, DisposableBean {

    private static final Map<Browser, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private final PlaywrightProperties playwrightProperties;

    public BrowserPagePooledObjectFactory(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception if there is a problem activating {@code obj}
     */
    @Override
    public void activateObject(PooledObject<Page> p) throws Exception {
        Page page = p.getObject();
        if (Objects.isNull(page)) {
            log.warn("Activate Browser Page Instance Error, Page is null.");
            return;
        }
        if (page.isClosed()) {
            log.warn("Activate Browser Page Instance Error, Page is already closed.");
            return;
        }
        // 设置默认页面为 about:blank
        page.navigate("about:blank");
        log.info("Activate Browser Page Instance '{}'.", page);
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception if there is a problem destroying {@code obj}
     */
    @Override
    public void destroyObject(PooledObject<Page> p) throws Exception {
        Page page = p.getObject();
        if (Objects.isNull(page)) {
            log.warn("Destroy Browser Page Instance Error, Page is null.");
            return;
        }
        if (page.isClosed()) {
            log.warn("Destroy Browser Page Instance Error, Page is already closed.");
            return;
        }
        try {
            log.info("Destroy Browser Page Instance '{}'.", page);
            page.close();
        } catch (Exception e) {
            log.error("Destroy Browser Page Instance '{}' Error.", page, e);
        }
    }

    /**
     * 创建池中物（Browser）
     * @return a new instance that can be served by the pool
     * @throws Exception if there is a problem creating a new instance
     */
    @Override
    public PooledObject<Page> makeObject() throws Exception {
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
        browser.onDisconnected((b) -> {
            log.error("Browser disconnected: {}", b);
            Playwright playwright2 = PLAYWRIGHT_MAP.remove(b);
            if (Objects.nonNull(playwright2)) {
                playwright2.close();
                log.info("Destroy browserContext of Playwright Instance '{}' Success.", playwright2);
            }
        });
        log.info("Create Browser Instance {} Success.", browser);
        // 在新的浏览器上下文中创建新页面。关闭此页面也将关闭上下文。
        Page page;
        if(Objects.nonNull(playwrightProperties.getNewPageOptions())){
            Browser.NewPageOptions newPageOptions = playwrightProperties.getNewPageOptions().toOptions();
            page = browser.newPage(newPageOptions);
        } else {
            page = browser.newPage();
        }
        log.info("Create Browser Page Instance '{}', Success.", page);
        PLAYWRIGHT_MAP.put(browser, playwright);
        return new DefaultPooledObject<>(page);
    }

    /**
     * 归还一个池中物（Browser）时调用，不应该 activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception if there is a problem passivating {@code obj}
     */
    @Override
    public void passivateObject(PooledObject<Page> p) throws Exception {
        Page page = p.getObject();
        log.info("Return Browser Page Instance '{}'.", page);
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
    public boolean validateObject(PooledObject<Page> p) {
        Page page = p.getObject();
        if(Objects.nonNull(page) ){
            log.info("Validate Browser Page Instance '{}'.", page);
            boolean isPageValidated = !page.isClosed() && page.context().browser().isConnected();
            log.info("Validate Browser Page : {}, isValidated : {}", page, isPageValidated);
            return isPageValidated;
        }
        return true;
    }

    @Override
    public void destroy() throws Exception {
        log.info("Destroy Browser Page Instance Success.");
    }

}
