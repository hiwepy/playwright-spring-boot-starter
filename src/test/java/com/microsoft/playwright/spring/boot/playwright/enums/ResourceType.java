package com.microsoft.playwright.spring.boot.playwright.enums;

/**
 * 渲染引擎所感知的请求的资源类型。ResourceType将是以下之一：
 * document, stylesheet, image, media, font, script, texttrack, xhr, fetch, eventsource, websocket, manifest, other.
 */
public enum ResourceType {

    document, stylesheet, image, media, font, script, texttrack, xhr, fetch, eventsource, websocket, manifest, other;

    public static ResourceType getByName(String name) {
        for (ResourceType value : ResourceType.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }
        return null;
    }

    public boolean isNeedRetry() {
        return this == ResourceType.stylesheet || this == ResourceType.image || this == ResourceType.script || this == ResourceType.fetch || this == ResourceType.xhr;
    }

}
