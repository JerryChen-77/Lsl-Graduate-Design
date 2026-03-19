package com.lsl.lslaiserviceagent.crawler.parser;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 中国联通HTML解析器 - 精进版
 * 解析功能：
 * 1. 支持城市列表（完整信息）
 * 2. 套餐服务（明星套餐、特色套餐分类）
 * 3. 手机专区（品牌分类、价格信息）
 * 4. 宽带专区
 * 5. 智能终端
 * 6. 选靓号服务
 * 7. 底部导航信息
 */
@Slf4j
public class ChinaUnicomParser implements Parser{
    @Override
    public void parseHtml(String filePath,String targetFilePath){
        try {
            // 解析HTML文件
            File input = new File(filePath);
            Document doc = Jsoup.parse(input, "UTF-8");

            // 创建解析结果对象
            ChinaUnicomData data = new ChinaUnicomData();
            data.pageTitle = doc.title();

            // 1. 解析运营商支持的城市
            parseSupportedCities(doc, data);

            // 2. 解析套餐服务
            parsePackages(doc, data);

            // 3. 解析手机专区
            parsePhones(doc, data);

            // 4. 解析宽带专区
            parseBroadband(doc, data);

            // 5. 解析智能终端
            parseTerminals(doc, data);

            // 6. 解析选靓号服务
            parseNiceNumbers(doc, data);

            // 7. 解析底部导航
            parseFooter(doc, data);

            // 输出解析结果
            printParsedData(data,targetFilePath);

        } catch (IOException e) {
            System.err.println("解析文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 解析支持的城市列表
     */
    private static void parseSupportedCities(Document doc, ChinaUnicomData data) {
        // 获取当前默认城市
        Element currentCity = doc.selectFirst(".menu-city .cityName");
        if (currentCity != null) {
            data.defaultCity = currentCity.text().trim();
        }

        // 获取城市下拉框
        Element cityBox = doc.selectFirst(".city-box");
        if (cityBox == null) return;

        Elements cityGroups = cityBox.select(".add-list");

        for (Element group : cityGroups) {
            Element wordRange = group.selectFirst(".word-range");
            if (wordRange == null) continue;

            String groupLetter = wordRange.text();
            List<CityInfo> citiesInGroup = new ArrayList<>();

            Elements cityLinks = group.select("ul li a");
            for (Element link : cityLinks) {
                CityInfo city = new CityInfo();
                city.name = link.text();
                city.provinceCode = link.attr("p");
                city.link = link.attr("href");

                // 提取省份代码中的数字
                if (city.provinceCode != null && !city.provinceCode.isEmpty()) {
                    try {
                        city.provinceId = Integer.parseInt(city.provinceCode);
                    } catch (NumberFormatException e) {
                        // 忽略转换错误
                    }
                }

                citiesInGroup.add(city);
                data.allCities.add(city);
            }

            data.cityGroups.put(groupLetter, citiesInGroup);
        }

        data.cityCount = data.allCities.size();
    }

    /**
     * 解析套餐服务
     */
    private static void parsePackages(Document doc, ChinaUnicomData data) {
        // 2.1 明星套餐
        Element starGoods = doc.selectFirst(".star-goods");
        if (starGoods != null) {
            Elements packageItems = starGoods.select("li");
            for (Element item : packageItems) {
                Element link = item.selectFirst("a");
                Element img = item.selectFirst("img");

                if (link != null && img != null) {
                    PackageInfo pkg = new PackageInfo();
                    pkg.name = img.attr("alt");
                    pkg.link = link.attr("href");
                    pkg.imageUrl = img.attr("src");
                    pkg.type = "明星套餐";

                    // 从名称中提取价格
                    if (pkg.name != null && pkg.name.contains("元档")) {
                        Pattern pattern = Pattern.compile("(\\d+)元档");
                        java.util.regex.Matcher matcher = pattern.matcher(pkg.name);
                        if (matcher.find()) {
                            pkg.price = matcher.group(1) + "元档";
                            pkg.priceValue = Integer.parseInt(matcher.group(1));
                        }
                    }

                    data.starPackages.add(pkg);
                    data.allPackages.add(pkg);
                }
            }
        }

        // 2.2 特色套餐
        Element secFloor = doc.selectFirst(".sec-floor .goods");
        if (secFloor != null) {
            Elements packageItems = secFloor.select("li a.cards");
            for (Element link : packageItems) {
                PackageInfo pkg = new PackageInfo();
                pkg.name = link.attr("value");
                pkg.link = link.attr("href");
                pkg.type = "特色套餐";

                // 获取套餐详情
                Element titleP = link.selectFirst(".title-p");
                Element saleP = link.selectFirst(".sale-p");
                Elements sales = link.select(".sale");

                if (titleP != null) {
                    pkg.featureName = titleP.text();
                }

                if (saleP != null) {
                    pkg.price = saleP.text();
                    // 提取价格数值
                    Pattern pattern = Pattern.compile("(\\d+)元");
                    java.util.regex.Matcher matcher = pattern.matcher(pkg.price);
                    if (matcher.find()) {
                        pkg.priceValue = Integer.parseInt(matcher.group(1));
                    }
                }

                // 获取流量和分钟数
                if (sales.size() >= 2) {
                    pkg.data = sales.get(0).text();
                    pkg.minutes = sales.get(1).text();
                }

                data.featurePackages.add(pkg);
                data.allPackages.add(pkg);
            }
        }

        // 2.3 Banner广告套餐
        Element banner = doc.selectFirst(".banner_img");
        if (banner != null) {
            Elements bannerItems = banner.select("li a");
            for (Element link : bannerItems) {
                Element img = link.selectFirst("img");
                if (img != null) {
                    PackageInfo pkg = new PackageInfo();
                    pkg.name = img.attr("alt");
                    pkg.link = link.attr("href");
                    pkg.imageUrl = img.attr("src");
                    pkg.type = "Banner广告";

                    data.bannerPackages.add(pkg);
                    data.allPackages.add(pkg);
                }
            }
        }
    }

    /**
     * 解析手机专区
     */
    private static void parsePhones(Document doc, ChinaUnicomData data) {
        // 手机一区
        Element mobileZone1 = doc.selectFirst("#359725 .mobileZone");
        if (mobileZone1 != null) {
            parseMobileZone(mobileZone1, data, "一区");
        }

        // 手机二区
        Element mobileZone2 = doc.selectFirst("#359726 .mobileZone");
        if (mobileZone2 != null) {
            parseMobileZone(mobileZone2, data, "二区");
        }

        // 手机三区
        Element mobileZone3 = doc.selectFirst("#359727 .mobileZone");
        if (mobileZone3 != null) {
            parseMobileZone(mobileZone3, data, "三区");
        }

        // 解析品牌筛选链接
        Element brandList = doc.selectFirst(".pinpai-list");
        if (brandList != null) {
            Elements brandLinks = brandList.select("a");
            for (Element link : brandLinks) {
                PhoneBrand brand = new PhoneBrand();
                brand.name = link.text();
                brand.link = link.attr("href");
                data.phoneBrands.put(brand.name, brand);
            }
        }
    }

    /**
     * 解析单个手机区域
     */
    private static void parseMobileZone(Element zone, ChinaUnicomData data, String zoneName) {
        Elements items = zone.select("li");
        for (Element item : items) {
            // 第一个li可能是区域广告图
            Element adLink = item.selectFirst("a[target]");
            if (adLink != null && adLink.selectFirst("img") != null && item.select(".goods-name").isEmpty()) {
                // 这是广告图
                Element img = adLink.selectFirst("img");
                PhoneInfo phone = new PhoneInfo();
                phone.isAd = true;
                phone.adLink = adLink.attr("href");
                phone.adImageUrl = img != null ? img.attr("src") : "";
                phone.zone = zoneName;
                data.phones.add(phone);
                continue;
            }

            // 普通手机商品
            Element link = item.selectFirst("a");
            if (link == null) continue;

            PhoneInfo phone = new PhoneInfo();
            phone.link = link.attr("href");
            phone.zone = zoneName;

            Element img = link.selectFirst(".img img");
            if (img != null) {
                phone.imageUrl = img.attr("src");
            }

            Element coin = link.selectFirst(".coin");
            if (coin != null) {
                phone.hotTag = coin.text();
                phone.isHot = true;
            }

            Element nameElem = link.selectFirst(".goods-name");
            if (nameElem != null) {
                phone.name = nameElem.text();
            }

            Element secNameElem = link.selectFirst(".sec-name");
            if (secNameElem != null) {
                phone.version = secNameElem.text();
            }

            Element priceElem = link.selectFirst(".goods-money span");
            if (priceElem != null) {
                phone.price = priceElem.text();
                try {
                    phone.priceValue = Integer.parseInt(phone.price);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }

            // 判断品牌
            if (phone.name != null) {
                if (phone.name.contains("OPPO")) {
                    phone.brand = "OPPO";
                } else if (phone.name.contains("vivo") || phone.name.contains("VIVO")) {
                    phone.brand = "vivo";
                } else if (phone.name.contains("荣耀")) {
                    phone.brand = "荣耀";
                } else if (phone.name.contains("真我")) {
                    phone.brand = "真我";
                }
            }

            data.phones.add(phone);
        }
    }

    /**
     * 解析宽带专区
     */
    private static void parseBroadband(Document doc, ChinaUnicomData data) {
        Element brdZone = doc.selectFirst("#359730 ._brd");
        if (brdZone == null) return;

        Elements broadbandItems = brdZone.select("> div");
        for (Element item : broadbandItems) {
            BroadbandInfo bb = new BroadbandInfo();

            Element nameElem = item.selectFirst(".kd-name");
            if (nameElem != null) {
                bb.name = nameElem.text();
            }

            Element descElem = item.selectFirst(".kd-f-name");
            if (descElem != null) {
                bb.description = descElem.text();
            }

            Element priceElem = item.selectFirst(".kd-money span");
            if (priceElem != null) {
                bb.price = priceElem.text();
                try {
                    bb.priceValue = Integer.parseInt(bb.price);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }

            Element btnElem = item.selectFirst(".btn");
            if (btnElem != null) {
                bb.link = btnElem.attr("href");
                bb.buttonText = btnElem.text();
            }

            data.broadbandList.add(bb);
        }
    }

    /**
     * 解析智能终端
     */
    private static void parseTerminals(Document doc, ChinaUnicomData data) {
        Element terminalZone = doc.selectFirst("#371203 ._brd");
        if (terminalZone == null) return;

        // 左侧大图
        Element leftNav = terminalZone.selectFirst(".left-nav-intel ul li");
        if (leftNav != null) {
            Element link = leftNav.selectFirst("a");
            if (link != null) {
                TerminalInfo terminal = new TerminalInfo();
                terminal.isMain = true;
                terminal.link = link.attr("href");

                Element img = link.selectFirst(".img img");
                if (img != null) {
                    terminal.imageUrl = img.attr("src");
                }

                Element nameElem = link.selectFirst(".goods-name");
                if (nameElem != null) {
                    terminal.name = nameElem.text();
                }

                Element descElem = link.selectFirst(".sec-name");
                if (descElem != null) {
                    terminal.description = descElem.text();
                }

                Element priceElem = link.selectFirst(".goods-money span");
                if (priceElem != null) {
                    terminal.price = priceElem.text();
                    try {
                        terminal.priceValue = Integer.parseInt(terminal.price);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }

                data.terminals.add(terminal);
            }
        }

        // 右侧小图列表
        Element rightNav = terminalZone.selectFirst(".right-nav");
        if (rightNav != null) {
            Elements items = rightNav.select("ul");
            for (Element item : items) {
                Element link = item.selectFirst("a");
                if (link == null) continue;

                TerminalInfo terminal = new TerminalInfo();
                terminal.isMain = false;
                terminal.link = link.attr("href");

                Element img = link.selectFirst(".img img");
                if (img != null) {
                    terminal.imageUrl = img.attr("src");
                }

                Element nameElem = link.selectFirst(".goods-name");
                if (nameElem != null) {
                    terminal.name = nameElem.text();
                }

                Element descElem = link.selectFirst(".sec-name");
                if (descElem != null) {
                    terminal.description = descElem.text();
                }

                Element priceElem = link.selectFirst(".goods-money span");
                if (priceElem != null) {
                    terminal.price = priceElem.text();
                    try {
                        terminal.priceValue = Integer.parseInt(terminal.price);
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }

                data.terminals.add(terminal);
            }
        }
    }

    /**
     * 解析选靓号服务
     */
    private static void parseNiceNumbers(Document doc, ChinaUnicomData data) {
        Element numListArea = doc.getElementById("hotNumListArea");
        if (numListArea != null) {
            String jsonText = numListArea.html();
            // 简单解析，实际项目中可使用JSON库
            // 这里简化处理，只提取信息
        }

        // 解析号码类型
        Element numTypeList = doc.selectFirst(".numType-list");
        if (numTypeList != null) {
            Elements typeItems = numTypeList.select("li a");
            for (Element item : typeItems) {
                NiceNumberType type = new NiceNumberType();
                type.name = item.text();
                type.groupId = item.attr("c");
                data.niceNumberTypes.add(type);
            }
        }

        // 解析靓号列表
        Element numDataBroad = doc.getElementById("numDataBroad");
        if (numDataBroad != null) {
            Elements numberItems = numDataBroad.select("li");
            int count = 0;
            for (Element item : numberItems) {
                if (count++ >= 10) break; // 只取前10个示例

                Element link = item.selectFirst("a");
                if (link == null) continue;

                NiceNumberInfo num = new NiceNumberInfo();

                Element numberSpan = link.selectFirst(".num-span.number");
                if (numberSpan != null) {
                    num.number = numberSpan.text().replaceAll("\\s+", "");
                }

                // 获取属性
                num.goodsId = item.attr("gi");
                num.serialNumber = item.attr("a");
                num.advanceLimit = item.attr("b");
                num.cityCode = item.attr("f");
                num.groupKey = item.attr("k");
                num.link = item.attr("url");

                // 获取价格信息
                Elements priceLabels = link.select(".num-mes label");
                if (priceLabels.size() >= 2) {
                    num.advancePrice = priceLabels.get(0).text();
                    num.monthFee = priceLabels.get(1).text();
                }

                data.niceNumbers.add(num);
            }
        }
    }

    /**
     * 解析底部导航
     */
    private static void parseFooter(Document doc, ChinaUnicomData data) {
        Element footer = doc.selectFirst(".wt-header-footer-box");
        if (footer == null) return;

        // 解析版权信息
        Element trademark = footer.selectFirst(".wt-header-trademark");
        if (trademark != null) {
            data.copyright = trademark.text();
        }

        // 解析经营许可证
        Element license = footer.selectFirst(".wt-header-license");
        if (license != null) {
            data.license = license.text();
        }

        // 解析底部导航链接
        Elements navDetails = footer.select(".wt-header-footer-nav-details");
        for (Element detail : navDetails) {
            Elements links = detail.select(".wt-header-footer-nav-content span");
            for (Element link : links) {
                String text = link.text().trim();
                if (!text.isEmpty() && !text.equals("购物指南") && !text.equals("支付方式")
                        && !text.equals("配送方式") && !text.equals("售后服务") && !text.equals("其他服务")) {
                    data.footerLinks.add(text);
                }
            }
        }

        // 解析企业信息
        Element hotline = footer.selectFirst(".wt-header-hotline");
        if (hotline != null) {
            Elements spans = hotline.select("span");
            for (Element span : spans) {
                String text = span.text().trim();
                if (text.contains("京ICP备")) {
                    data.icp = text;
                } else if (text.contains("京公网安备")) {
                    data.policeRecord = text;
                }
            }
        }
    }


    /**
     * 打印中国联通解析结果到文件
     * @param data 中国联通解析数据
     * @param targetFilePath 目标文件路径
     */
    private static void printParsedData(ChinaUnicomData data, String targetFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFilePath))) {

            // 1. 城市信息
            writer.write("\n【1. 支持城市】");
            writer.newLine();
            writer.write("  当前默认城市: " + data.defaultCity);
            writer.newLine();
            writer.write("  支持城市总数: " + data.cityCount + "个");
            writer.newLine();
            writer.newLine();
            writer.write("  按字母分组:");
            writer.newLine();

            for (Map.Entry<String, List<CityInfo>> entry : data.cityGroups.entrySet()) {
                writer.write("  " + entry.getKey() + " (" + entry.getValue().size() + "个):");
                writer.newLine();
                StringBuilder sb = new StringBuilder("    ");
                int count = 0;
                for (CityInfo city : entry.getValue()) {
                    sb.append(city.name).append(" ");
                    count++;
                    if (count % 8 == 0) {
                        writer.write(sb.toString());
                        writer.newLine();
                        sb = new StringBuilder("    ");
                    }
                }
                if (sb.length() > 4) {
                    writer.write(sb.toString());
                    writer.newLine();
                }
            }

            // 2. 套餐信息
            writer.write("\n【2. 套餐服务】");
            writer.newLine();
            writer.write("  明星套餐: " + data.starPackages.size() + "个");
            writer.newLine();
            writer.write("  特色套餐: " + data.featurePackages.size() + "个");
            writer.newLine();
            writer.write("  Banner广告: " + data.bannerPackages.size() + "个");
            writer.newLine();

            if (!data.starPackages.isEmpty()) {
                writer.write("\n  5G套餐系列:");
                writer.newLine();
                for (PackageInfo pkg : data.starPackages) {
                    writer.write("    - " + pkg.name + (pkg.price != null ? " (" + pkg.price + ")" : ""));
                    writer.newLine();
                }
            }

            if (!data.featurePackages.isEmpty()) {
                writer.write("\n  特色套餐:");
                writer.newLine();
                for (PackageInfo pkg : data.featurePackages) {
                    writer.write("    - " + pkg.name);
                    writer.newLine();
                    writer.write("      " + pkg.price + ", " + pkg.data + ", " + pkg.minutes);
                    writer.newLine();
                }
            }

            // 3. 手机专区
            writer.write("\n【3. 手机专区】");
            writer.newLine();
            writer.write("  共 " + data.phones.size() + " 款手机");
            writer.newLine();

            // 按品牌统计
            Map<String, Integer> brandStats = new HashMap<>();
            for (PhoneInfo phone : data.phones) {
                if (phone.brand != null) {
                    brandStats.put(phone.brand, brandStats.getOrDefault(phone.brand, 0) + 1);
                }
            }

            writer.write("  品牌分布:");
            writer.newLine();
            for (Map.Entry<String, Integer> entry : brandStats.entrySet()) {
                writer.write("    - " + entry.getKey() + ": " + entry.getValue() + "款");
                writer.newLine();
            }

            writer.write("\n  热销手机推荐:");
            writer.newLine();
            int hotCount = 0;
            for (PhoneInfo phone : data.phones) {
                if (phone.isHot && phone.name != null && hotCount < 5) {
                    writer.write("    - " + phone.name + " " + phone.version + " ¥" + phone.price + " " + phone.hotTag);
                    writer.newLine();
                    hotCount++;
                }
            }

            // 4. 宽带专区
            writer.write("\n【4. 宽带专区】");
            writer.newLine();
            for (BroadbandInfo bb : data.broadbandList) {
                writer.write("  ■ " + bb.name);
                writer.newLine();
                writer.write("    描述: " + bb.description);
                writer.newLine();
                writer.write("    价格: ¥" + bb.price);
                writer.newLine();
                writer.write("    办理: " + bb.buttonText);
                writer.newLine();
            }

            // 5. 智能终端
            writer.write("\n【5. 智能终端】");
            writer.newLine();
            writer.write("  共 " + data.terminals.size() + " 款智能设备");
            writer.newLine();

            writer.write("\n  热门设备:");
            writer.newLine();
            int termCount = 0;
            for (TerminalInfo term : data.terminals) {
                if (term.name != null && termCount < 8) {
                    writer.write("    - " + term.name + " ¥" + term.price);
                    writer.newLine();
                    writer.write("      " + term.description);
                    writer.newLine();
                    termCount++;
                }
            }

            // 6. 选靓号
            writer.write("\n【6. 选靓号服务】");
            writer.newLine();
            writer.write("  号码类型: " + data.niceNumberTypes.size() + "种");
            writer.newLine();
            for (NiceNumberType type : data.niceNumberTypes) {
                writer.write("    - " + type.name);
                writer.newLine();
            }

            writer.write("\n  靓号示例:");
            writer.newLine();
            for (NiceNumberInfo num : data.niceNumbers) {
                writer.write("    - " + num.number + " 预存" + num.advancePrice + "元 月承诺" + num.monthFee + "元");
                writer.newLine();
            }

            // 7. 底部信息
            writer.write("\n【7. 网站信息】");
            writer.newLine();
            writer.write("  " + data.copyright);
            writer.newLine();
            writer.write("  " + data.license);
            writer.newLine();
            writer.write("  " + data.icp);
            writer.newLine();
            writer.write("  " + data.policeRecord);
            writer.newLine();

            writer.write("\n  底部导航服务:");
            writer.newLine();
            writer.write("    ");
            int linkCount = 0;
            for (String link : data.footerLinks) {
                writer.write(link + "  ");
                linkCount++;
                if (linkCount % 6 == 0) {
                    writer.newLine();
                    writer.write("    ");
                }
            }

            writer.newLine();
            writer.newLine();
            writer.write("==========================================");
            writer.newLine();

            System.out.println("中国联通解析结果已成功写入文件: " + targetFilePath);

        } catch (IOException e) {
            System.err.println("写入文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 数据类定义 ====================

    static class ChinaUnicomData {
        String pageTitle;
        String defaultCity = "北京";
        int cityCount = 0;
        List<CityInfo> allCities = new ArrayList<>();
        Map<String, List<CityInfo>> cityGroups = new HashMap<>();
        List<PackageInfo> starPackages = new ArrayList<>();
        List<PackageInfo> featurePackages = new ArrayList<>();
        List<PackageInfo> bannerPackages = new ArrayList<>();
        List<PackageInfo> allPackages = new ArrayList<>();
        List<PhoneInfo> phones = new ArrayList<>();
        Map<String, PhoneBrand> phoneBrands = new HashMap<>();
        List<BroadbandInfo> broadbandList = new ArrayList<>();
        List<TerminalInfo> terminals = new ArrayList<>();
        List<NiceNumberType> niceNumberTypes = new ArrayList<>();
        List<NiceNumberInfo> niceNumbers = new ArrayList<>();
        String copyright = "";
        String license = "";
        String icp = "";
        String policeRecord = "";
        List<String> footerLinks = new ArrayList<>();
    }

    static class CityInfo {
        String name;
        String provinceCode;
        int provinceId;
        String link;
    }

    static class PackageInfo {
        String name;
        String link;
        String imageUrl;
        String type; // 明星套餐、特色套餐、Banner广告
        String price;
        int priceValue;
        String featureName; // 特色套餐名称
        String data; // 流量
        String minutes; // 分钟数
    }

    static class PhoneInfo {
        String name;
        String brand;
        String version;
        String price;
        int priceValue;
        String link;
        String imageUrl;
        String zone; // 一区、二区、三区
        boolean isHot = false;
        String hotTag;
        boolean isAd = false;
        String adLink;
        String adImageUrl;
    }

    static class PhoneBrand {
        String name;
        String link;
    }

    static class BroadbandInfo {
        String name;
        String description;
        String price;
        int priceValue;
        String link;
        String buttonText;
    }

    static class TerminalInfo {
        String name;
        String description;
        String price;
        int priceValue;
        String link;
        String imageUrl;
        boolean isMain; // 是否为主推大图
    }

    static class NiceNumberType {
        String name;
        String groupId;
    }

    static class NiceNumberInfo {
        String number;
        String goodsId;
        String serialNumber;
        String advanceLimit;
        String cityCode;
        String groupKey;
        String link;
        String advancePrice;
        String monthFee;
    }
}