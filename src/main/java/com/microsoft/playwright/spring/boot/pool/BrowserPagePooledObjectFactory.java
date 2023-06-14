package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.Objects;

public class BrowserPagePooledObjectFactory implements PooledObjectFactory<Page> {

    private final BrowserContextPool browserContextPool;

    public BrowserPagePooledObjectFactory(BrowserContextPool browserContextPool) {
        this.browserContextPool = browserContextPool;
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception
     */
    @Override
    public void activateObject(PooledObject<Page> p) throws Exception {
        // 激活对象时的逻辑，这里不需要执行任何操作，留空即可
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception
     */
    @Override
    public void destroyObject(PooledObject<Page> p) throws Exception {
        // 销毁对象时的逻辑，关闭 Page 对象
        p.getObject().close();
    }

    /**
     * 创建池中物（playwright）
     * @return
     * @throws Exception
     */
    @Override
    public PooledObject<Page> makeObject() throws Exception {
        // 创建对象时的逻辑，借用 BrowserContextPool 中的 BrowserContext 对象创建 Page 对象
        BrowserContext browserContext = browserContextPool.borrowObject();
        Page page = browserContext.newPage();
        return new DefaultPooledObject<>(page);
    }

    /**
     * 归还一个池中物（playwright）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception
     */
    @Override
    public void passivateObject(PooledObject<Page> p) throws Exception {
        // 归还对象时的逻辑，执行一些清理操作
        p.getObject().evaluate("try {window.localStorage.clear()} catch(e){console.log(e)}");
        p.getObject().navigate("about:blank");
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
    public boolean validateObject(PooledObject<Page> p) {
        // 验证对象的有效性，检查对象是否为空且未关闭
        return Objects.nonNull(p.getObject()) && !p.getObject().isClosed();
    }

}
