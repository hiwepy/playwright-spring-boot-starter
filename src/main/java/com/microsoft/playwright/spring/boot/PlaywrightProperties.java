/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import lombok.Data;
import org.apache.commons.pool2.impl.EvictionPolicy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 
 * @author ： <a href="https://github.com/hiwepy">hiwepy</a>
 */
@ConfigurationProperties(PlaywrightProperties.PREFIX)
@Data
public class PlaywrightProperties {

	public static final String PREFIX = "playwright";

	private ObjectPoolConfig browserPool = new ObjectPoolConfig();

	private ObjectPoolConfig pagePool = new ObjectPoolConfig();

	@Data
	public static class ObjectPoolConfig {

		private boolean blockWhenExhausted = GenericObjectPoolConfig.DEFAULT_BLOCK_WHEN_EXHAUSTED;

		private Duration durationBetweenEvictionRuns = GenericObjectPoolConfig.DEFAULT_TIME_BETWEEN_EVICTION_RUNS;

		private Duration evictorShutdownTimeoutDuration = GenericObjectPoolConfig.DEFAULT_EVICTOR_SHUTDOWN_TIMEOUT;

		private String evictionPolicyClassName = GenericObjectPoolConfig.DEFAULT_EVICTION_POLICY_CLASS_NAME;

		private boolean fairness = GenericObjectPoolConfig.DEFAULT_FAIRNESS;

		private boolean lifo = GenericObjectPoolConfig.DEFAULT_LIFO;

		private Duration maxWaitDuration = GenericObjectPoolConfig.DEFAULT_MAX_WAIT;

		private int maxTotal = GenericObjectPoolConfig.DEFAULT_MAX_TOTAL;

		private int maxIdle = GenericObjectPoolConfig.DEFAULT_MAX_IDLE;

		private int minIdle = GenericObjectPoolConfig.DEFAULT_MIN_IDLE;

		private Duration minEvictableIdleDuration = GenericObjectPoolConfig. DEFAULT_MIN_EVICTABLE_IDLE_DURATION;

		private Duration softMinEvictableIdleDuration = GenericObjectPoolConfig.DEFAULT_SOFT_MIN_EVICTABLE_IDLE_DURATION;

		private int numTestsPerEvictionRun = GenericObjectPoolConfig.DEFAULT_NUM_TESTS_PER_EVICTION_RUN;

		private boolean testOnCreate = GenericObjectPoolConfig.DEFAULT_TEST_ON_CREATE;

		private boolean testOnBorrow = GenericObjectPoolConfig.DEFAULT_TEST_ON_BORROW;

		private boolean testOnReturn = GenericObjectPoolConfig.DEFAULT_TEST_ON_RETURN;

		private boolean testWhileIdle = GenericObjectPoolConfig.DEFAULT_TEST_WHILE_IDLE;

	}

}
