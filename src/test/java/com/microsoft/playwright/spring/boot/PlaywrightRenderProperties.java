/**
 * Copyright (C) 2022 杭州天音计算机系统工程有限公司
 * All Rights Reserved.
 */
package com.microsoft.playwright.spring.boot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(PlaywrightRenderProperties.PREFIX)
@Data
public class PlaywrightRenderProperties {

	public static final String PREFIX = "playwright.render";

	/**
	 * 浏览器会话是否隔离
	 */
	protected boolean isolated;

	/**
	 * 临时是否写入磁盘
	 */
	protected boolean writeToFile = true;

	/**
	 * 临时目录
	 */
	protected String tmpDir = "/tmp";

	/**
	 * 截图地址追加公共前缀
	 */
	protected String urlPrefix;

	/**
	 * 是否加载等待
	 */
	private boolean loadWait;
	/**
	 * 加载等待时间
	 */
	protected Duration loadWaitDuration = Duration.ofSeconds(3);

	/**
	 * 是否运行重新加载
	 */
	private boolean reloadAble;

	/**
	 * 重试加载页面次数
	 */
	protected Integer reloadLimit = 3;

	/**
	 * 是否加加载等待
	 */
	private boolean reloadWait;

	/**
	 * 重加载等待时间
	 */
	protected Duration reloadWaitDuration = Duration.ofSeconds(3);

	/**
	 * 截图大小，判定截图失败(kb)
	 */
	protected Long lowerLimit = 100L;

}
