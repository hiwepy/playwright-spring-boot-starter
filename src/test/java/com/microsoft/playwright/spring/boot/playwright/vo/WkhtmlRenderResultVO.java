package com.microsoft.playwright.spring.boot.playwright.vo;

import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * PDF 渲染结果
 */
@Data
public class WkhtmlRenderResultVO implements Serializable {

    /**
     * 截图列表，包含截图结果和截图失败原因
     */
    private List<PageScreenshotTemp> screenshots;
    /**
     * PDF/Zip压缩文件下载地址
     */
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
