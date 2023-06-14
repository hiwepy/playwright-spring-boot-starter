package com.microsoft.playwright.spring.boot.pool;

import com.microsoft.playwright.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

@Slf4j
public class BrowserPagePool extends GenericObjectPool<Page> {

    public BrowserPagePool(PooledObjectFactory<Page> factory) {
        super(factory);
        registerMXBean();
    }

    public BrowserPagePool(PooledObjectFactory<Page> factory, GenericObjectPoolConfig<Page> config) {
        super(factory, config);;
        registerMXBean();
    }

    public BrowserPagePool(PooledObjectFactory<Page> factory, GenericObjectPoolConfig<Page> config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);;
        registerMXBean();
    }

    private void registerMXBean() {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = getObjectName();
            if (!mBeanServer.isRegistered(objectName)) {
                mBeanServer.registerMBean(this, objectName);
                log.info("MXBean {} 注册成功", objectName);
            } else {
               log.info("MXBean {} 已经注册", objectName);
            }
        } catch (Exception e) {
            log.error("注册MXBean时发生异常", e);
        }
    }

    private ObjectName getObjectName() throws MalformedObjectNameException {
        String className = getClass().getSimpleName();
        String packageName = getClass().getPackage().getName();
        String objectName = packageName + ":type=" + className;

        return new ObjectName(objectName);
    }

}
