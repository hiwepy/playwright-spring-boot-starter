package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.initializer.BrowserContextInitializer;
import com.microsoft.playwright.spring.boot.initializer.BrowserInitializer;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPooledObjectFactory;
import com.microsoft.playwright.spring.boot.pool.BrowserPool;
import com.microsoft.playwright.spring.boot.pool.BrowserPooledObjectFactory;
import com.microsoft.playwright.spring.boot.utils.JmxBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BrowserPooledObjectFactory browserPooledObjectFactory(PlaywrightProperties playwrightProperties){
        BrowserType.LaunchOptions launchOptions = playwrightProperties.getLaunchOptions().toOptions();
        return new BrowserPooledObjectFactory(playwrightProperties.getBrowserType(), launchOptions);
    }

    @Bean
    @ConditionalOnMissingBean
    public BrowserPool browserPool(PlaywrightProperties playwrightProperties, BrowserPooledObjectFactory browserPooledObjectFactory){

        // 1、创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<Browser> poolConfig = playwrightProperties.getBrowserPool().toPoolConfig();
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserPool.class));

        // 2、创建 BrowserPool 对象
        BrowserPool browserPool = new BrowserPool(browserPooledObjectFactory, poolConfig);

        // 3、创建 PlaywrightBrowserInitializer 实例
        BrowserInitializer installer = new BrowserInitializer(browserPool, playwrightProperties);

        // 4、调用 run 方法开始安装
        installer.run();

        return browserPool;
    }

    @Bean
    @ConditionalOnMissingBean
    public BrowserContextPooledObjectFactory browserContextPooledObjectFactory(PlaywrightProperties playwrightProperties){

        Browser.NewContextOptions newContextOptions = playwrightProperties.getNewContextOptions().toOptions();
        BrowserContextPooledObjectFactory factory;
        if (Objects.requireNonNull(playwrightProperties.getBrowserMode()) == PlaywrightProperties.BrowserMode.persistent) {
            BrowserType.LaunchPersistentContextOptions launchPersistentOptions = playwrightProperties.getLaunchPersistentOptions().toOptions();
            String userDataRootDir;
            if (StringUtils.hasText(playwrightProperties.getLaunchPersistentOptions().getUserDataRootDir())) {
                userDataRootDir = playwrightProperties.getLaunchPersistentOptions().getUserDataRootDir();
            } else {
                userDataRootDir = System.getProperty("java.io.tmpdir");
            }
            factory = new BrowserContextPooledObjectFactory(playwrightProperties.getBrowserType(), launchPersistentOptions, userDataRootDir);
        } else {
            BrowserType.LaunchOptions launchOptions = playwrightProperties.getLaunchOptions().toOptions();
            factory = new BrowserContextPooledObjectFactory(playwrightProperties.getBrowserType(), launchOptions, newContextOptions);
        }

        return factory;
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
