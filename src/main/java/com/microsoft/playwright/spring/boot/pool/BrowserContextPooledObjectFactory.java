package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.PlaywrightProperties;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.springframework.beans.factory.DisposableBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Slf4j
public class BrowserContextPooledObjectFactory implements PooledObjectFactory<BrowserContext>, DisposableBean {

    /**
     * Playwright管理容器
     */
    private static final Map<BrowserContext, Playwright> PLAYWRIGHT_MAP = new ConcurrentHashMap<>();
    private static final Map<BrowserContext, Path> CONTEXT_DIR_MAP = new ConcurrentHashMap<>();
    private static final Map<Path, Long> CONTEXT_DIR_LAST_USED = new ConcurrentHashMap<>();
    private static final Map<Path, Long> CONTEXT_DIR_SIZE = new ConcurrentHashMap<>();
    private static final int MAX_CONTEXT_DIRS = 100;
    private static final String CONTEXT_DIR_PREFIX = "context_";
    private static final AtomicInteger contextCounter = new AtomicInteger(0);
    private static final long DIR_USAGE_TIMEOUT = 30 * 60 * 1000; // 30分钟
    private static final long MAX_DIR_SIZE = 100 * 1024 * 1024; // 100MB
    private static final ReentrantLock dirCreationLock = new ReentrantLock();
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long RESOURCE_CLEANUP_TIMEOUT_MS = 30000;

    private final PlaywrightProperties playwrightProperties;

    public BrowserContextPooledObjectFactory(PlaywrightProperties playwrightProperties) {
        this.playwrightProperties = playwrightProperties;
        // 在构造函数中预分配目录
        if (!playwrightProperties.isIsolated()) {
            File userDataRootDir = new File(playwrightProperties.getLaunchPersistentContextOptions().getUserDataDir());
            if (userDataRootDir.exists()) {
                preallocateContextDirs(userDataRootDir);
            }
        }
    }

    /**
     * 从池中取出一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be activated
     *
     * @throws Exception if there is a problem activating {@code obj}
     */
    @Override
    public void activateObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        log.info("Activate BrowserContext Instance '{}'.", browserContext);
        if(Objects.nonNull(browserContext)){
            browserContext.clearCookies();
        }
    }

    /**
     * 销毁一个池中物（playwright）时调用
     * @param p a {@code PooledObject} wrapping the instance to be destroyed
     *
     * @throws Exception if there is a problem destroying {@code obj}
     */
    @Override
    public void destroyObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        if (Objects.isNull(browserContext)) {
            return;
        }
        
        int retryCount = 0;
        Exception lastException = null;
        
        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                // 设置超时
                CompletableFuture<Void> cleanupFuture = CompletableFuture.runAsync(() -> {
                    try {
                        // 1. 清理浏览器上下文
                        try {
                            PlaywrightUtil.cleanupBrowserContext(browserContext);
                        } catch (Exception e) {
                            log.warn("Error cleaning up browser context, continuing with cleanup", e);
                        }
                        
                        // 2. 关闭上下文
                        try {
                            if (browserContext.browser() != null && browserContext.browser().isConnected()) {
                                browserContext.close();
                            }
                        } catch (Exception e) {
                            log.warn("Error closing browser context, continuing with cleanup", e);
                        }
                        
                        // 3. 清理 Playwright 实例
                        Playwright playwright = PLAYWRIGHT_MAP.remove(browserContext);
                        if (playwright != null) {
                            try {
                                playwright.close();
                                log.info("Cleaned up Playwright instance in destroyObject");
                            } catch (Exception e) {
                                log.warn("Error closing Playwright instance, continuing with cleanup", e);
                            }
                        }
                        
                        // 4. 清理目录
                        Path contextDir = CONTEXT_DIR_MAP.remove(browserContext);
                        if (contextDir != null) {
                            CONTEXT_DIR_LAST_USED.remove(contextDir);
                            CONTEXT_DIR_SIZE.remove(contextDir);
                            try {
                                FileUtils.deleteDirectory(contextDir.toFile());
                                log.info("Cleaned up Playwright context directory in destroyObject");
                            } catch (Exception e) {
                                log.error("Error cleaning up Playwright context directory", e);
                                // 不抛出异常，继续清理
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error during resource cleanup", e);
                        throw new CompletionException(e);
                    }
                });
                
                // 等待清理完成或超时
                try {
                    cleanupFuture.get(RESOURCE_CLEANUP_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    return; // 清理成功，直接返回
                } catch (TimeoutException e) {
                    log.warn("Resource cleanup timed out after {} ms", RESOURCE_CLEANUP_TIMEOUT_MS);
                    throw new Exception("Resource cleanup timed out", e);
                }
            } catch (Exception e) {
                lastException = e;
                retryCount++;
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    log.warn("Retry {} of {} for resource cleanup", retryCount, MAX_RETRY_ATTEMPTS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }
        
        // 所有重试都失败
        log.error("Failed to cleanup resources after {} attempts", MAX_RETRY_ATTEMPTS, lastException);
        throw lastException;
    }

   protected Consumer<Browser> browserDisconnectedHandler = browser -> {
       log.info("Browser disconnected, cleaning up resources...");
       browser.contexts().forEach(context -> {
           try {
               // 获取相关的 BrowserContext
               if (Objects.nonNull(context)) {
                   try {
                       // 清理浏览器上下文
                       PlaywrightUtil.cleanupBrowserContext(context);
                   } catch (Exception e) {
                       log.warn("Error cleaning up browser context on disconnect", e);
                   }

                   try {
                       if (context.browser() != null && context.browser().isConnected()) {
                           context.close();
                       }
                   } catch (Exception e) {
                       log.warn("Error closing browser context on disconnect", e);
                   }

                   // 从 Map 中移除并关闭 Playwright
                   Playwright pw = PLAYWRIGHT_MAP.remove(context);
                   if (Objects.nonNull(pw)) {
                       try {
                           pw.close();
                           log.info("Cleaned up Playwright instance on browser disconnect");
                       } catch (Exception e) {
                           log.warn("Error closing Playwright instance on disconnect", e);
                       }
                   }

                   Path contextDir = CONTEXT_DIR_MAP.remove(context);
                   if (contextDir != null) {
                       try {
                           FileUtils.deleteDirectory(contextDir.toFile());
                           log.info("Cleaned up Playwright context directory on disconnect");
                       } catch (Exception e) {
                           log.error("Error cleaning up Playwright context directory on disconnect", e);
                       }
                   }
               }
           } catch (Exception e) {
               log.error("Error cleaning up resources on browser disconnect", e);
           }
       });
   };



    /**
     * 创建池中物（playwright）
     * @return a new instance that can be served by the pool
     */
    @Override
    public PooledObject<BrowserContext> makeObject() {
        log.info("Create Playwright Instance .");
        Playwright playwright = Playwright.create();
        log.info("Create Playwright Instance '{}' Success.", playwright);
        // Browser Type
        PlaywrightProperties.BrowserTypeEnum browserTypeEnum = Objects.nonNull(playwrightProperties.getBrowserType()) ? playwrightProperties.getBrowserType() : PlaywrightProperties.BrowserTypeEnum.chromium;
        // Get Browser Launch Options
        BrowserType.LaunchOptions launchOptions = Objects.nonNull(playwrightProperties.getLaunchOptions()) ? playwrightProperties.getLaunchOptions().toOptions() : new BrowserType.LaunchOptions().setHeadless(true);
        // Get Browser
        log.info("Create Browser Instance .");
        BrowserType browserType = browserTypeEnum.getBrowserType(playwright);
        // 判断浏览器会话是否隔离
        if(playwrightProperties.isIsolated()){
            Browser browser = browserType.launch(launchOptions);
            // 添加断开连接监听器
            browser.onDisconnected(browserDisconnectedHandler);
            log.info("Create Browser Instance {} Success.", browser);
            // Get Browser New Context Options
            Browser.NewContextOptions newContextOptions = Objects.nonNull(playwrightProperties.getNewContextOptions()) ?
                    playwrightProperties.getNewContextOptions().toOptions() : new Browser.NewContextOptions();
            // 使用 Browser.newContext() 方法创建隔离的非持久性浏览器上下文。非持久性浏览器上下文不会将任何浏览数据写入磁盘。
            BrowserContext browserContext = browser.newContext(newContextOptions);
            log.info("Create BrowserContext Instance '{}', browserType : {} , Success.", browserContext, browserType);
            PLAYWRIGHT_MAP.put(browserContext, playwright);
            return new DefaultPooledObject<>(browserContext);
        } else {
            // Get Browser Launch Options
            BrowserType.LaunchPersistentContextOptions launchPersistentContextOptions = Objects.nonNull(playwrightProperties.getLaunchPersistentContextOptions()) ?
                    playwrightProperties.getLaunchPersistentContextOptions().toOptions() : new BrowserType.LaunchPersistentContextOptions().setHeadless(true);
            File userDataRootDir = new File(playwrightProperties.getLaunchPersistentContextOptions().getUserDataDir());
            if (!userDataRootDir.exists()) {
                userDataRootDir.mkdirs();
                log.info("Create User Data Root Directory '{}' Success.", userDataRootDir);
                // 创建目录后预分配上下文目录
                preallocateContextDirs(userDataRootDir);
            }
            // 获取下一个可用的上下文目录
            Path contextDir = getNextContextDir(userDataRootDir);
            // 更新目录使用时间
            CONTEXT_DIR_LAST_USED.put(contextDir, System.currentTimeMillis());
            // 更新目录大小
            updateDirectorySize(contextDir);
            // Launches browser that uses persistent storage located at userDataDir and returns the only context. Closing this context will automatically close the browser.
            BrowserContext browserContext = browserType.launchPersistentContext(contextDir, launchPersistentContextOptions);
            // 添加断开连接监听器
            if (Objects.nonNull(browserContext) && Objects.nonNull(browserContext.browser())) {
                browserContext.browser().onDisconnected(browserDisconnectedHandler);
            }
            log.info("Create Persistent BrowserContext Instance '{}', browserType : {} , Success.", browserContext, browserType);
            PLAYWRIGHT_MAP.put(browserContext, playwright);
            CONTEXT_DIR_MAP.put(browserContext, contextDir);
            return new DefaultPooledObject<>(browserContext);
        }
    }

    /**
     * 预分配目录
     * @param userDataRootDir 用户数据根目录
     */
    private void preallocateContextDirs(File userDataRootDir) {
        dirCreationLock.lock();
        try {
            for (int i = 0; i < MAX_CONTEXT_DIRS; i++) {
                String dirName = CONTEXT_DIR_PREFIX + String.format("%03d", i);
                Path dirPath = Paths.get(userDataRootDir.getAbsolutePath(), dirName);
                if (!dirPath.toFile().exists()) {
                    dirPath.toFile().mkdirs();
                    CONTEXT_DIR_LAST_USED.put(dirPath, System.currentTimeMillis());
                    CONTEXT_DIR_SIZE.put(dirPath, 0L);
                }
            }
        } finally {
            dirCreationLock.unlock();
        }
    }

    /**
     * 获取目录大小
     * @param dir 要检查的目录
     * @return 目录大小
     */
    private long getDirectorySize(Path dir) {
        try {
            return Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            log.error("Error getting file size: {}", p, e);
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            log.error("Error calculating directory size: {}", dir, e);
            return 0L;
        }
    }

    /**
     * 更新目录大小
     * @param dir 要更新的目录
     */
    private void updateDirectorySize(Path dir) {
        long size = getDirectorySize(dir);
        CONTEXT_DIR_SIZE.put(dir, size);
        if (size > MAX_DIR_SIZE) {
            log.warn("Context directory {} exceeds size limit: {} bytes", dir, size);
        }
    }

    /**
     * 获取下一个可用的上下文目录
     * @param userDataRootDir 用户数据根目录
     * @return 新的上下文目录路径
     */
    private Path getNextContextDir(File userDataRootDir) {
        dirCreationLock.lock();
        try {
            // 清理旧的目录
            cleanupOldContextDirs(userDataRootDir);
            
            // 使用负载均衡策略选择目录
            return findLeastUsedDir(userDataRootDir);
        } finally {
            dirCreationLock.unlock();
        }
    }

    /**
     * 查找使用最少的目录
     * @param userDataRootDir 用户数据根目录
     * @return 使用最少的目录路径
     */
    private Path findLeastUsedDir(File userDataRootDir) {
        return CONTEXT_DIR_LAST_USED.entrySet().stream()
                .filter(entry -> isDirectoryNotUsed(entry.getKey()))
                .min(Comparator.comparingLong(entry -> {
                    Path dir = entry.getKey();
                    long lastUsed = entry.getValue();
                    long size = CONTEXT_DIR_SIZE.getOrDefault(dir, 0L);
                    // 综合考虑最后使用时间和目录大小
                    return lastUsed + (size * 1000); // 每1MB增加1秒的权重
                }))
                .map(Map.Entry::getKey)
                .orElseGet(() -> {
                    // 如果没有可用目录，创建新目录
                    int nextNumber = contextCounter.getAndIncrement() % MAX_CONTEXT_DIRS;
                    String dirName = CONTEXT_DIR_PREFIX + String.format("%03d", nextNumber);
                    Path newDir = Paths.get(userDataRootDir.getAbsolutePath(), dirName);
                    if (!newDir.toFile().exists()) {
                        newDir.toFile().mkdirs();
                    }
                    CONTEXT_DIR_LAST_USED.put(newDir, System.currentTimeMillis());
                    CONTEXT_DIR_SIZE.put(newDir, 0L);
                    return newDir;
                });
    }

    /**
     * 清理旧的上下文目录
     * @param userDataRootDir 用户数据根目录
     */
    private void cleanupOldContextDirs(File userDataRootDir) {
        File[] contextDirs = userDataRootDir.listFiles((dir, name) -> name.startsWith(CONTEXT_DIR_PREFIX));
        if (contextDirs != null) {
            for (File dir : contextDirs) {
                Path dirPath = dir.toPath();
                if (isDirectoryNotUsed(dirPath)) {
                    long lastUsed = CONTEXT_DIR_LAST_USED.getOrDefault(dirPath, 0L);
                    long size = CONTEXT_DIR_SIZE.getOrDefault(dirPath, 0L);
                    
                    // 如果目录超过大小限制或超时未使用，则清理
                    if (size > MAX_DIR_SIZE || System.currentTimeMillis() - lastUsed > DIR_USAGE_TIMEOUT) {
                        try {
                            FileUtils.deleteDirectory(dir);
                            CONTEXT_DIR_LAST_USED.remove(dirPath);
                            CONTEXT_DIR_SIZE.remove(dirPath);
                            log.info("Cleaned up context directory: {}, size: {} bytes, last used: {} ms ago", 
                                    dir.getAbsolutePath(), size, System.currentTimeMillis() - lastUsed);
                        } catch (Exception e) {
                            log.error("Error cleaning up context directory: {}", dir.getAbsolutePath(), e);
                        }
                    }
                }
            }
        }
    }

    /**
     * 检查目录是否正在使用
     * @param dir 要检查的目录
     * @return 如果目录正在使用返回true，否则返回false
     */
    private boolean isDirectoryNotUsed(Path dir) {
        // 检查目录是否在CONTEXT_DIR_MAP中
        if (CONTEXT_DIR_MAP.containsValue(dir)) {
            return false;
        }
        
        // 检查目录最后使用时间
        Long lastUsed = CONTEXT_DIR_LAST_USED.get(dir);
        if (lastUsed != null) {
            // 如果目录在超时时间内被使用过，认为它仍在被使用
            return System.currentTimeMillis() - lastUsed >= DIR_USAGE_TIMEOUT;
        }
        
        return true;
    }

    /**
     * 归还一个池中物（playwright）时调用，不应该activateObject冲突
     * @param p a {@code PooledObject} wrapping the instance to be passivated
     *
     * @throws Exception if there is a problem passivating {@code obj}
     */
    @Override
    public void passivateObject(PooledObject<BrowserContext> p) throws Exception {
        BrowserContext browserContext = p.getObject();
        log.info("Return BrowserContext Instance '{}'.", browserContext);
        if(Objects.nonNull(browserContext)){
            try {
                // 1. 清除 cookies
                browserContext.clearCookies();
                
                // 2. 清除 localStorage
                browserContext.pages().forEach(page -> {
                    try {
                        page.evaluate("() => window.localStorage.clear()");
                    } catch (Exception e) {
                        log.warn("Failed to clear localStorage");
                    }
                });
                
                // 3. 清除 sessionStorage
                browserContext.pages().forEach(page -> {
                    try {
                        page.evaluate("() => window.sessionStorage.clear()");
                    } catch (Exception e) {
                        log.warn("Failed to clear sessionStorage");
                    }
                });
                
                // 4. 清除 IndexedDB
                browserContext.pages().forEach(page -> {
                    try {
                        page.evaluate("() => { " +
                                "const databases = window.indexedDB.databases(); " +
                                "databases.then(dbs => dbs.forEach(db => window.indexedDB.deleteDatabase(db.name))); " +
                                "}");
                    } catch (Exception e) {
                        log.warn("Failed to clear IndexedDB");
                    }
                });
                
                // 5. 清除 WebSQL
                browserContext.pages().forEach(page -> {
                    try {
                        page.evaluate("() => { " +
                                "if (window.openDatabase) { " +
                                "  const databases = window.openDatabase('', '', '', 0); " +
                                "  databases.transaction(tx => { " +
                                "    tx.executeSql('SELECT name FROM sqlite_master WHERE type=\"table\"', [], (tx, result) => { " +
                                "      for (let i = 0; i < result.rows.length; i++) { " +
                                "        const tableName = result.rows.item(i).name; " +
                                "        tx.executeSql('DROP TABLE IF EXISTS ' + tableName); " +
                                "      } " +
                                "    }); " +
                                "  }); " +
                                "} " +
                                "}");
                    } catch (Exception e) {
                        log.warn("Failed to clear WebSQL");
                    }
                });
                
                // 6. 关闭所有页面
                browserContext.pages().forEach(PlaywrightUtil::closePage);
                
                // 7. 更新目录大小
                Path contextDir = CONTEXT_DIR_MAP.get(browserContext);
                if (contextDir != null) {
                    updateDirectorySize(contextDir);
                }
                
                log.info("Return BrowserContext Instance : clear all session data success");
            } catch (Exception e) {
                log.error("Error clearing session data", e);
                throw e;
            }
        }
    }

    /**
     * 检测对象是否"有效";Pool中不能保存无效的"对象",因此"后台检测线程"会周期性的检测Pool中"对象"的有效性,如果对象无效则会导致此对象从Pool中移除,并destroy;此外在调用者从Pool获取一个"对象"时,也会检测"对象"的有效性,确保不能讲"无效"的对象输出给调用者;当调用者使用完毕将"对象归还"到Pool时,仍然会检测对象的有效性.所谓有效性,就是此"对象"的状态是否符合预期,是否可以对调用者直接使用;如果对象是Socket,那么它的有效性就是socket的通道是否畅通/阻塞是否超时等.
     * 这里若要检测，需要在PoolConfig中配置检测项目。
     * true：检测正常，符合预期；false：异常，销毁对象
     * @param p a {@code PooledObject} wrapping the instance to be validated
     *
     * @return {@code false} if this object is not currently valid and should be dropped from the pool, {@code true} otherwise.
     */
    @Override
    public boolean validateObject(PooledObject<BrowserContext> p) {
        BrowserContext browserContext = p.getObject();
        log.info("Validate BrowserContext Instance '{}'.", browserContext);
        boolean isValidated = Objects.nonNull(browserContext);
        log.info("Validate BrowserContext : {}, isValidated : {}", browserContext, isValidated);
        return isValidated;
    }

    @Override
    public void destroy() throws Exception {
        PLAYWRIGHT_MAP.forEach((browserContext, playwright) -> {
            try {
                PlaywrightUtil.cleanupBrowserContext(browserContext);
                browserContext.close();
                if (playwright != null) {
                    playwright.close();
                    log.info("Destroy browserContext of Playwright Instance '{}' Success.", playwright);
                }
            } catch (Exception e) {
                log.error("Error destroying browser context", e);
            }
        });
    }

}