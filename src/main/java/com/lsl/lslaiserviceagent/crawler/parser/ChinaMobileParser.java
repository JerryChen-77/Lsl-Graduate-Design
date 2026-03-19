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

/**
 * 中国移动HTML解析器 - 精进版
 * 解析功能：
 * 1. 支持城市列表（完整信息）
 * 2. 套餐服务（分类提取）
 * 3. 品牌介绍（详细描述）
 * 4. 便捷服务入口
 * 5. 新闻动态（分类）
 * 6. 底部导航信息
 */
@Slf4j
public class ChinaMobileParser implements Parser{

    @Override
    public void parseHtml(String filePath,String targetPath) {
        try {
            log.info("==========================================");
            log.info("    中国移动官方网站 - 精进解析器输出");
            log.info("==========================================\n");

            // 解析HTML文件
            File input = new File(filePath);
            Document doc = Jsoup.parse(input, "UTF-8");

            // 创建解析结果对象
            ChinaMobileData data = new ChinaMobileData();

            // 1. 解析运营商支持的城市
            parseSupportedCities(doc, data);

            // 2. 解析套餐服务
            parsePackages(doc, data);

            // 3. 解析品牌介绍
            parseBrands(doc, data);

            // 4. 解析便捷服务
            parseServices(doc, data);

            // 5. 解析新闻动态
            parseNews(doc, data);

            // 6. 解析底部导航
            parseFooter(doc, data);

            // 输出解析结果
            printParsedData(data,targetPath);

        } catch (IOException e) {
            System.err.println("解析文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 解析支持的城市列表
     */
    private static void parseSupportedCities(Document doc, ChinaMobileData data) {
        // 获取当前默认城市
        Element currentCity = doc.selectFirst(".topcity");
        if (currentCity != null) {
            data.defaultCity = currentCity.text().trim();
        }

        // 获取城市列表容器
        Element cityList = doc.getElementById("DivCity");
        if (cityList != null) {
            Elements cityGroups = cityList.select("dl");

            for (Element group : cityGroups) {
                // 获取分组字母
                Element dt = group.selectFirst("dt");
                if (dt == null) continue;

                String groupLetter = dt.text().replace("组", "").trim();
                List<CityInfo> citiesInGroup = new ArrayList<>();

                // 解析该分组下的所有城市
                Elements cityLinks = group.select("dd a");
                for (Element link : cityLinks) {
                    CityInfo city = new CityInfo();
                    city.name = link.text().replace(">", "").trim();
                    city.provinceId = link.attr("prov_id");
                    city.provinceAbbr = link.attr("prov_abbr");
                    city.fullName = city.name; // 名称已经是完整名称

                    citiesInGroup.add(city);
                    data.allCities.add(city);
                }

                data.cityGroups.put(groupLetter, citiesInGroup);
            }
        }

        // 统计信息
        data.cityCount = data.allCities.size();
    }

    /**
     * 解析套餐服务
     */
    private static void parsePackages(Document doc, ChinaMobileData data) {
        // 2.1 业务推荐区套餐
        Element businessArea = doc.getElementById("bussinessArea");
        if (businessArea != null) {
            // 解析大图广告套餐
            Elements adBoxes = businessArea.select(".business_box03");
            for (Element box : adBoxes) {
                Element link = box.selectFirst("a");
                Element img = box.selectFirst("img");

                if (link != null && img != null) {
                    PackageInfo pkg = new PackageInfo();
                    pkg.name = img.attr("alt");
                    pkg.description = "宽带预约";
                    pkg.link = link.attr("href");
                    pkg.imageUrl = img.attr("src");
                    pkg.category = "宽带业务";
                    pkg.source = "业务推荐区";

                    // 从链接判断业务类型
                    if (pkg.link.contains("broadband")) {
                        pkg.tags.add("宽带");
                        pkg.tags.add("预约");
                    }

                    data.packages.add(pkg);
                }
            }

            // 解析业务盒子（6个小图标业务）
            Elements businessBoxes = businessArea.select(".business_box01 .yewu_box");
            for (Element box : businessBoxes) {
                Element link = box.selectFirst("a");
                if (link == null) continue;

                PackageInfo pkg = new PackageInfo();
                pkg.link = link.attr("href");
                pkg.source = "业务推荐区";

                // 获取标题
                Element title = box.selectFirst("h3");
                if (title != null) {
                    pkg.name = title.text();
                }

                // 获取描述
                Element desc = box.selectFirst(".ywbm_txt");
                if (desc != null) {
                    pkg.description = desc.text();
                }

                // 获取图片
                Element img = box.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("src");
                    if (pkg.name == null || pkg.name.isEmpty()) {
                        pkg.name = img.attr("alt");
                    }
                }

                // 根据名称分类和打标签
                if (pkg.name != null) {
                    if (pkg.name.contains("5G")) {
                        pkg.category = "5G套餐";
                        pkg.tags.add("5G");
                        pkg.tags.add("高速");
                    } else if (pkg.name.contains("号卡")) {
                        pkg.category = "号卡业务";
                        pkg.tags.add("选号");
                        pkg.tags.add("邮寄");
                    } else if (pkg.name.contains("资费")) {
                        pkg.category = "资费查询";
                        pkg.tags.add("资费");
                        pkg.tags.add("透明");
                    } else if (pkg.name.contains("流量")) {
                        pkg.category = "流量套餐";
                        pkg.tags.add("流量");
                        pkg.tags.add("特惠");
                    } else if (pkg.name.contains("权益")) {
                        pkg.category = "会员权益";
                        pkg.tags.add("权益");
                        pkg.tags.add("福利");
                    } else if (pkg.name.contains("国际") || pkg.name.contains("港澳台")) {
                        pkg.category = "国际业务";
                        pkg.tags.add("国际漫游");
                        pkg.tags.add("出境");
                    }
                }

                data.packages.add(pkg);
            }
        }

        // 2.2 服务入口区（顶部服务）
        Element serviceArea = doc.getElementById("serviceRkArea");
        if (serviceArea != null) {
            Elements serviceBoxes = serviceArea.select(".service_box");
            for (Element box : serviceBoxes) {
                Element link = box.selectFirst("a");
                Element img = box.selectFirst("img");
                Element p = box.selectFirst("p");

                if (link != null && img != null) {
                    PackageInfo pkg = new PackageInfo();
                    pkg.name = img.attr("alt");
                    pkg.link = link.attr("href");
                    pkg.imageUrl = img.attr("src");
                    pkg.category = "便捷服务";
                    pkg.source = "服务入口区";

                    if (p != null) {
                        pkg.description = p.text();
                    }

                    // 打标签
                    if (pkg.name.contains("账单")) {
                        pkg.tags.add("账单查询");
                    } else if (pkg.name.contains("发票")) {
                        pkg.tags.add("电子发票");
                    } else if (pkg.name.contains("充值")) {
                        pkg.tags.add("话费充值");
                    } else if (pkg.name.contains("5G")) {
                        pkg.tags.add("5G专区");
                    }

                    data.packages.add(pkg);
                }
            }
        }
    }

    /**
     * 解析品牌介绍
     */
    private static void parseBrands(Document doc, ChinaMobileData data) {
        Element brandArea = doc.getElementById("brandArea");
        if (brandArea == null) return;

        Elements brands = brandArea.select(".brand_box");
        for (Element brand : brands) {
            BrandInfo brandInfo = new BrandInfo();

            Element h3 = brand.selectFirst("h3");
            Element span = brand.selectFirst("span");
            Element p = brand.selectFirst("p font");
            Element img = brand.selectFirst("img");

            if (h3 != null) {
                brandInfo.name = h3.text();
            }

            if (span != null) {
                brandInfo.slogan = span.text();
            }

            if (p != null) {
                brandInfo.description = p.attr("title");
                if (brandInfo.description == null || brandInfo.description.isEmpty()) {
                    brandInfo.description = p.text();
                }
            }

            if (img != null) {
                brandInfo.imageUrl = img.attr("src");
                brandInfo.imageAlt = img.attr("alt");
            }

            // 根据品牌名称设置详细描述
            switch (brandInfo.name) {
                case "全球通":
                    brandInfo.targetAudience = "高端商务人士";
                    brandInfo.coreValues = "高质量服务、高标准品质";
                    brandInfo.features = new String[]{"尊享服务", "公益活动", "健康主题", "文化活动"};
                    brandInfo.detailedDescription = "“全球通”是中国移动的旗舰客户品牌。自创立以来，全球通始终坚持用高质量服务、高标准品质赢得客户信赖。伴随移动业务发展，全球通持续践行“我能”的品牌理念，不仅以尊享服务继承延续着优质可靠的品牌形象，更积极开展公益、健康、文化等主题活动，丰富“创新、进取、品位”的品牌新内涵。";
                    break;
                case "动感地带":
                    brandInfo.targetAudience = "年轻人";
                    brandInfo.coreValues = "智潮感";
                    brandInfo.features = new String[]{"元宇宙布局", "数智代言人橙络络", "萌卡", "潮玩卡", "音乐圈层", "街舞圈层"};
                    brandInfo.detailedDescription = "“动感地带”是中国移动面向年轻人打造的“通信潮牌”。焕新以来，围绕“智潮感”，动感地带布局元宇宙，推出“数智代言人橙络络”，结合5G打造萌卡、潮玩卡等产品，持续开展音乐、街舞等圈层活动，建立“社交媒体矩阵”快速深入年轻群体。";
                    break;
                case "神州行":
                    brandInfo.targetAudience = "银发族、快递外卖员等垂类市场";
                    brandInfo.coreValues = "回馈、关怀、自豪";
                    brandInfo.features = new String[]{"欢孝卡", "骑士卡"};
                    brandInfo.detailedDescription = "“神州行”是中国移动面向特定垂类市场的客户品牌。全新升级后，面向银发族和快递外卖员，推出欢孝卡和骑士卡，彰显“回馈、关怀、自豪”的品牌内涵，聚焦客户需求，提升客户获得感。神州行还将面向更多垂类用户，推出更加丰富的卡品和服务。";
                    break;
            }

            data.brands.add(brandInfo);
        }
    }

    /**
     * 解析便捷服务
     */
    private static void parseServices(Document doc, ChinaMobileData data) {
        // 从底部导航解析便捷服务
        Element footerNav = doc.getElementById("footerNavArea");
        if (footerNav == null) return;

        Elements bmLists = footerNav.select(".bm_list");
        for (Element list : bmLists) {
            Element h3 = list.selectFirst("h3");
            if (h3 == null) continue;

            String category = h3.text();
            Elements links = list.select("a");

            for (Element link : links) {
                ServiceInfo service = new ServiceInfo();
                service.name = link.text();
                service.link = link.attr("href");
                service.category = category;
                service.source = "底部导航";

                // 根据链接判断是否需要登录
                if (service.link.contains("i/?f=") || service.link.contains("login")) {
                    service.needLogin = true;
                }

                // 根据名称分类
                if (category.equals("便捷服务")) {
                    if (service.name.contains("账单")) {
                        service.tags.add("查询");
                    } else if (service.name.contains("详单")) {
                        service.tags.add("详单");
                        service.tags.add("查询");
                    } else if (service.name.contains("套餐余量")) {
                        service.tags.add("套餐");
                        service.tags.add("余量");
                    }
                } else if (category.equals("产品推荐")) {
                    service.tags.add("推荐产品");
                }

                data.services.add(service);
            }
        }

        // 从右侧导航解析服务
        Element rightNav = doc.getElementById("rightNav");
        if (rightNav != null) {
            Elements rightLinks = rightNav.select(".lf_fix2 li a.wz");
            for (Element link : rightLinks) {
                ServiceInfo service = new ServiceInfo();
                service.name = link.text();
                service.link = link.attr("href");
                service.category = "右侧导航";
                service.source = "右侧悬浮";

                if (service.name.contains("咨询")) {
                    service.tags.add("在线咨询");
                } else if (service.name.contains("热线")) {
                    service.tags.add("客服热线");
                } else if (service.name.contains("投诉")) {
                    service.tags.add("投诉建议");
                }

                data.services.add(service);
            }
        }
    }

    /**
     * 解析新闻动态
     */
    private static void parseNews(Document doc, ChinaMobileData data) {
        // 移动要闻
        Element newsArea = doc.getElementById("jtNewsArea");
        if (newsArea != null) {
            Elements newsItems = newsArea.select(".news-list li");
            for (Element item : newsItems) {
                Element link = item.selectFirst("a");
                Element titleSpan = item.selectFirst(".news_tt01");
                Element dateSpan = item.selectFirst(".news_tt02");

                if (link != null && titleSpan != null) {
                    NewsInfo news = new NewsInfo();
                    news.title = titleSpan.attr("title");
                    if (news.title == null || news.title.isEmpty()) {
                        news.title = titleSpan.text();
                    }
                    news.link = link.attr("href");
                    news.category = "移动要闻";

                    if (dateSpan != null) {
                        news.publishDate = dateSpan.text();
                    }

                    data.news.add(news);
                }
            }
        }

        // 公告
        Element announceArea = doc.getElementById("ggNewsArea");
        if (announceArea != null) {
            Elements newsLists = announceArea.select(".news_txt");
            for (Element list : newsLists) {
                Elements items = list.select("li");
                for (Element item : items) {
                    Element link = item.selectFirst("a");
                    Element titleSpan = item.selectFirst(".news_tt01");
                    Element dateSpan = item.selectFirst(".news_tt02");

                    if (link != null && titleSpan != null) {
                        NewsInfo news = new NewsInfo();
                        news.title = titleSpan.text();
                        news.link = link.attr("href");
                        news.category = "公告";

                        if (dateSpan != null) {
                            news.publishDate = dateSpan.text();
                        }

                        data.news.add(news);
                    }
                }
            }
        }
    }

    /**
     * 解析底部导航
     */
    private static void parseFooter(Document doc, ChinaMobileData data) {
        Element footer = doc.getElementById("bj_tail");
        if (footer == null) return;

        // 解析版权信息
        Element copyright = footer.selectFirst(".tailClass_new");
        if (copyright != null) {
            Elements txtDivs = copyright.select(".footcon_txt");
            for (Element div : txtDivs) {
                String text = div.text();
                if (text.contains("Copyright")) {
                    data.copyright = text;
                } else if (text.contains("经营许可证")) {
                    data.license = text;
                } else if (text.contains("IPv6")) {
                    data.ipv6Support = true;
                }
            }
        }

        // 解析友情链接
        Elements footerLinks = footer.select(".footcon_txt a");
        for (Element link : footerLinks) {
            if (!data.friendLinks.containsKey(link.text())) {
                data.friendLinks.put(link.text(), link.attr("href"));
            }
        }
    }


    /**
     * 打印解析结果到文件
     * @param data 中国移动解析数据
     * @param targetPath 目标文件路径
     */
    private static void printParsedData(ChinaMobileData data, String targetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetPath))) {

            // 1. 城市信息
            writer.write("【1. 支持城市】");
            writer.newLine();
            writer.write("  当前默认城市: " + data.defaultCity);
            writer.newLine();
            writer.write("  支持城市总数: " + data.cityCount + "个");
            writer.newLine();
            writer.newLine();
            writer.write("  按字母分组:");
            writer.newLine();

            for (Map.Entry<String, List<CityInfo>> entry : data.cityGroups.entrySet()) {
                writer.write("  " + entry.getKey() + "组 (" + entry.getValue().size() + "个):");
                writer.newLine();
                StringBuilder sb = new StringBuilder("    ");
                for (CityInfo city : entry.getValue()) {
                    sb.append(city.name).append("(").append(city.provinceAbbr).append(") ");
                }
                writer.write(sb.toString());
                writer.newLine();
            }
            writer.newLine();

            // 2. 套餐信息
            writer.write("【2. 套餐服务】");
            writer.newLine();
            writer.write("  共找到 " + data.packages.size() + " 个套餐/业务");
            writer.newLine();

            // 按分类统计
            Map<String, Integer> categoryStats = new HashMap<>();
            for (PackageInfo pkg : data.packages) {
                categoryStats.put(pkg.category, categoryStats.getOrDefault(pkg.category, 0) + 1);
            }

            writer.write("  分类统计:");
            writer.newLine();
            for (Map.Entry<String, Integer> entry : categoryStats.entrySet()) {
                writer.write("    - " + entry.getKey() + ": " + entry.getValue() + "个");
                writer.newLine();
            }
            writer.newLine();

            // 详细列出前10个主要套餐
            writer.write("  主要套餐列表:");
            writer.newLine();
            int count = 0;
            for (PackageInfo pkg : data.packages) {
                if (count++ >= 10) break;
                writer.write("    " + count + ". " + pkg.name);
                writer.newLine();
                if (pkg.description != null && !pkg.description.isEmpty()) {
                    writer.write("       描述: " + pkg.description);
                    writer.newLine();
                }
                if (!pkg.tags.isEmpty()) {
                    writer.write("       标签: " + String.join(", ", pkg.tags));
                    writer.newLine();
                }
            }
            writer.newLine();

            // 3. 品牌介绍
            writer.write("【3. 品牌介绍】");
            writer.newLine();
            for (BrandInfo brand : data.brands) {
                writer.write("  ■ " + brand.name);
                writer.newLine();
                writer.write("    口号: " + brand.slogan);
                writer.newLine();
                writer.write("    目标人群: " + brand.targetAudience);
                writer.newLine();
                writer.write("    核心价值: " + brand.coreValues);
                writer.newLine();
                writer.write("    特色: " + String.join(", ", brand.features));
                writer.newLine();
                String detailedDesc = brand.detailedDescription.length() > 60
                        ? brand.detailedDescription.substring(0, 60) + "..."
                        : brand.detailedDescription;
                writer.write("    详细介绍: " + detailedDesc);
                writer.newLine();
            }
            writer.newLine();

            // 4. 便捷服务
            writer.write("【4. 便捷服务】");
            writer.newLine();
            writer.write("  共 " + data.services.size() + " 项服务");
            writer.newLine();

            Map<String, List<String>> serviceByCategory = new HashMap<>();
            for (ServiceInfo service : data.services) {
                serviceByCategory.computeIfAbsent(service.category, k -> new ArrayList<>()).add(service.name);
            }

            for (Map.Entry<String, List<String>> entry : serviceByCategory.entrySet()) {
                writer.write("  " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
                writer.newLine();
            }
            writer.newLine();

            // 5. 新闻动态
            writer.write("【5. 新闻动态】");
            writer.newLine();
            long newsCount = data.news.stream().filter(n -> "移动要闻".equals(n.category)).count();
            long announceCount = data.news.stream().filter(n -> "公告".equals(n.category)).count();
            writer.write("  移动要闻: " + newsCount + "条");
            writer.newLine();
            writer.write("  公告: " + announceCount + "条");
            writer.newLine();
            writer.newLine();

            writer.write("  最新要闻:");
            writer.newLine();
            data.news.stream()
                    .filter(n -> "移动要闻".equals(n.category))
                    .limit(3)
                    .forEach(n -> {
                        try {
                            writer.write("    - " + n.publishDate + " " + n.title);
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            writer.newLine();
            writer.write("  最新公告:");
            writer.newLine();
            data.news.stream()
                    .filter(n -> "公告".equals(n.category))
                    .limit(2)
                    .forEach(n -> {
                        try {
                            writer.write("    - " + n.publishDate + " " + n.title);
                            writer.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            writer.newLine();

            // 6. 底部信息
            writer.write("【6. 网站信息】");
            writer.newLine();
            writer.write("  " + data.copyright);
            writer.newLine();
            writer.write("  " + data.license);
            writer.newLine();
            writer.write("  IPv6支持: " + (data.ipv6Support ? "是" : "否"));
            writer.newLine();
            writer.newLine();

            writer.write("  友情链接:");
            writer.newLine();
            int linkCount = 0;
            for (Map.Entry<String, String> entry : data.friendLinks.entrySet()) {
                if (linkCount++ >= 8) {
                    writer.write("    ...等" + data.friendLinks.size() + "个链接");
                    writer.newLine();
                    break;
                }
                writer.write("    - " + entry.getKey());
                writer.newLine();
            }

            writer.newLine();
            writer.write("==========================================");
            writer.newLine();

            System.out.println("解析结果已成功写入文件: " + targetPath);

        } catch (IOException e) {
            System.err.println("写入文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }



    // ==================== 数据类定义 ====================

    static class ChinaMobileData {
        String defaultCity = "北京";
        int cityCount = 0;
        List<CityInfo> allCities = new ArrayList<>();
        Map<String, List<CityInfo>> cityGroups = new HashMap<>();
        List<PackageInfo> packages = new ArrayList<>();
        List<BrandInfo> brands = new ArrayList<>();
        List<ServiceInfo> services = new ArrayList<>();
        List<NewsInfo> news = new ArrayList<>();
        String copyright = "";
        String license = "";
        boolean ipv6Support = false;
        Map<String, String> friendLinks = new HashMap<>();
    }

    static class CityInfo {
        String name;
        String provinceId;
        String provinceAbbr;
        String fullName;

        @Override
        public String toString() {
            return name + "(" + provinceAbbr + ")";
        }
    }

    static class PackageInfo {
        String name;
        String description;
        String link;
        String imageUrl;
        String category = "其他";
        String source;
        List<String> tags = new ArrayList<>();
    }

    static class BrandInfo {
        String name;
        String slogan;
        String description;
        String detailedDescription;
        String imageUrl;
        String imageAlt;
        String targetAudience;
        String coreValues;
        String[] features;
    }

    static class ServiceInfo {
        String name;
        String link;
        String category;
        String source;
        boolean needLogin = false;
        List<String> tags = new ArrayList<>();
    }

    static class NewsInfo {
        String title;
        String link;
        String publishDate;
        String category;
    }
}