package com.microsoft.playwright.spring.boot.util;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.options.Cookie;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BrowserUtil {

    public static void copyCookies(HttpServletRequest request, BrowserContext browserContext) {
        if (Objects.isNull(request)) {
            return;
        }
        javax.servlet.http.Cookie[] requestCookies = request.getCookies();
        if(Objects.isNull(browserContext) || ArrayUtils.isEmpty(requestCookies)){
            return;
        }
        List<Cookie> cookies = Arrays.stream(requestCookies)
                .filter(cookie -> StringUtils.isNotEmpty(cookie.getDomain()) || StringUtils.isNotEmpty(cookie.getPath()))
                .map(cookie -> {
                    Cookie cookie1 = new Cookie(cookie.getName(), cookie.getValue());
                    cookie1.setDomain(cookie.getDomain());
                    cookie1.setExpires(cookie.getMaxAge());
                    cookie1.setHttpOnly(cookie.isHttpOnly());
                    cookie1.setPath(cookie.getPath());
                    cookie1.setSecure(cookie.getSecure());
                    return cookie1;
                }).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cookies)){
            return;
        }
        browserContext.addCookies(cookies);
    }

}
