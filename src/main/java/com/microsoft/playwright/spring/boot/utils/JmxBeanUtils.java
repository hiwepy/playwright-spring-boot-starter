package com.microsoft.playwright.spring.boot.utils;

public class JmxBeanUtils {

    public static <T> String getObjectName(Class<T> tClass) {
        String packageName = tClass.getPackage().getName();
        return packageName + ":type=" + tClass.getSimpleName();
    }

}
