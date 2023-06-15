package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
@ConditionalOnClass({ Playwright.class, PooledObjectFactory.class })
@EnableConfigurationProperties(PlaywrightProperties.class)
@Slf4j
public class PlaywrightAutoConfiguration {

    protected static final PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

    @Bean(name = "browserContextPool")
    @ConditionalOnMissingBean
    public BrowserContextPool browserContextPool(PlaywrightProperties playwrightProperties){

        // 创建 BrowserContextPooledObjectFactory 对象
        BrowserContextPooledObjectFactory factory = new BrowserContextPooledObjectFactory();
        // 创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<BrowserContext> poolConfig = new GenericObjectPoolConfig<>();
        this.copyProperties(playwrightProperties.getPagePool(), poolConfig);
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserContextPool.class));
        // 创建 BrowserContextPool 对象
        BrowserContextPool browserContextPool = new BrowserContextPool(factory, poolConfig);
        return browserContextPool;
    }

    @Bean(name = "browserPagePool")
    @ConditionalOnMissingBean
    public BrowserPagePool browserPagePool(PlaywrightProperties playwrightProperties, BrowserContextPool browserContextPool){

        // 创建 BrowserPagePooledObjectFactory 对象，并传入 BrowserContextPool
        BrowserPagePooledObjectFactory factory = new BrowserPagePooledObjectFactory(browserContextPool);
        // 创建 GenericObjectPoolConfig 对象，并进行必要的配置
        GenericObjectPoolConfig<Page> poolConfig = new GenericObjectPoolConfig<>();
        this.copyProperties(playwrightProperties.getPagePool(), poolConfig);
        poolConfig.setJmxEnabled(Boolean.FALSE);
        poolConfig.setJmxNameBase(JmxBeanUtils.getObjectName(BrowserPagePool.class));
        // 创建 BrowserPagePool 对象，传入 factory 和 poolConfig
        BrowserPagePool pagePool = new BrowserPagePool(factory, poolConfig);
        return pagePool;
    }

    protected void copyProperties(PlaywrightProperties.ObjectPoolConfig source, GenericObjectPoolConfig poolConfig){
        if (Objects.isNull(source) || Objects.isNull(poolConfig)){
            return;
        }
        map.from(source.isBlockWhenExhausted()).to(poolConfig::setBlockWhenExhausted);
        map.from(source.getDurationBetweenEvictionRuns()).to(poolConfig::setTimeBetweenEvictionRuns);
        map.from(source.getEvictionPolicyClassName()).to(poolConfig::setEvictionPolicyClassName);
        map.from(source.getEvictorShutdownTimeoutDuration()).to(poolConfig::setEvictorShutdownTimeout);
        map.from(source.isFairness()).to(poolConfig::setFairness);
        map.from(source.isLifo()).to(poolConfig::setLifo);
        map.from(source.getMaxWaitDuration()).to(poolConfig::setMaxWait);
        map.from(source.getMaxIdle()).to(poolConfig::setMaxIdle);
        map.from(source.getMaxTotal()).to(poolConfig::setMaxTotal);
        map.from(source.getMinEvictableIdleDuration()).to(poolConfig::setMinEvictableIdleTime);
        map.from(source.getMinIdle()).to(poolConfig::setMinIdle);
        map.from(source.getNumTestsPerEvictionRun()).to(poolConfig::setNumTestsPerEvictionRun);
        map.from(source.getSoftMinEvictableIdleDuration()).to(poolConfig::setSoftMinEvictableIdleTime);
        map.from(source.isTestOnBorrow()).to(poolConfig::setTestOnBorrow);
        map.from(source.isTestOnCreate()).to(poolConfig::setTestOnCreate);
        map.from(source.isTestOnReturn()).to(poolConfig::setTestOnReturn);
        map.from(source.isTestWhileIdle()).to(poolConfig::setTestWhileIdle);
    }

}
