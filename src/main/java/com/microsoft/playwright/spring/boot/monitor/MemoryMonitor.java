package com.microsoft.playwright.spring.boot.monitor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MemoryMonitor {
    
    private static final double MEMORY_THRESHOLD = 0.8; // 80%内存使用率阈值
    
    @Scheduled(fixedRate = 5000) // 每5秒检查一次
    public void monitorMemory() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        double memoryUsage = (double) usedMemory / maxMemory;
        
        if (memoryUsage > MEMORY_THRESHOLD) {
            log.warn("High memory usage detected: {}%, triggering cleanup", 
                String.format("%.2f", memoryUsage * 100));
            System.gc();
        }
        
        log.info("Memory usage: {}/{} MB ({}%)",
            usedMemory / 1024 / 1024,
            maxMemory / 1024 / 1024,
            String.format("%.2f", memoryUsage * 100));
    }
    
    public boolean isMemoryAvailable() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (double) usedMemory / maxMemory < MEMORY_THRESHOLD;
    }
} 