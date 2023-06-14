package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPooledObjectFactory;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePooledObjectFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    @Bean
    public BrowserContextPool browserContextPool(PlaywrightProperties playwrightProperties){

        // 创建 BrowserContextPooledObjectFactory 对象
        BrowserContextPooledObjectFactory factory = new BrowserContextPooledObjectFactory();
        // 创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<BrowserContext> poolConfig = new GenericObjectPoolConfig<>();

        // 创建 BrowserContextPool 对象
        BrowserContextPool browserContextPool = new BrowserContextPool(factory, poolConfig);
        return browserContextPool;
    }

    @Bean
    public BrowserPagePool browserPagePool(PlaywrightProperties playwrightProperties, BrowserContextPool browserContextPool){


        // 创建 BrowserPagePooledObjectFactory 对象，并传入 BrowserContextPool
        BrowserPagePooledObjectFactory factory = new BrowserPagePooledObjectFactory(browserContextPool);
        // 创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<Page> poolConfig = new GenericObjectPoolConfig<>();

        // 创建 BrowserPagePool 对象，传入 factory 和 poolConfig
        BrowserPagePool pagePool = new BrowserPagePool(factory, poolConfig);
        return pagePool;
    }



}
