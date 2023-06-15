package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.*;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserContextPooledObjectFactory implements PooledObjectFactory<BrowserContext>, AutoCloseable {

    /**
     * Playwright管理容器
     */
    private static final Map<BrowserContext, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private PlaywrightProperties.BrowserType browserType = PlaywrightProperties.BrowserType.CHROMIUM;
    private BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);
    private Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions().setScreenSize(1920, 1080);

    public BrowserContextPooledObjectFactory(PlaywrightProperties.BrowserType browserType) {
        this(browserType, null, null);
    }

    public BrowserContextPooledObjectFactory(PlaywrightProperties.BrowserType browserType,
                                             BrowserType.LaunchOptions launchOptions) {
        this(browserType, launchOptions, null);
    }

    public BrowserContextPooledObjectFactory(PlaywrightProperties.BrowserType browserType,
                                             BrowserType.LaunchOptions launchOptions,
                                             Browser.NewContextOptions newContextOptions) {
        if (Objects.nonNull(browserType)) {
            this.browserType = browserType;
        }
        if (Objects.nonNull(launchOptions)) {
            this.launchOptions = launchOptions;
        }
        if (Objects.nonNull(newContextOptions)) {
            this.newContextOptions = newContextOptions;
        }
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception
     */
    @Override
    public void activateObject(PooledObject<BrowserContext> p) throws Exception {
        p.getObject().clearCookies();
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception
     */
    @Override
    public void destroyObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        Playwright playwright = PLAYWRIGHT_MAP.remove(browserContext);
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * 创建池中物（playwright）
     * @return
     * @throws Exception
     */
    @Override
    public PooledObject<BrowserContext> makeObject() throws Exception {
        Playwright playwright = Playwright.create();
        // 创建一个新的浏览器上下文
        BrowserContext browserContext;
        switch (browserType) {
            case CHROMIUM:
                browserContext = playwright.chromium().launch(launchOptions).newContext(newContextOptions);
                break;
            case FIREFOX:
                browserContext = playwright.firefox().launch(launchOptions).newContext(newContextOptions);
                break;
            case WEBKIT:
                browserContext = playwright.webkit().launch(launchOptions).newContext(newContextOptions);
                break;
            default:
                throw new IllegalArgumentException("browserType is not supported");
        }
        PLAYWRIGHT_MAP.put(browserContext, playwright);
        // 创建一个默认的页面
        Page page = browserContext.newPage();
        page.navigate("about:blank");
        return new DefaultPooledObject<>(browserContext);
    }

    /**
     * 归还一个池中物（playwright）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception
     */
    @Override
    public void passivateObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        browserContext.pages().get(0).evaluate("try {window.localStorage.clear()} catch(e){console.log(e)}");
        browserContext.clearCookies();
        browserContext.pages().get(0).navigate("about:blank");
    }

    /**
     * 检测对象是否"有效";Pool中不能保存无效的"对象",因此"后台检测线程"会周期性的检测Pool中"对象"的有效性,如果对象无效则会导致此对象从Pool中移除,并destroy;此外在调用者从Pool获取一个"对象"时,也会检测"对象"的有效性,确保不能讲"无效"的对象输出给调用者;当调用者使用完毕将"对象归还"到Pool时,仍然会检测对象的有效性.所谓有效性,就是此"对象"的状态是否符合预期,是否可以对调用者直接使用;如果对象是Socket,那么它的有效性就是socket的通道是否畅通/阻塞是否超时等.
     * 这里若要检测，需要在PoolConfig中配置检测项目。
     * true：检测正常，符合预期；false：异常，销毁对象
     * @param p a {@code PooledObject} wrapping the instance to be validated
     *
     * @return
     */
    @Override
    public boolean validateObject(PooledObject<BrowserContext> p) {
        BrowserContext browserContext = p.getObject();
        if(Objects.isNull(browserContext)){
            return Boolean.FALSE;
        }
        return browserContext.browser().isConnected();
    }

    @Override
    public void close() throws Exception {
        PLAYWRIGHT_MAP.forEach((browserContext, playwright) -> {
            browserContext.close();
            playwright.close();
        });
    }

}
