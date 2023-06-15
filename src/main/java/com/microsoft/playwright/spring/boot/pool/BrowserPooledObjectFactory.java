package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserPooledObjectFactory implements PooledObjectFactory<Browser>, AutoCloseable {

    /**
     * Playwright管理容器
     */
    private static final Map<Browser, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private BrowserType.ConnectOptions connectOptions = new BrowserType.ConnectOptions();
    private BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions().setHeadless(false);

    public BrowserPooledObjectFactory() {
    }
    public BrowserPooledObjectFactory(BrowserType.LaunchOptions launchOptions) {
        this(launchOptions, null);
    }
    public BrowserPooledObjectFactory(BrowserType.LaunchOptions launchOptions, BrowserType.ConnectOptions connectOptions) {
        if (Objects.nonNull(connectOptions)) {
            this.connectOptions = connectOptions;
        }
        if (Objects.nonNull(launchOptions)) {
            this.launchOptions = launchOptions;
        }
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception
     */
    @Override
    public void activateObject(PooledObject<Browser> p) throws Exception {
        // 激活对象时的逻辑，这里不需要执行任何操作，留空即可
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception
     */
    @Override
    public void destroyObject(PooledObject<Browser> p) throws Exception {
        Browser browser = p.getObject();
        Playwright playwright = PLAYWRIGHT_MAP.remove(browser);
        browser.close();
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
    public PooledObject<Browser> makeObject() throws Exception {
        Playwright playwright = Playwright.create();
        // 创建一个新的浏览器
        Browser browser = playwright.chromium()
                .launch(launchOptions);
        // 创建一个默认的页面
        browser.newPage();
        PLAYWRIGHT_MAP.put(browser, playwright);
        return new DefaultPooledObject<>(browser);
    }

    /**
     * 归还一个池中物（playwright）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception
     */
    @Override
    public void passivateObject(PooledObject<Browser> p) throws Exception {
        // 钝化对象时的逻辑，这里不需要执行任何操作，留空即可
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
    public boolean validateObject(PooledObject<Browser> p) {
        Browser browser = p.getObject();
        return Objects.nonNull(browser) && browser.isConnected();
    }

    @Override
    public void close() throws Exception {
        PLAYWRIGHT_MAP.forEach((browser, playwright) -> {
            browser.close();
            playwright.close();
        });
    }

}
