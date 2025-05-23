package com.microsoft.playwright.spring.boot;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(PlaywrightRenderProperties.PREFIX)
@Data
public class PlaywrightRenderProperties {

	public static final String PREFIX = "playwright.page-render-options";

	/**
	 * 浏览器会话是否隔离
	 */
	protected boolean isolated;

	/**
	 * 页面加载完成状态标记元素的选择器（前后端协商后确定的页面就绪的判断标识）
	 */
	private String waitForSelector;

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

	/**
	 * 缓存配置
	 */
	protected RenderCache cache = new RenderCache();

	/**
	 * 远程 PDF 配置
	 */
	protected RemotePdf pdf = new RemotePdf();

	@Data
	public static class RenderCache {

		protected Duration expireAfterWrite = Duration.ofSeconds(3);
		protected int initialCapacity = 3;
		protected int maximumSize = 10;

	}
	/**
	 * 远程 PDF 配置
	 */
	@Data
	public static class RemotePdf {

		/**
		 * 连接超时时间（毫秒）
		 */
		protected Duration connectTimeout = Duration.ofSeconds(3);

		/**
		 * 读取超时时间（毫秒）
		 */
		protected Duration readTimeout = Duration.ofSeconds(10);

	}


}
