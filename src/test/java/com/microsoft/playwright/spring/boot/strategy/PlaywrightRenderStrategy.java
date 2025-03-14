package com.microsoft.playwright.spring.boot.strategy;


import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;

import java.util.function.Function;

/**
 * Playwright 处理策略
 */
public interface PlaywrightRenderStrategy<B extends WkhtmlRenderBO> {

    /**
     * 使用 Playwright 渲染引擎将 HTML 渲染为 PDF 和各种图像格式
     * @param renderBO 渲染参数来源 BO
     */
    WkhtmlRenderResultVO render(B renderBO) throws Exception;
 
    /**
     * 设置自定义判断方法
     * @param function 自定义判断方法
     */
    void customPresentable(Function<BufferTemp, Boolean> function);

    /**
     * 清理临时文件
     * @param renderBO 渲染参数来源 BO
     * @param resultBO 渲染结果 BO
     */
    void cleanTemporary(B renderBO, WkhtmlRenderResultVO resultBO);

    /**
     * 处理类型
     * @return 渲染类型
     */
    RenderType getRenderType();

}
