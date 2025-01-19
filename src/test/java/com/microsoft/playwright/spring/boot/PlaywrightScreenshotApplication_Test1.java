package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.pool.BrowserPagePool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//@SpringBootApplication
@Slf4j
public class PlaywrightScreenshotApplication_Test1 implements CommandLineRunner {

    protected static final String BASE_DIR = "D://tmp";
    @Autowired
    private BrowserPagePool browserPagePool;

    public static void main(String[] args) throws Exception {
        SpringApplication.run(PlaywrightScreenshotApplication_Test1.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {

            FileUtils.forceMkdir( new File(BASE_DIR));

            // 截图测试
            captureScreenshot(UUID.randomUUID().toString(), BufferTemp.builder().url("https://www.baidu.com").index(1).build(), null).whenComplete((pdf, throwable) -> {
                try(ByteArrayInputStream input = new ByteArrayInputStream(pdf.getBuffer());) {
                    IOUtils.copy(input, Files.newOutputStream(new File(BASE_DIR, "test.png").toPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            }).join();

            // 批量截图测试
            List<BufferTemp> urlTemps = Arrays.asList(
                BufferTemp.builder().url("https://www.baidu.com").index(1).build(),
                BufferTemp.builder().url("https://www.baidu.com").index(2).build()
            );
            List<BufferTemp> screenshots = captureScreenshots(UUID.randomUUID().toString(), urlTemps, null);
            mergeScreenshotsToZip(UUID.randomUUID().toString(), screenshots).whenComplete((pdf, throwable) -> {
                log.info("Zip file path: {}", pdf.getPath());
            }).exceptionally(throwable -> {
                throwable.printStackTrace();
                return null;
            }).join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 定义一个浏览器内容截图方法
     * @param rendeId 渲染ID
     * @param urlTemps url列表
     * @param selector 选择器
     * @return 截图列表
     */
    protected List<BufferTemp> captureScreenshots(String rendeId, List<BufferTemp> urlTemps, String selector) {
        log.info("Capturing screenshots for urls: {}", urlTemps.stream().map(BufferTemp::getUrl).collect(Collectors.toList()));
        // 1、使用CompletableFuture异步处理
        List<CompletableFuture<BufferTemp>> futureList = urlTemps.stream()
                .map(urlTemp -> captureScreenshot(rendeId, urlTemp, selector))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0]));
        CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(CompletableFuture::join)
                        .collect(Collectors.toList()));
        return resultFuture.join();
    }

    /**
     * 定义一个浏览器页面截图方法
     * @param rendeId
     * @param urlTemp
     * @param selector
     * @return
     */
    protected CompletableFuture<BufferTemp> captureScreenshot(String rendeId, BufferTemp urlTemp, String selector){
        // 1、使用CompletableFuture.supplyAsync()方法，异步执行截图
        return CompletableFuture.supplyAsync(() -> {
            log.info("Capturing screenshot : rendeId: {}, selector: {}, url : {}", rendeId, selector, urlTemp.getUrl());
            Browser browser = null;
            try {
                browser = browserPagePool.borrowObject();
                // 从池中获取一个浏览器页面
                Page page = browser.newPage();
                // 设置页面加载参数, 并跳转到url
                page.navigate(urlTemp.getUrl(), new Page.NavigateOptions()
                        .setTimeout(60 * 1000)
                        .setWaitUntil(WaitUntilState.NETWORKIDLE));
                // 定义截图输出路径
                String fileName = String.format("%s.png", urlTemp.getIndex());
                log.info("screenshot start for {} : {}", rendeId, fileName);
                // 截图
                byte[] screenshotBuffer;
                if(StringUtils.isEmpty(selector)){
                    Page.ScreenshotOptions options = new Page.ScreenshotOptions()
                            .setFullPage(true)
                            .setOmitBackground(true)
                            .setTimeout(30 * 1000)
                            .setType(ScreenshotType.PNG);
                    screenshotBuffer = page.screenshot(options);
                } else {

                    // 定位到要截图的元素
                    ElementHandle element = page.querySelector(selector);

                    ElementHandle.ScreenshotOptions options = new ElementHandle.ScreenshotOptions()
                            .setOmitBackground(true)
                            .setTimeout(30 * 1000)
                            .setType(ScreenshotType.PNG);

                    // 截取指定元素的屏幕截图
                    screenshotBuffer = element.screenshot(options);
                }
                log.info("screenshot success for {} : {}", rendeId, fileName);
                urlTemp.setBuffer(screenshotBuffer);
                urlTemp.setName(fileName);
                return urlTemp;
            } catch (Exception e) {
                throw new RuntimeException("Capture screenshot error: {}", e);
            } finally {
                if (Objects.nonNull(browser)){
                    browserPagePool.returnObject(browser);
                }
            }
        });

    }

    /**
     * 定义一个图片合并为Zip方法
     * @param rendeId
     * @param screenshots
     * @return
     */
    protected CompletableFuture<BufferTemp> mergeScreenshotsToZip(String rendeId, List<BufferTemp> screenshots) {
        if(screenshots.size() == 1){
            BufferTemp screenshot = screenshots.get(0);
            String imageFileName = rendeId + "." + FilenameUtils.getExtension(screenshot.getName());
            screenshot.setName(imageFileName);
            return CompletableFuture.completedFuture(screenshot);
        }
        return CompletableFuture.supplyAsync(() -> {
            String zipFileName = rendeId + ".zip";
            log.info("Merging screenshots to ZIP: {}", zipFileName);
            // 请求数+1
            //metrics.playwright_zip_total_requset_count.inc(1);
            File zipFile = new File(BASE_DIR, zipFileName);
            try {
                ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()));
                // 将所有截图写入ZIP文件
                for (BufferTemp screenshot : screenshots) {
                    // 读取图片文件并写入 ZipOutputStream
                    try (FileInputStream fileInput = new FileInputStream(screenshot.getPath())) {
                        String fileName = screenshot.getName();
                        log.info("Merging screenshot to ZIP: {}", fileName);
                        // 创建 ZipEntry 对象
                        ZipEntry zipEntry = new ZipEntry(fileName);
                        zipOutputStream.putNextEntry(zipEntry);
                        // 将截图文件写入 ZipOutputStream
                        IOUtils.copy(fileInput, zipOutputStream);
                        // 关闭当前 ZipEntry
                        zipOutputStream.closeEntry();
                    }
                    zipOutputStream.flush();
                }
                IOUtils.closeQuietly(zipOutputStream);
                //metrics.playwright_zip_total_requset_success_count.inc(1);
                return BufferTemp.builder().index(0).name(zipFileName).path(zipFile.getAbsolutePath()).build();
            } catch (Exception e) {
                throw new PlaywrightException("Failed to pack ZIP File : " + zipFileName, e);
            }
        });
    }
}
