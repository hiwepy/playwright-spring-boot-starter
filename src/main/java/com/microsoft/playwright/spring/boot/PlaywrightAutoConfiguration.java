package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPooledObjectFactory;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePooledObjectFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    @Bean(destroyMethod = "close")
    public BrowserContextPooledObjectFactory browserContextPooledObjectFactory(PlaywrightProperties playwrightProperties){

        BrowserContextPooledObjectFactory browserContextPooledObjectFactory = new BrowserContextPooledObjectFactory();

        return browserContextPooledObjectFactory;
    }

    @Bean
    public BrowserContextPool browserContextPool(PlaywrightProperties playwrightProperties){

        BrowserContextPooledObjectFactory browserContextPooledObjectFactory = new BrowserContextPooledObjectFactory();

        BrowserContextPool browserContextPool = new BrowserContextPool(browserContextPooledObjectFactory, playwrightProperties.getBrowserPoolConfig());
        return browserContextPool;
    }

    @Bean(destroyMethod = "close")
    public BrowserPagePooledObjectFactory browserPagePooledObjectFactory(BrowserContextPool browserContextPool){

        BrowserPagePooledObjectFactory browserPagePooledObjectFactory = new BrowserPagePooledObjectFactory(browserContextPool);

        return browserPagePooledObjectFactory;
    }

    @Bean
    public BrowserPagePool browserPagePool(PlaywrightProperties playwrightProperties, BrowserPagePooledObjectFactory browserPagePooledObjectFactory){
        BrowserPagePool browserContextPool = new BrowserPagePool(browserPagePooledObjectFactory, playwrightProperties.getPagePoolConfig());
        return browserContextPool;
    }



}
