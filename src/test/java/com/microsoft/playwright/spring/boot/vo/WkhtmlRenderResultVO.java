package com.microsoft.playwright.spring.boot.vo;

import lombok.Data;

import java.io.Serializable;

/**
 *
 */
@Data
public class WkhtmlRenderResultVO implements Serializable {

    private String fileUrl;
    /**
     * PDF/Zip压缩文件路径
     */
    private String filePath;
    /**
     * PDF/Zip压缩文件名称
     */
    private String fileName;
    /**
     * PDF/Zip/Image 文件字节数组
     */
    private byte[] fileBuffer;

}
