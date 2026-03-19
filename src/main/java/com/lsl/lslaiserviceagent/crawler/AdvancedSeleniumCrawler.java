package com.lsl.lslaiserviceagent.crawler;

import com.lsl.lslaiserviceagent.crawler.model.CrawlResult;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static com.lsl.lslaiserviceagent.constant.RagConstant.HTML_TAIL;
import static com.lsl.lslaiserviceagent.constant.RagConstant.RAG_HTML_DIRECTORY_PATH;

/**
 * 完整的Selenium爬虫工具类
 */
@Slf4j
public class AdvancedSeleniumCrawler {
    
    private WebDriver driver;
    private WebDriverWait wait;

    /**
     * 初始化驱动（可选择浏览器类型）
     * @param browserType 浏览器类型：chrome, edge, firefox
     */
    public void initDriver(String browserType) {
        // 根据浏览器类型选择驱动
        switch (browserType.toLowerCase()) {
            case "chrome":
                initChromeDriver();
                break;
            case "edge":
                initEdgeDriver();
                break;
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                // 需要添加FirefoxOptions
                break;
            default:
                initEdgeDriver();
        }

        // 设置等待
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(60));
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // 注入JavaScript来隐藏自动化特征
        hideAutomationFeatures();

        System.out.println("✅ WebDriver初始化完成，浏览器: " + browserType);
    }

    private void initChromeDriver(){
        ChromeOptions options = new ChromeOptions();

        // 基础配置
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=zh-CN");

        // 反反爬虫配置
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        // 设置用户代理
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
        options.addArguments("user-agent=" + userAgent);

        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver(options);
    }

    private void initEdgeDriver() {
        System.out.println("正在初始化Edge浏览器...");

        WebDriverManager.edgedriver().clearDriverCache().clearResolutionCache();
        System.setProperty("webdriver.edge.driver", "D:\\edgeDriver\\msedgedriver.exe");

        EdgeOptions options = new EdgeOptions();

        // ========== 基础配置 ==========
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=zh-CN");
        options.addArguments("--disable-notifications");

        // ========== 增强反爬虫配置 ==========
        // 禁用自动化控制特征
        options.addArguments("--disable-blink-features=AutomationControlled");

        // 排除自动化开关
        options.setExperimentalOption("excludeSwitches", new String[]{
                "enable-automation",
                "enable-logging"
        });

        // 禁用自动化扩展
        options.setExperimentalOption("useAutomationExtension", false);

        // 添加额外的排除开关
        options.setExperimentalOption("excludeSwitches", new String[]{
                "enable-automation",
                "test-type",
                "ignore-certificate-errors"
        });

        // 设置真实的用户代理
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
        options.addArguments("user-agent=" + userAgent);

        // 添加更多真实浏览器的参数
        options.addArguments("--disable-features=TranslateUI");
        options.addArguments("--disable-ipc-flooding-protection");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--force-color-profile=srgb");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--metrics-recording-only");
        options.addArguments("--mute-audio");
        options.addArguments("--no-first-run");
        options.addArguments("--no-pings");
        options.addArguments("--password-store=basic");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--safebrowsing-disable-auto-update");
        options.addArguments("--use-mock-keychain");

        // ========== 创建EdgeDriver实例 ==========
        driver = new EdgeDriver(options);

        // 等待浏览器稳定
        sleep(3000);

        // 执行多重反检测脚本
        executeAntiDetectionScripts();
    }

    /**
     * 执行反监测脚本
     */
    private void executeAntiDetectionScripts() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 1. 首先导航到空白页执行脚本
            driver.get("about:blank");
            sleep(1000);

            // 2. 综合反检测脚本
            String script =
                    "try {" +
                            "  // 修改webdriver属性" +
                            "  const proto = Object.getPrototypeOf(navigator);" +
                            "  Object.defineProperty(proto, 'webdriver', {get: () => undefined});" +
                            "  " +
                            "  // 修改plugins" +
                            "  Object.defineProperty(proto, 'plugins', {get: () => [" +
                            "    {0: {type: 'application/x-google-chrome-pdf'}, description: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer'}" +
                            "  ]});" +
                            "  " +
                            "  // 修改languages" +
                            "  Object.defineProperty(proto, 'languages', {get: () => ['zh-CN', 'zh']});" +
                            "  " +
                            "  // 添加chrome对象" +
                            "  window.chrome = {" +
                            "    runtime: {}," +
                            "    loadTimes: function() {}," +
                            "    csi: function() {}," +
                            "    app: {}" +
                            "  };" +
                            "  " +
                            "  // 修改connection" +
                            "  Object.defineProperty(proto, 'connection', {" +
                            "    get: () => ({effectiveType: '4g', rtt: 50, downlink: 10})" +
                            "  });" +
                            "  " +
                            "  // 修改platform" +
                            "  Object.defineProperty(proto, 'platform', {get: () => 'Win32'});" +
                            "  " +
                            "  // 修改deviceMemory" +
                            "  Object.defineProperty(proto, 'deviceMemory', {get: () => 8});" +
                            "  " +
                            "  // 修改hardwareConcurrency" +
                            "  Object.defineProperty(proto, 'hardwareConcurrency', {get: () => 8});" +
                            "  " +
                            "  // 修改Permissions" +
                            "  if (window.Permissions && window.Permissions.prototype) {" +
                            "    const originalQuery = window.Permissions.prototype.query;" +
                            "    window.Permissions.prototype.query = function(permissionDesc) {" +
                            "      return Promise.resolve({state: 'prompt'});" +
                            "    };" +
                            "  }" +
                            "  " +
                            "  console.log('反检测脚本执行成功');" +
                            "} catch(e) {" +
                            "  console.error('反检测脚本错误:', e);" +
                            "}";

            js.executeScript(script);
            System.out.println("✅ 反检测脚本执行完成");

        } catch (Exception e) {
            System.err.println("反检测脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 隐藏自动化特征
     */
    private void hideAutomationFeatures() {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // 确保页面已加载
            if (driver.getCurrentUrl() == null || driver.getCurrentUrl().isEmpty() ||
                    driver.getCurrentUrl().equals("about:blank")) {
                driver.get("about:blank");
                Thread.sleep(1000);
            }

            // 通过原型链修改属性
            String script =
                    "try {" +
                            "  const proto = Object.getPrototypeOf(navigator);" +
                            "  Object.defineProperty(proto, 'webdriver', {" +
                            "    get: () => undefined," +
                            "    configurable: true" +
                            "  });" +
                            "  console.log('webdriver隐藏成功');" +
                            "  return true;" +
                            "} catch(e) {" +
                            "  console.error('隐藏失败:', e);" +
                            "  return false;" +
                            "}";

            Boolean result = (Boolean) js.executeScript(script);

            if (result != null && result) {
                System.out.println("✅ 自动化特征隐藏成功");
            } else {
                System.out.println("⚠️ 自动化特征隐藏可能失败");
            }

        } catch (Exception e) {
            System.err.println("❌ 隐藏自动化特征时发生异常: " + e.getMessage());
        }
    }


    /**
     * 爬取网页
     * @param url
     * @return
     */
    public CrawlResult crawl(String url) {
        CrawlResult result = new CrawlResult();
        result.setUrl(url);
        result.setStartTime(LocalDateTime.now());

        try {
            System.out.println("🌐 正在访问: " + url);

            // 模拟真实用户行为
            simulateHumanBehavior();

            // 导航到URL
            driver.get(url);

            // 随机等待，模拟人类阅读时间
            int waitTime = 3000 + new Random().nextInt(5000);
            Thread.sleep(waitTime);

            // 模拟鼠标移动（如果有元素）
            try {
                WebElement body = driver.findElement(By.tagName("body"));
                if (body != null) {
                    Actions actions = new Actions(driver);
                    actions.moveByOffset(10, 10).perform();
                    Thread.sleep(500);
                    actions.moveByOffset(100, 50).perform();
                }
            } catch (Exception e) {
                // 忽略移动失败
            }

            // 模拟滚动
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, 300);");
            Thread.sleep(1000);
            js.executeScript("window.scrollTo(0, 600);");
            Thread.sleep(1000);
            js.executeScript("window.scrollTo(0, 0);");

            // 等待页面完全加载
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));

            // 额外等待动态内容
            Thread.sleep(2000);

            // 尝试关闭可能的弹窗
            handlePopups();

            // 获取页面信息
            result.setTitle(driver.getTitle());
            result.setCurrentUrl(driver.getCurrentUrl());
            result.setHtml(driver.getPageSource());
            result.setCookies(driver.manage().getCookies());
            result.setSuccess(true);

            System.out.println("✅ 页面获取成功");
            System.out.println("   标题: " + result.getTitle());
            System.out.println("   HTML长度: " + result.getHtml().length());

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            System.err.println("❌ 爬取失败: " + e.getMessage());
        }

        result.setEndTime(LocalDateTime.now());
        return result;
    }

    /**
     * 模拟人类行为
     */
    private void simulateHumanBehavior() {
        try {
            // 随机延迟，模拟人类思考
            Thread.sleep(new Random().nextInt(2000) + 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 尝试关闭常见弹窗
     */
    private void handlePopups() {
        try {
            // 常见的关闭按钮选择器
            String[] closeSelectors = {
                "//button[contains(text(),'关闭')]",
                "//button[contains(@class,'close')]",
                "//span[contains(@class,'close')]",
                "//div[contains(@class,'popup')]//button",
                "//i[contains(@class,'icon-close')]"
            };
            
            for (String selector : closeSelectors) {
                try {
                    WebElement closeBtn = driver.findElement(By.xpath(selector));
                    if (closeBtn.isDisplayed()) {
                        closeBtn.click();
                        System.out.println("已关闭弹窗");
                        Thread.sleep(1000);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // 忽略弹窗处理异常
        }
    }
    
    /**
     * 执行JavaScript获取额外信息
     */
    public Map<String, Object> executeJavaScript() {
        Map<String, Object> jsInfo = new HashMap<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            
            // 获取页面性能数据
            jsInfo.put("performance", js.executeScript(
                "return window.performance.timing;"));
            
            // 获取所有cookie
            jsInfo.put("documentCookies", js.executeScript(
                "return document.cookie;"));
            
            // 获取localStorage
            jsInfo.put("localStorage", js.executeScript(
                "var items = {}; for (var i = 0; i < localStorage.length; i++) { items[localStorage.key(i)] = localStorage.getItem(localStorage.key(i)); } return items;"));
            
        } catch (Exception e) {
            System.err.println("JavaScript执行失败: " + e.getMessage());
        }
        return jsInfo;
    }

    /**
     * 保存HTML到文件
     * @param html HTML内容
     * @param filename 文件名（如 "page.html"）
     */
    public String saveHtmlToFile(String html, String filename) {
        // 确保目录存在
        File directory = new File(RAG_HTML_DIRECTORY_PATH);
        if (!directory.exists()) {
            directory.mkdirs(); // 创建目录（包括父目录）
        }

        // 构建完整的文件路径
        String fullPath = RAG_HTML_DIRECTORY_PATH + File.separator + filename + HTML_TAIL;

        try (FileWriter writer = new FileWriter(fullPath)) {
            writer.write(html);
            log.info("💾 HTML已保存到: " + fullPath);
        } catch (IOException e) {
            log.error("保存文件失败: " + e.getMessage());
        }
        return fullPath;
    }

    
    /**
     * 关闭驱动
     */
    public void quit() {
        if (driver != null) {
            driver.quit();
            System.out.println("🔚 浏览器已关闭");
        }
    }


    /**
     * 线程睡眠工具方法
     * @param millis 睡眠时间（毫秒）
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("睡眠被中断: " + e.getMessage());
        }
    }

}