package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Page;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public class BrowserPagePool extends GenericObjectPool<Page> {

    public BrowserPagePool(PooledObjectFactory<Page> factory) {
        super(factory);
    }

    public BrowserPagePool(PooledObjectFactory<Page> factory, GenericObjectPoolConfig<Page> config) {
        super(factory, config);
    }

    public BrowserPagePool(PooledObjectFactory<Page> factory, GenericObjectPoolConfig<Page> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
