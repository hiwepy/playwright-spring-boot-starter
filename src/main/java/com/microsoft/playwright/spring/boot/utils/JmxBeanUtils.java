package com.microsoft.playwright.spring.boot.utils;

public class JmxBeanUtils {

    public static <T> String getObjectName(Class<T> tClass) {
        String packageName = tClass.getPackage().getName();
        String objectName = packageName + ":type=" + tClass.getSimpleName();
        return objectName;
    }

}
