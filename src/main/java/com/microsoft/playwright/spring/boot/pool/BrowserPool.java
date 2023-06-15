package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Browser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

@Slf4j
public class BrowserPool extends GenericObjectPool<Browser> {

    public BrowserPool(PooledObjectFactory<Browser> factory) {
        super(factory);
    }

    public BrowserPool(PooledObjectFactory<Browser> factory, GenericObjectPoolConfig<Browser> config) {
        super(factory, config);
    }

    public BrowserPool(PooledObjectFactory<Browser> factory, GenericObjectPoolConfig<Browser> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
