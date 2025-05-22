package com.microsoft.playwright.spring.boot.playwright.redis;

import org.springframework.data.redis.core.RedisKey;

import java.util.function.BiFunction;

public enum BizRedisKey {

    /**
     * 报告单渲染状态
     */
    RENDER_STATE("报告单渲染状态", (taskId, p2) -> {
        return RedisKey.getKeyStr(BizRedisKeyConstant.RENDER_STATE_KEY, taskId);
    }),

    ;

    private String desc;
    private BiFunction<Object, Object, String> function;

    BizRedisKey(String desc, BiFunction<Object, Object, String> function) {
        this.desc = desc;
        this.function = function;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 1、获取全名称key
     *
     * @return
     */
    public String getKey() {
        return this.function.apply(null, null);
    }

    /**
     * 1、获取全名称key
     *
     * @param key
     * @return
     */
    public String getKey(Object key) {
        return this.function.apply(key, null);
    }

    /**
     * 1、获取全名称key
     *
     * @param key1
     * @param key2
     * @return
     */
    public String getKey(Object key1, Object key2) {
        return this.function.apply(key1, key2);
    }

}
