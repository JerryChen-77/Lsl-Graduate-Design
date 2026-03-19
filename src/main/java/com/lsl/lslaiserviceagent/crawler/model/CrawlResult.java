package com.lsl.lslaiserviceagent.crawler.model;

import lombok.Data;
import org.openqa.selenium.Cookie;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 爬取结果内部类
 */
@Data
public class CrawlResult {
    private String url;
    private String title;
    private String currentUrl;
    private String html;
    private Set<Cookie> cookies;
    private boolean success;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public long getDurationSeconds() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).getSeconds();
        }
        return 0;
    }
}