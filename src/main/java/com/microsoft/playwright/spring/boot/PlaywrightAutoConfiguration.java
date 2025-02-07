package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.initializer.BrowserContextInitializer;
import com.microsoft.playwright.spring.boot.initializer.BrowserPageInitializer;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPooledObjectFactory;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePooledObjectFactory;
import com.microsoft.playwright.spring.boot.utils.JmxBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BrowserPagePooledObjectFactory browserPooledObjectFactory(PlaywrightProperties playwrightProperties){
        return new BrowserPagePooledObjectFactory(playwrightProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BrowserPagePool browserPool(PlaywrightProperties playwrightProperties, BrowserPagePooledObjectFactory browserPagePooledObjectFactory){

        // 1、创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<Page> poolConfig = playwrightProperties.getBrowserPagePool().toPoolConfig();
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserPagePool.class));

        // 2、创建 BrowserPool 对象
        BrowserPagePool browserPagePool = new BrowserPagePool(browserPagePooledObjectFactory, poolConfig);

        // 3、创建 PlaywrightBrowserInitializer 实例
        BrowserPageInitializer installer = new BrowserPageInitializer(browserPagePool, playwrightProperties);

        // 4、调用 run 方法开始安装
        installer.run();

        return browserPagePool;
    }

    @Bean
    @ConditionalOnMissingBean
    public BrowserContextPooledObjectFactory browserContextPooledObjectFactory(PlaywrightProperties playwrightProperties){
        return new BrowserContextPooledObjectFactory(playwrightProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public BrowserContextPool browserContextPool(PlaywrightProperties playwrightProperties, BrowserContextPooledObjectFactory browserContextPooledObjectFactory){

        // 1、创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<BrowserContext> poolConfig = playwrightProperties.getBrowserContextPool().toPoolConfig();
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserContextPool.class));

        // 2、创建 BrowserContextPool 对象
        BrowserContextPool browserContextPool = new BrowserContextPool(browserContextPooledObjectFactory, poolConfig);

        // 3、创建 PlaywrightInstall 实例
        BrowserContextInitializer installer = new BrowserContextInitializer(browserContextPool, playwrightProperties);

        // 4、调用 run 方法开始安装
        installer.run();

        return browserContextPool;
    }


}
