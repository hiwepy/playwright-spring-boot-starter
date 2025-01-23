package com.microsoft.playwright.spring.boot;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.spring.boot.utils.PlaywrightUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PlaywrightMultiThreadExample {

    public static void main(String[] args) {
        try (Playwright playwright = PlaywrightUtil.getInstance()) {
            Browser browser = playwright.chromium().launch();
            List<String> urls = Arrays.asList("https://example.com", "https://example.org", "https://example.net");

            List<CompletableFuture<Void>> futures = urls.stream()
                    .map(url -> openPageAsync(browser, url))
                    .collect(Collectors.toList());

            // Wait for all pages to be opened
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    private static CompletableFuture<Void> openPageAsync(Browser browser, String url) {
        return CompletableFuture.runAsync(() -> {
            try (Page page = browser.newPage()) {
                page.navigate(url);
                System.out.println("Opened page: " + url);
                // Perform additional actions on the page if needed
            }
        });
    }
}
