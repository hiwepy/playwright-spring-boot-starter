package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.hooks.PlaywrightHook;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPool;
import com.microsoft.playwright.spring.boot.pool.BrowserContextPooledObjectFactory;
import com.microsoft.playwright.spring.boot.utils.JmxBeanUtils;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    @Bean(name = "browserContextPool")
    @ConditionalOnMissingBean
    public BrowserContextPool browserContextPool(PlaywrightProperties playwrightProperties){

        // 1、创建 BrowserContextPooledObjectFactory 对象

        Browser.NewContextOptions newContextOptions = new Browser.NewContextOptions();
        PlaywrightUtil.copyProperties(playwrightProperties.getNewContextOptions(), newContextOptions);

        BrowserContextPooledObjectFactory factory;
        switch (playwrightProperties.getBrowserMode()){
            case persistent: {

                BrowserType.LaunchPersistentContextOptions launchPersistentOptions = new BrowserType.LaunchPersistentContextOptions();
                PlaywrightUtil.copyProperties(playwrightProperties.getLaunchPersistentOptions(), launchPersistentOptions);
                String userDataRootDir;
                if(StringUtils.hasText(playwrightProperties.getLaunchPersistentOptions().getUserDataRootDir())){
                    userDataRootDir = playwrightProperties.getLaunchPersistentOptions().getUserDataRootDir();
                } else {
                    userDataRootDir = System.getProperty("java.io.tmpdir");
                }
                factory = new BrowserContextPooledObjectFactory(playwrightProperties.getBrowserType(), launchPersistentOptions, userDataRootDir);
                Runtime.getRuntime().addShutdownHook(new PlaywrightHook(factory, 0));
            };break;
            default: {

                BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions();
                PlaywrightUtil.copyProperties(playwrightProperties.getLaunchOptions(), launchOptions);

                factory = new BrowserContextPooledObjectFactory(playwrightProperties.getBrowserType(), launchOptions, newContextOptions);

            };break;
        }

        // 2、创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<BrowserContext> poolConfig = new GenericObjectPoolConfig<>();
        PlaywrightUtil.copyProperties(playwrightProperties.getBrowserPool(), poolConfig);
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserContextPool.class));

        // 3、创建 BrowserContextPool 对象
        BrowserContextPool browserContextPool = new BrowserContextPool(factory, poolConfig);
        return browserContextPool;
    }


}
