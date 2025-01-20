package com.microsoft.playwright.spring.boot.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 使用 Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式的参数
 */
@Data
public class WkhtmlRenderBO implements Serializable {

    /**
     * 渲染任务Id
     */
    private String randerId;
    /**
     * 选择器
     */
    private String selector;
    /**
     * 图片压缩质量 1-100
     */
    private Integer quality;
    /**
     * PDF是否进行压缩
     */
    private Boolean compress;
    /**
     * Base64 编码的 JSON 格式的字符串
     */
    private String param;
    /**
     * 是否保存到文件
     */
    private Boolean toFile;
    /**
     * 待渲染为为 PDF 和各种图像格式的 HTML URL 访问地址;多个使用,分割
     */
    private List<BufferTemp> urls;
    /**
     * PDF 作者
     */
    private String author;
    /**
     * PDF 关键字
     */
    private String keywords;
    /**
     * PDF 主题
     */
    private String subject;
    /**
     * PDF 标题
     */
    private String title;
    /**
     * PDF 创建者
     */
    private String creator;
    /**
     * PDF 页面大小，LETTER, LEGAL, A0, A1, A2, A3, A4, A5, A6
     */
    private String pageSize;
    /**
     * 是否异步处理每页截图/生成PDF
     */
    private boolean async = true;
}
