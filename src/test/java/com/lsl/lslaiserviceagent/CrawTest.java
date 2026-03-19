package com.lsl.lslaiserviceagent;

import com.lsl.lslaiserviceagent.crawler.model.CrawlResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


/**
 * 完整的Selenium爬虫工具类
 */
@Slf4j
public class CrawTest {
    private static final String EDGE_BROWER = "edge";

    private static final List<String> URLS2CRAW = List.of(
            // 中国联通官网
            "https://www.chinaunicom.com.cn/",
            // 中国移动官网
            "https://www.10086.cn/",
            //中国联通套餐网页
            "https://mall.10010.com/",
            // 中国电信
            "https://www.189.cn/"
    );
    public static void main(String[] args) {
        com.lsl.lslaiserviceagent.crawler.AdvancedSeleniumCrawler crawler = new com.lsl.lslaiserviceagent.crawler.AdvancedSeleniumCrawler();
        try {
            // 1. 初始化edge浏览器
            crawler.initDriver(EDGE_BROWER);

            // 2. 爬取电信官网
            List<CrawlResult> crawlResults = new ArrayList<>();
            for (String url : URLS2CRAW) {
                CrawlResult result = crawler.crawl(url);
                if (result.isSuccess()) {
                    // 3. 打印基本信息
                    log.info("\n📊 网站{}爬取统计:",url);
                    log.info("   页面标题: " + result.getTitle());
                    log.info("   最终URL: " + result.getCurrentUrl());
                    log.info("   耗时: " + result.getDurationSeconds() + "秒");

                    // 4. 保存HTML到文件
                    crawler.saveHtmlToFile(result.getHtml(), result.getTitle());

                    // 6. 执行JavaScript获取额外信息
                    Map<String, Object> jsInfo = crawler.executeJavaScript();
                    log.info("\n📜 JavaScript信息获取完成");

                    // 7. 打印HTML预览
                    log.info("\n🔍 HTML内容预览:");
                    String preview = result.getHtml().substring(0, Math.min(result.getHtml().length(), 500));
                    log.info(preview);
                    crawlResults.add(result);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 8. 关闭浏览器
            crawler.quit();
        }
    }
}