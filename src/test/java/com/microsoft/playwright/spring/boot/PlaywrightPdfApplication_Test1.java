package com.microsoft.playwright.spring.boot;

import com.alibaba.fastjson2.JSON;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.strategy.PlaywrightRenderStrategyRouter;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import hitool.core.lang3.uid.Sequence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Base64Utils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@EnableConfigurationProperties(PlaywrightRenderProperties.class)
@SpringBootApplication
@Slf4j
public class PlaywrightPdfApplication_Test1 implements CommandLineRunner {

    @Bean
    public Sequence sequence() {
        return new Sequence();
    }

    @Bean
    public ThreadPoolExecutor dtpToImageExecutor(){
        return new ThreadPoolExecutor(4, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Bean
    public ThreadPoolExecutor dtpToImageCompressExecutor(){
        return new ThreadPoolExecutor(4, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
    @Bean
    public ThreadPoolExecutor dtpToImageZipExecutor(){
        return new ThreadPoolExecutor(4, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
    @Bean
    public ThreadPoolExecutor dtpToPdfExecutor(){
        return new ThreadPoolExecutor(4, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
    @Bean
    public ThreadPoolExecutor dtpToPdfMergeExecutor(){
        return new ThreadPoolExecutor(4, 20, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PlaywrightPdfApplication_Test1.class, args);
    }

    @Resource
    protected Sequence sequence;
    @Autowired
    private PlaywrightRenderStrategyRouter renderStrategyRouter;

    @Override
    public void run(String... args) throws Exception {
        List<String> reportUrls = Arrays.asList("https://www.baidu.com", "https://www.baidu.com");


        Map<String, Object> params = new HashMap<>();
        params.put("report_urls", reportUrls);
        // 2.1、配置 PDF 渲染参数
        WkhtmlRenderBO renderBO = new WkhtmlRenderBO();
        //renderBO.setSchoolCode(schoolCode);
        //renderBO.setUserId(userId);
        renderBO.setSelector(".growthMain");
        renderBO.setQuality(80);
        renderBO.setCompress(false);
        renderBO.setPageSize("A4");
        renderBO.setParam(Base64Utils.encodeToString(JSON.toJSONString(params).getBytes()));
        renderBO.setRanderId(Objects.toString(sequence.nextId()));
        WkhtmlRenderResultVO resultBO = null;
        try {
            // 2.2、渲染 PDF
            resultBO = renderStrategyRouter.route(RenderType.TO_PDF_FILE).render(renderBO);

        } catch (Exception e) {
            log.error("pdf生成失败;", e);
        } finally {
            // 3、删除临时文件
            renderStrategyRouter.route(RenderType.TO_PDF_FILE).cleanTemporary(renderBO, resultBO);
        }
    }

}
