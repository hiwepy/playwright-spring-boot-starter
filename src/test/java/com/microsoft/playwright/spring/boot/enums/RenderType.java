package com.microsoft.playwright.spring.boot.enums;

public enum RenderType {

    TO_PDF_FILE("PDF输出到文件"),
    TO_PDF_BUFFER("PDF输出到缓存"),
    TO_PDF_MERGER_FILE("PDF输出到文件"),
    TO_PDF_MERGER_BUFFER("PDF输出到缓存"),
    TO_IMAGE_FILE("图片输出到文件"),
    TO_IMAGE_BUFFER("图片输出到缓存");

    private String desc ;

    RenderType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
