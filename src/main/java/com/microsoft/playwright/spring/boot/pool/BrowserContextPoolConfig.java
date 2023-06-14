package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.BrowserContext;
import lombok.Data;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(BrowserContextPoolConfig.PREFIX)
@Data
public class BrowserContextPoolConfig extends GenericObjectPoolConfig<BrowserContext> {

    public static final String PREFIX = "playwright.browser-context-pool";

}
