package com.microsoft.playwright.spring.boot.playwright.page.supplier;

import com.microsoft.playwright.spring.boot.PlaywrightRenderProperties;
import com.microsoft.playwright.spring.boot.playwright.bo.PageScreenshotTemp;
import com.microsoft.playwright.spring.boot.playwright.bo.WkhtmlRenderBO;
import com.microsoft.playwright.spring.boot.playwright.exception.TaskRuntimeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class PageScreenshotPackToZipFileSupplier implements Supplier<PageScreenshotTemp> {

    @Getter
    protected PlaywrightRenderProperties playwrightRenderProperties;
    @Getter
    protected WkhtmlRenderBO renderBO;
    @Getter
    protected List<PageScreenshotTemp> screenshots;

    public PageScreenshotPackToZipFileSupplier(PlaywrightRenderProperties playwrightRenderProperties,
                                               WkhtmlRenderBO renderBO,
                                               List<PageScreenshotTemp> screenshots) {
        this.playwrightRenderProperties = playwrightRenderProperties;
        this.renderBO = renderBO;
        this.screenshots = screenshots;
    }

    @Override
    public PageScreenshotTemp get() {
        String zipFileName = renderBO.getTaskId() + ".zip";
        log.info("Packing screenshots to ZIP: {}", zipFileName);
        try {
            // 创建执行器
            DefaultExecutor executor = DefaultExecutor.builder()
                    .setWorkingDirectory(new File(playwrightRenderProperties.getTmpDir()))
                    .get();
            // 创建监控时间10分钟，超过10分钟则中断执行
            ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofMinutes(10)).get();
            executor.setWatchdog(watchdog);
            /*
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
            cmdLine.addArgument(renderBO.getTaskId());
            // 执行 zip 命令行
            executor.execute(cmdLine);
            // 返回结果
            File zipFile = new File(playwrightRenderProperties.getTmpDir(), zipFileName);
            return new PageScreenshotTemp().setIndex(0).setName(zipFileName).setPath(zipFile.getAbsolutePath());
        } catch (Exception e) {
            throw new TaskRuntimeException("Failed to pack ZIP File : " + zipFileName, e);
        }
    }
}