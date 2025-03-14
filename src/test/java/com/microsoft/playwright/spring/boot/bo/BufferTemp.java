package com.microsoft.playwright.spring.boot.bo;

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
     * 文件大小
     */
    private Long fileSize;
    /**
     * 截图缓存
     */
    private byte[] buffer;

    /**
     * 是否需要重新加载
     */
    private Boolean needReload;
    /**
     * 当前是否是在重新加载
     */
    private Boolean reload;
    /**
     * 重新超时时间（初次使用全局配置，重试会计算新的超时时间）
     */
    private Double reloadTimeout;

   public static BufferTemp.BufferTempBuilder from(BufferTemp temp) {
       return BufferTemp.builder()
               .index(temp.getIndex())
               .url(temp.getUrl())
               .buffer(temp.getBuffer())
               .name(temp.getName());
   }

    public boolean isReload() {
        return reload != null && reload;
    }

    public boolean isNeedReload() {
        return needReload != null && needReload;
    }

}

