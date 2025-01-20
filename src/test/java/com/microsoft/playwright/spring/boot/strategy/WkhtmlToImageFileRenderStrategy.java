package com.microsoft.playwright.spring.boot.strategy;


import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.spring.boot.bo.BufferTemp;
import com.microsoft.playwright.spring.boot.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.enums.RenderType;
import com.microsoft.playwright.spring.boot.exception.TaskRuntimeException;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import com.microsoft.playwright.spring.boot.vo.WkhtmlRenderResultVO;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 使用 Playwright 渲染引擎将 HTML 渲染为各种图像格式
 */
@Slf4j
@Component
public class WkhtmlToImageFileRenderStrategy extends WkhtmlToImageBufferRenderStrategy {

    @Override
    public RenderType getRenderType() {
        return RenderType.TO_IMAGE_FILE;
    }

    @Override
    protected List<BufferTemp> doGenerate(WkhtmlRenderBO renderBO) throws IOException {
        // 2、生成截图
        return this.captureScreenshots(renderBO);
    }

    @Override
    protected List<BufferTemp> captureScreenshots(WkhtmlRenderBO renderBO) {
        log.info("Capturing screenshots for urls: {}", renderBO.getUrls().stream().map(BufferTemp::getUrl).collect(Collectors.toList()));

        if(renderBO.getType() == 0) {
            try {
                // 1、使用CompletableFuture异步处理

                try(Browser browser = PlaywrightUtil.getBrowser(playwrightProperties)) {
                    List<CompletableFuture<BufferTemp>> futureList = new ArrayList<>();
                    for (BufferTemp urlTemp : renderBO.getUrls()) {
                        futureList.add(captureScreenshotFuture1(browser, renderBO.getRanderId(), urlTemp, renderBO.getSelector()));
                    }
                    // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
                    CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
                    return renderBO.getUrls().stream().filter(urlTemp -> Objects.nonNull(urlTemp.getBuffer())).collect(Collectors.toList());
                }


            } catch (Exception e) {
                throw new TaskRuntimeException("Failed to create browser instance: " + e.getMessage());
            }
        }
        if(renderBO.getType() == 1){
            try {
                // 1、使用CompletableFuture异步处理
                try(Browser browser = PlaywrightUtil.getBrowser(playwrightProperties)) {
                    List<CompletableFuture<BufferTemp>> futureList = new ArrayList<>();
                    for (BufferTemp urlTemp : renderBO.getUrls()) {
                        futureList.add(captureScreenshotFuture1(browser, renderBO.getRanderId(), urlTemp, renderBO.getSelector()));
                    }
                    // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
                    CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
                    return futureList.stream().map(CompletableFuture::join).filter(urlTemp -> Objects.nonNull(urlTemp.getPath())).collect(Collectors.toList());
                } catch (Exception e) {
                    throw new TaskRuntimeException("Failed to create browser instance: " + e.getMessage());
                }
            } catch (Exception e) {
                throw new TaskRuntimeException("Failed to create browser instance: " + e.getMessage());
            }
        }
        if(renderBO.getType() == 2) {
            // Page page = null;
            BrowserContext browserContext = null;
            try {
                browserContext = browserContextPool.borrowObject();
                List<CompletableFuture<BufferTemp>> futureList = new ArrayList<>();
                for (BufferTemp urlTemp : renderBO.getUrls()) {
                    futureList.add(captureScreenshotFuture2(browserContext, renderBO.getRanderId(), urlTemp, renderBO.getSelector()));
                }
                // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
                CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
                return futureList.stream().map(CompletableFuture::join).filter(urlTemp -> Objects.nonNull(urlTemp.getPath())).collect(Collectors.toList());
            } catch (Exception e) {
                log.error("Capture screenshot error: ", e);
                throw new PlaywrightException("Capture screenshot error", e);
            } finally {
                if (Objects.nonNull(browserContext)) {
                    browserContextPool.returnObject(browserContext);
                }
            }
        }

        if(renderBO.getType() == 3) {
            List<CompletableFuture<BufferTemp>> futureList = new ArrayList<>();
            for (BufferTemp urlTemp : renderBO.getUrls()) {
                futureList.add(captureScreenshotFuture3(renderBO.getRanderId(), urlTemp, renderBO.getSelector()));
            }
            // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
            return futureList.stream().map(CompletableFuture::join).filter(urlTemp -> Objects.nonNull(urlTemp.getPath())).collect(Collectors.toList());
        }
        /*
        List<CompletableFuture<BufferTemp>> futureList = renderBO.getUrls().stream()
                .map(urlTemp -> captureScreenshotFuture(renderBO.getRanderId(), urlTemp, renderBO.getSelector()))
                .collect(Collectors.toList());
        // 2、使用CompletableFuture.allOf()方法，等待所有异步线程执行完毕
        CompletableFuture<Void> allFuture = CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()]));
        CompletableFuture<List<BufferTemp>> resultFuture = allFuture
                .thenApply(v -> futureList.stream().map(CompletableFuture::join).filter(urlTemp -> StringUtils.isNotBlank(urlTemp.getPath())).collect(Collectors.toList()));
        return resultFuture.join();*/
        return null;
    }


    @Override
    protected List<BufferTemp> doCompress(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) {
        // 1、获取压缩质量，如果压缩质量不在范围内，则不压缩
        Integer quality = renderBO.getQuality();
        if((quality > MAX_QUALITY || quality <= MIN_QUALITY)){
            log.info("Compressing screenshot ignore.");
            return screenshots;
        }
        // 2、异步压缩图片
        return compressScreenshots(screenshots, quality);
    }

    /**
     * 定义一个图片压缩方法
     * @param screenshot
     * @param quality
     * @return
     */
    @Override
    protected CompletableFuture<BufferTemp> compressScreenshot(BufferTemp screenshot, Integer quality) {
        // 判断压缩质量是否在范围内
        if(quality < MAX_QUALITY && quality > MIN_QUALITY){
            return CompletableFuture.supplyAsync(() -> {
                try{
                    log.info("Compressing screenshot file : {}", screenshot.getPath());
                    File sourceFile = new File(screenshot.getPath());
                    File outFile = new File(playwrightRenderProperties.getTmpDir(), screenshot.getName());
                    Thumbnails.of(sourceFile)
                            .allowOverwrite(true)
                            .scale(1f)
                            .outputQuality(quality / 100f)
                            .toFile(outFile);
                    screenshot.setPath(outFile.getAbsolutePath());
                    log.info("Compressing screenshot file success : {}", screenshot.getName());
                } catch (Exception e) {
                    throw new TaskRuntimeException("Compressing screenshot file error: " +  e.getMessage());
                }
                return screenshot ;
            }, dtpToImageCompressExecutor);
        }
        log.info("Compressing screenshot ignore: {}", screenshot.getName());
        return CompletableFuture.completedFuture(screenshot);
    }

    @Override
    public WkhtmlRenderResultVO doPacking(WkhtmlRenderBO renderBO, List<BufferTemp> screenshots) throws IOException {
        WkhtmlRenderResultVO resultBO = new WkhtmlRenderResultVO();
        // 1、判断操作系统，如果是windows，则使用mergeScreenshotsToZip方法，否则使用packScreenshotsToZip方法
        if(SystemUtils.IS_OS_WINDOWS){
            mergeScreenshotsToZip(renderBO.getRanderId(), screenshots).thenAccept(bufferTemp -> {
                resultBO.setFilePath(bufferTemp.getPath());
                resultBO.setFileName(bufferTemp.getName());
            }).join();
        } else {
            packScreenshotsToZip(renderBO.getRanderId(), screenshots).thenAccept(bufferTemp -> {
                resultBO.setFilePath(bufferTemp.getPath());
                resultBO.setFileName(bufferTemp.getName());
            }).join();;
        }
        return resultBO;
    }

    /**
     * 定义一个图片合并为Zip方法
     * @param rendeId
     * @param screenshots
     * @return
     */
    @Override
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
            File zipFile = new File(playwrightRenderProperties.getTmpDir(), zipFileName);
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
                throw new TaskRuntimeException("Failed to pack ZIP File : " + zipFileName, e);
            }
        }, dtpToImageZipExecutor);
    }

    protected CompletableFuture<BufferTemp> packScreenshotsToZip(String rendeId, List<BufferTemp> screenshots) {
        if(screenshots.size() == 1){
            return CompletableFuture.completedFuture(screenshots.get(0));
        }
        log.info("Packing screenshots to ZIP: {}", rendeId);
        String zipFileName = rendeId + ".zip";
        return CompletableFuture.supplyAsync(() -> {
            // 如果有多个文件，则打包成zip
            try {
                // 创建执行器
                DefaultExecutor executor = new DefaultExecutor();
                executor.setWorkingDirectory(new File(playwrightRenderProperties.getTmpDir()));
                // 创建监控时间10分钟，超过10分钟则中断执行
                ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 60 * 1000);
                executor.setWatchdog(watchdog);
                /**
                 zip -r ../files.zip ./*
                 zip [参数] [打包后的文件名] [打包的目录路径]
                 linux zip命令参数列表：
                 -a 将文件转成ASCII模式
                 -F 尝试修复损坏的压缩文件
                 -h 显示帮助界面
                 -m 将文件压缩之后，删除源文件
                 -n 特定字符串 不压缩具有特定字尾字符串的文件
                 -o 将压缩文件内的所有文件的最新变动时间设为压缩时候的时间
                 -q 安静模式，在压缩的时候不显示指令的执行过程
                 -r 将指定的目录下的所有子目录以及文件一起处理
                 -S 包含系统文件和隐含文件（S是大写）
                 -t 日期 把压缩文件的最后修改日期设为指定的日期，日期格式为mmddyyyy
                 */
                // zip 命令行
                CommandLine cmdLine = new CommandLine("zip");
                cmdLine.addArgument("-r");
                cmdLine.addArgument(zipFileName);
                cmdLine.addArgument(rendeId);
                // 执行 zip 命令行
                executor.execute(cmdLine);
                // 返回结果
                File zipFile = new File(playwrightRenderProperties.getTmpDir(), zipFileName);
                return BufferTemp.builder().index(0).name(zipFileName).path(zipFile.getAbsolutePath()).build();
            } catch (Exception e) {
                throw new TaskRuntimeException("Failed to pack ZIP File : " + zipFileName, e);
            }
        }, dtpToImageZipExecutor);
    }

}
