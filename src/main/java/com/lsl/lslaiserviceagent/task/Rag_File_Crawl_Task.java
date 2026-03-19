package com.lsl.lslaiserviceagent.task;

import com.lsl.lslaiserviceagent.crawler.AdvancedSeleniumCrawler;
import com.lsl.lslaiserviceagent.crawler.model.CrawlResult;
import com.lsl.lslaiserviceagent.crawler.parser.Parser;
import com.lsl.lslaiserviceagent.crawler.parser.ParserFactory;
import com.lsl.lslaiserviceagent.model.enums.OperatorEnum;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.lsl.lslaiserviceagent.constant.RagConstant.*;

@Slf4j
@Component
public class Rag_File_Crawl_Task {

    private static final String EDGE_BROWER = "edge";
    
    private static final List<String> URLS2CRAW = OperatorEnum.getOperatorUrls();
    /**
     * TODO 当前一分钟执行一次，为了方便测试
     */
    @PostConstruct
    public void executeCrawlTask() {
        AdvancedSeleniumCrawler crawler = new AdvancedSeleniumCrawler();
        try {
            // 1. 初始化edge浏览器
            crawler.initDriver(EDGE_BROWER);
            // 2. 爬取电信官网,获取HTML页面
            for (String url : URLS2CRAW) {
                Parser parser = null;
                String htmlFileUrl = null;
                OperatorEnum operator = null;
                if(StringUtils.isNotBlank(url)){
                    CrawlResult result = crawler.crawl(url);
                    if (result.isSuccess()) {
                        // 3. 打印基本信息
                        log.info("\n📊 网站{}爬取统计:",url);
                        log.info("   最终URL: " + result.getCurrentUrl());
                        log.info("   耗时: " + result.getDurationSeconds() + "秒");
                        // 4. 保存HTML到文件
                        htmlFileUrl = crawler.saveHtmlToFile(result.getHtml(), result.getTitle());
                    }
                }else{
                    operator = OperatorEnum.getOperatorEnumByUrl(url);
                    htmlFileUrl = RAG_HTML_DIRECTORY_PATH + File.separator + operator.getName() + HTML_TAIL;
                }
                String targetFileUrl = RAG_TXT_DIRECTORY_PATH + File.separator + operator.getName() + TXT_TAIL;
                parser = ParserFactory.getParser(operator);
                parser.parseHtml(htmlFileUrl,targetFileUrl);
            }
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 8. 关闭浏览器
            crawler.quit();
        }
    }

}
