package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.BrowserContext;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class BrowserContextPool extends GenericObjectPool<BrowserContext> {

    public BrowserContextPool(PooledObjectFactory<BrowserContext> factory) {
        super(factory);
    }

    public BrowserContextPool(PooledObjectFactory<BrowserContext> factory, GenericObjectPoolConfig<BrowserContext> config) {
        super(factory, config);
    }

    public BrowserContextPool(PooledObjectFactory<BrowserContext> factory, GenericObjectPoolConfig<BrowserContext> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
