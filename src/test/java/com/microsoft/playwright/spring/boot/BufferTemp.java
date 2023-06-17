package com.microsoft.playwright.spring.boot;


import lombok.Builder;
import lombok.Data;

/**
 *
 */
@Data
@Builder
public class BufferTemp {

    /**
     * 原始序号
     */
    private int index;
    /**
     * 原始请求URL
     */
    private String url;
    /**
     * 文件名称
     */
    private String name;
    /**
     * 是否保存到文件
     */
    private Boolean toFile;
    /**
     * 文件存储路径
     */
    private String path;
    /**
     * 截图缓存
     */
    private byte[] buffer;

    public static BufferTemp.BufferTempBuilder from(BufferTemp temp) {
        return BufferTemp.builder()
                .index(temp.getIndex())
                .url(temp.getUrl())
                .buffer(temp.getBuffer())
                .name(temp.getName());
    }

}

