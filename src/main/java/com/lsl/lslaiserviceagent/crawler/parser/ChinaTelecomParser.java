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
 * 中国电信HTML解析器 - 精进版
 * 解析功能：
 * 1. 支持城市列表（省份和地市两级）
 * 2. 套餐服务（导航菜单、流量超市、智能宽带）
 * 3. 充值服务
 * 4. 欢享服务（快捷入口）
 * 5. 公告资讯
 * 6. 底部导航和版权信息
 */
@Slf4j
public class ChinaTelecomParser implements Parser{

    @Override
    public void parseHtml(String filePath, String targetFilePath){
        try {
            log.info("==========================================");
            log.info("    中国电信网上营业厅 - 精进解析器输出");
            log.info("==========================================\n");

            // 解析HTML文件
            File input = new File(filePath);
            Document doc = Jsoup.parse(input, "UTF-8");

            // 创建解析结果对象
            ChinaTelecomData data = new ChinaTelecomData();
            data.pageTitle = doc.title();

            // 1. 解析运营商支持的城市（省份和地市）
            parseSupportedCities(doc, data);

            // 2. 解析导航菜单套餐
            parseNavPackages(doc, data);

            // 3. 解析流量超市（极客炫耀）
            parseTrafficPackages(doc, data);

            // 4. 解析智能宽带（快乐e家）
            parseBroadbandPackages(doc, data);

            // 5. 解析欢享服务（右侧快捷入口）
            parseQuickServices(doc, data);

            // 6. 解析充值服务
            parseRecharge(doc, data);

            // 7. 解析公告资讯
            parseAnnouncements(doc, data);

            // 8. 解析底部导航
            parseFooter(doc, data);

            // 输出解析结果
            printParsedData(data,targetFilePath);

        } catch (IOException e) {
            System.err.println("解析文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 解析支持的城市列表（省份和地市两级）
     */
    private static void parseSupportedCities(Document doc, ChinaTelecomData data) {
        // 获取当前默认省份
        Element currentProvince = doc.selectFirst(".topCity .show a");
        if (currentProvince != null) {
            data.currentProvince = currentProvince.text().trim();
        }
        
        // 获取当前默认地市
        Element currentCity = doc.selectFirst("#dishi_new a");
        if (currentCity != null) {
            data.currentCity = currentCity.text().trim();
        }
        
        // 解析省份列表
        Element provinceList = doc.selectFirst(".indexCity");
        if (provinceList != null) {
            Elements provinceGroups = provinceList.select("li");
            
            for (Element group : provinceGroups) {
                Element groupLetter = group.selectFirst(".ff8");
                if (groupLetter == null) continue;
                
                String letter = groupLetter.text().trim();
                List<ProvinceInfo> provincesInGroup = new ArrayList<>();
                
                Elements provinceLinks = group.select(".dpl a");
                for (Element link : provinceLinks) {
                    ProvinceInfo province = new ProvinceInfo();
                    province.name = link.text().trim();
                    province.code = link.attr("onclick")
                                       .replace("redirectProvince('", "")
                                       .replace("');", "");
                    
                    provincesInGroup.add(province);
                    data.allProvinces.add(province);
                }
                
                data.provinceGroups.put(letter, provincesInGroup);
            }
        }
        
        // 解析地市列表（以四川为例）
        Element cityList = doc.selectFirst(".indexCitya");
        if (cityList != null) {
            Elements cityLinks = cityList.select("a");
            for (Element link : cityLinks) {
                CityInfo city = new CityInfo();
                city.name = link.text().trim();
                city.code = link.attr("onclick")
                                 .replace("redirectProvince('", "")
                                 .replace("');", "");
                city.province = "四川";
                
                data.cities.add(city);
            }
        }
        
        data.provinceCount = data.allProvinces.size();
        data.cityCount = data.cities.size();
    }
    
    /**
     * 解析导航菜单套餐
     */
    private static void parseNavPackages(Document doc, ChinaTelecomData data) {
        Element menuDown = doc.selectFirst(".meun_down");
        if (menuDown == null) return;
        
        Elements menuItems = menuDown.select("> ul > li");
        
        for (Element item : menuItems) {
            String category = item.select("> a").text().trim();
            
            // 根据类别解析不同的下拉菜单
            if (category.contains("热门活动")) {
                parseHotActivities(item, data);
            } else if (category.contains("智能宽带")) {
                parseSmartBroadband(item, data);
            } else if (category.contains("充值")) {
                parseRechargeMenu(item, data);
            } else if (category.contains("费用")) {
                parseFeeMenu(item, data);
            } else if (category.contains("业务")) {
                parseBusinessMenu(item, data);
            }
        }
    }
    
    /**
     * 解析热门活动
     */
    private static void parseHotActivities(Element menuItem, ChinaTelecomData data) {
        Elements activityDivs = menuItem.select(".down_ul_a");
        
        for (Element div : activityDivs) {
            Element span = div.selectFirst(".span_font_a");
            if (span == null) continue;
            
            Elements links = span.select("a");
            for (Element link : links) {
                ActivityInfo activity = new ActivityInfo();
                activity.name = link.text().replace("|", "").trim();
                activity.link = link.attr("href");
                activity.category = div.select(".fb600").text().trim();
                
                // 判断是否有红色标签
                Element font = link.selectFirst("font");
                if (font != null) {
                    activity.hasRedTag = true;
                    activity.redTagText = font.text();
                }
                
                data.activities.add(activity);
            }
        }
        
        // 解析活动图片
        Element imgDiv = menuItem.selectFirst(".w.h.ov.ml20.mr20.pt10");
        if (imgDiv != null) {
            Element imgLink = imgDiv.selectFirst("a");
            Element img = imgDiv.selectFirst("img");
            if (imgLink != null && img != null) {
                ActivityBanner banner = new ActivityBanner();
                banner.link = imgLink.attr("href");
                banner.imageUrl = img.attr("data-original");
                banner.alt = img.attr("alt");
                data.activityBanners.add(banner);
            }
        }
    }
    
    /**
     * 解析智能宽带
     */
    private static void parseSmartBroadband(Element menuItem, ChinaTelecomData data) {
        Elements broadbandDivs = menuItem.select(".down_ul_a");
        
        for (Element div : broadbandDivs) {
            String category = div.select(".fb600").text().trim();
            Element span = div.selectFirst(".span_font_a");
            if (span == null) continue;
            
            Elements links = span.select("a");
            for (Element link : links) {
                BroadbandMenu menu = new BroadbandMenu();
                menu.name = link.text().replace("|", "").trim();
                menu.link = link.attr("href");
                menu.category = category;
                
                // 判断是否有红色字体
                Element font = link.selectFirst("font");
                if (font != null) {
                    menu.hasRedTag = true;
                    menu.redTagText = font.text();
                }
                
                data.broadbandMenus.add(menu);
            }
        }
    }
    
    /**
     * 解析充值菜单
     */
    private static void parseRechargeMenu(Element menuItem, ChinaTelecomData data) {
        Elements rechargeDivs = menuItem.select(".down_ul_a");
        
        for (Element div : rechargeDivs) {
            String category = div.select(".fb600").text().trim();
            Element span = div.selectFirst(".span_font_a");
            if (span == null) continue;
            
            Elements links = span.select("a");
            for (Element link : links) {
                RechargeMenu menu = new RechargeMenu();
                menu.name = link.text().replace("|", "").trim();
                menu.link = link.attr("href");
                menu.category = category;
                
                data.rechargeMenus.add(menu);
            }
        }
        
        // 解析充值图片
        Element imgDiv = menuItem.selectFirst(".w.h.ov.ml20.mr20.pt10");
        if (imgDiv != null) {
            Element imgLink = imgDiv.selectFirst("a");
            Element img = imgDiv.selectFirst("img");
            if (imgLink != null && img != null) {
                RechargeBanner banner = new RechargeBanner();
                banner.link = imgLink.attr("href");
                banner.imageUrl = img.attr("data-original");
                banner.alt = img.attr("alt");
                data.rechargeBanners.add(banner);
            }
        }
    }
    
    /**
     * 解析费用菜单
     */
    private static void parseFeeMenu(Element menuItem, ChinaTelecomData data) {
        Elements feeDivs = menuItem.select(".down_ul_a");
        
        for (Element div : feeDivs) {
            String category = div.select(".fb600").text().trim();
            Element span = div.selectFirst(".span_font_a");
            if (span == null) continue;
            
            Elements links = span.select("a");
            for (Element link : links) {
                FeeMenu menu = new FeeMenu();
                menu.name = link.text().replace("|", "").trim();
                menu.link = link.attr("href");
                menu.category = category;
                
                data.feeMenus.add(menu);
            }
        }
        
        // 解析费用图片
        Element imgDiv = menuItem.selectFirst(".w.h.ov.ml20.mr20.pt10");
        if (imgDiv != null) {
            Element imgLink = imgDiv.selectFirst("a");
            Element img = imgDiv.selectFirst("img");
            if (imgLink != null && img != null) {
                FeeBanner banner = new FeeBanner();
                banner.link = imgLink.attr("href");
                banner.imageUrl = img.attr("data-original");
                banner.alt = img.attr("alt");
                data.feeBanners.add(banner);
            }
        }
    }
    
    /**
     * 解析业务菜单
     */
    private static void parseBusinessMenu(Element menuItem, ChinaTelecomData data) {
        Elements businessDivs = menuItem.select(".down_ul_a");
        
        for (Element div : businessDivs) {
            String category = div.select(".fb600").text().trim();
            Element span = div.selectFirst(".span_font_a");
            if (span == null) continue;
            
            Elements links = span.select("a");
            for (Element link : links) {
                BusinessMenu menu = new BusinessMenu();
                menu.name = link.text().replace("|", "").trim();
                menu.link = link.attr("href");
                menu.category = category;
                
                data.businessMenus.add(menu);
            }
        }
        
        // 解析业务图片（可能有多个）
        Elements imgDivs = menuItem.select(".w.h.ov.ml20.mr20.pt10 span");
        for (Element imgSpan : imgDivs) {
            Element imgLink = imgSpan.selectFirst("a");
            Element img = imgSpan.selectFirst("img");
            if (imgLink != null && img != null) {
                BusinessBanner banner = new BusinessBanner();
                banner.link = imgLink.attr("href");
                banner.imageUrl = img.attr("data-original");
                banner.alt = img.attr("alt");
                data.businessBanners.add(banner);
            }
        }
    }
    
    /**
     * 解析流量超市（极客炫耀）
     */
    private static void parseTrafficPackages(Document doc, ChinaTelecomData data) {
        Element geeksDiv = doc.selectFirst(".sports_geeks");
        if (geeksDiv == null) return;
        
        // 解析流量套餐
        Elements trafficLinks = geeksDiv.select(".sps_gk_n_right_a li a");
        for (Element link : trafficLinks) {
            TrafficPackage pkg = new TrafficPackage();
            
            // 尝试获取价格/名称
            Element parent = link.parent();
            if (parent != null) {
                Element priceElem = parent.selectFirst(".fb600");
                if (priceElem != null) {
                    pkg.name = priceElem.text().trim();
                }
                
                Element img = parent.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("data-original");
                    if (pkg.name == null || pkg.name.isEmpty()) {
                        pkg.name = img.attr("alt");
                    }
                }
            }
            
            pkg.link = link.attr("href");
            pkg.type = "流量套餐";
            
            data.trafficPackages.add(pkg);
        }
        
        // 解析流量相关服务（充流量、充语音等）
        Elements trafficServices = geeksDiv.select(".sps_gk_n_right_b li a");
        for (Element link : trafficServices) {
            TrafficPackage pkg = new TrafficPackage();
            
            Element parent = link.parent();
            if (parent != null) {
                Element nameElem = parent.selectFirst(".fb600");
                if (nameElem != null) {
                    pkg.name = nameElem.text().trim();
                }
                
                Element img = parent.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("data-original");
                }
            }
            
            pkg.link = link.attr("href");
            pkg.type = "流量服务";
            
            data.trafficPackages.add(pkg);
        }
        
        // 解析左侧流量入口
        Element leftTraffic = geeksDiv.selectFirst(".sps_gk_n_left");
        if (leftTraffic != null) {
            Element trafficLink = leftTraffic.selectFirst(".br_img a");
            if (trafficLink != null) {
                TrafficPackage pkg = new TrafficPackage();
                pkg.name = trafficLink.text().trim();
                pkg.link = trafficLink.attr("href");
                pkg.type = "流量入口";
                
                Element img = trafficLink.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("data-original");
                }
                
                data.trafficPackages.add(pkg);
            }
        }
    }
    
    /**
     * 解析智能宽带（快乐e家）
     */
    private static void parseBroadbandPackages(Document doc, ChinaTelecomData data) {
        Element homeDiv = doc.select(".sports_geeks").get(1); // 第二个sports_geeks
        if (homeDiv == null) return;
        
        // 解析宽带套餐
        Elements broadbandLinks = homeDiv.select(".fr .fl li a, .fr .fr li a, .fr .fl.w201 a");
        for (Element link : broadbandLinks) {
            BroadbandPackage pkg = new BroadbandPackage();
            
            Element parent = link.parent();
            if (parent != null) {
                Element nameElem = parent.selectFirst(".fb600");
                if (nameElem != null) {
                    pkg.name = nameElem.text().trim();
                }
                
                Element descElem = parent.selectFirst(".f12");
                if (descElem != null && !descElem.text().contains("img")) {
                    pkg.description = descElem.text().trim();
                }
                
                Element img = parent.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("data-original");
                    if (pkg.name == null || pkg.name.isEmpty()) {
                        pkg.name = img.attr("alt");
                    }
                }
            }
            
            pkg.link = link.attr("href");
            data.broadbandPackages.add(pkg);
        }
        
        // 解析左侧宽带入口
        Element leftBroadband = homeDiv.selectFirst(".sps_gk_n_left");
        if (leftBroadband != null) {
            Elements broadbandEntries = leftBroadband.select(".br_img a");
            for (Element link : broadbandEntries) {
                BroadbandPackage pkg = new BroadbandPackage();
                pkg.name = link.text().trim();
                pkg.link = link.attr("href");
                
                Element img = link.selectFirst("img");
                if (img != null) {
                    pkg.imageUrl = img.attr("data-original");
                }
                
                data.broadbandPackages.add(pkg);
            }
        }
    }
    
    /**
     * 解析欢享服务（右侧快捷入口）
     */
    private static void parseQuickServices(Document doc, ChinaTelecomData data) {
        Element serviceDiv = doc.selectFirst(".pt_main");
        if (serviceDiv == null) return;
        
        Elements serviceLinks = serviceDiv.select(".list_img a");
        for (Element link : serviceLinks) {
            QuickService service = new QuickService();
            
            Element img = link.selectFirst("img");
            if (img != null) {
                service.icon = img.attr("src");
                service.name = img.attr("alt");
            } else {
                service.name = link.text().trim();
            }
            
            service.link = link.attr("href");
            data.quickServices.add(service);
        }
    }
    
    /**
     * 解析充值服务
     */
    private static void parseRecharge(Document doc, ChinaTelecomData data) {
        Element rechargeDiv = doc.selectFirst(".jiner").parent();
        if (rechargeDiv == null) return;
        
        // 解析充值选项卡
        Elements tabs = rechargeDiv.select("#lx_nav1 li");
        for (Element tab : tabs) {
            RechargeTab rechargeTab = new RechargeTab();
            rechargeTab.name = tab.text().trim();
            
            Element link = tab.selectFirst("a");
            if (link != null) {
                rechargeTab.link = link.attr("href");
                rechargeTab.hasLink = true;
            }
            
            data.rechargeTabs.add(rechargeTab);
        }
        
        // 解析充值方式
        Elements rechargeMethods = rechargeDiv.select("#lx_nav3 li");
        for (Element method : rechargeMethods) {
            String methodName = method.text().trim();
            data.rechargeMethods.add(methodName);
        }
        
        // 解析充值金额选项
        Elements amountOptions = rechargeDiv.select(".son_ul li");
        for (Element option : amountOptions) {
            String amount = option.text().trim();
            data.rechargeAmounts.add(amount);
        }
    }
    
    /**
     * 解析公告资讯
     */
    private static void parseAnnouncements(Document doc, ChinaTelecomData data) {
        Element announceDiv = doc.selectFirst(".gg_a marquee");
        if (announceDiv == null) return;
        
        Element link = announceDiv.selectFirst("a");
        if (link != null) {
            Announcement ann = new Announcement();
            ann.title = link.text().trim();
            ann.link = link.attr("href");
            data.announcements.add(ann);
        }
    }
    
    /**
     * 解析底部导航
     */
    private static void parseFooter(Document doc, ChinaTelecomData data) {
        Element footer = doc.selectFirst(".footer");
        if (footer == null) return;
        
        // 解析售后导航
        Element aftermarket = footer.selectFirst(".aftermarket");
        if (aftermarket != null) {
            Elements navSections = aftermarket.select("ul li");
            for (Element section : navSections) {
                String category = section.select(".fb600").text().trim();
                if (category.isEmpty()) continue;
                
                Elements links = section.select("p:not(.fb600) a");
                for (Element link : links) {
                    FooterLink footerLink = new FooterLink();
                    footerLink.name = link.text().trim();
                    footerLink.link = link.attr("href");
                    footerLink.category = category;
                    
                    data.footerLinks.add(footerLink);
                }
            }
        }
        
        // 解析合作伙伴（各省电信）
        Element partnerDiv = footer.selectFirst("#vouchb_1");
        if (partnerDiv != null) {
            Elements partnerLinks = partnerDiv.select("a");
            for (Element link : partnerLinks) {
                Partner partner = new Partner();
                partner.name = link.text().trim();
                partner.link = link.attr("href");
                
                data.partners.add(partner);
            }
        }
        
        // 解析版权信息
        Element copyrightDiv = footer.selectFirst(".footTwo");
        if (copyrightDiv != null) {
            Elements copyrightLinks = copyrightDiv.select(".cl_style a");
            for (Element link : copyrightLinks) {
                data.copyrightLinks.add(link.text().trim());
            }
            
            Element license = copyrightDiv.selectFirst(".foot_style");
            if (license != null) {
                data.license = license.text().trim();
            }
        }
    }

    /**
     * 打印中国电信解析结果到文件
     * @param data 中国电信解析数据
     * @param targetPath 目标文件路径
     */
    private static void printParsedData(ChinaTelecomData data, String targetPath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetPath))) {
            // 1. 城市信息
            writer.write("\n【1. 支持城市】");
            writer.newLine();
            writer.write("  支持省份总数: " + data.provinceCount + "个");
            writer.newLine();
            writer.newLine();
            writer.write("  省份分组:");
            writer.newLine();

            for (Map.Entry<String, List<ProvinceInfo>> entry : data.provinceGroups.entrySet()) {
                writer.write("  " + entry.getKey() + ": ");
                int count = 0;
                StringBuilder sb = new StringBuilder();
                for (ProvinceInfo p : entry.getValue()) {
                    sb.append(p.name).append(" ");
                    count++;
                    if (count % 8 == 0) {
                        sb.append("\n     ");
                    }
                }
                writer.write(sb.toString());
                writer.newLine();
            }

            // 2. 热门活动
            writer.write("\n【2. 热门活动】");
            writer.newLine();
            writer.write("  共 " + data.activities.size() + " 个活动");
            writer.newLine();
            for (ActivityInfo act : data.activities) {
                writer.write("    - " + act.category + ": " + act.name +
                        (act.hasRedTag ? " [" + act.redTagText + "]" : ""));
                writer.newLine();
            }

            // 3. 智能宽带菜单
            writer.write("\n【3. 智能宽带菜单】");
            writer.newLine();
            writer.write("  共 " + data.broadbandMenus.size() + " 个菜单项");
            writer.newLine();

            Map<String, List<String>> broadbandMap = new HashMap<>();
            for (BroadbandMenu menu : data.broadbandMenus) {
                broadbandMap.computeIfAbsent(menu.category, k -> new ArrayList<>()).add(menu.name);
            }
            for (Map.Entry<String, List<String>> entry : broadbandMap.entrySet()) {
                writer.write("    " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
                writer.newLine();
            }

            // 4. 充值菜单
            writer.write("\n【4. 充值服务】");
            writer.newLine();
            writer.write("  充值方式: " + data.rechargeMethods);
            writer.newLine();
            writer.write("  充值金额: " + data.rechargeAmounts);
            writer.newLine();
            writer.write("  充值菜单 (" + data.rechargeMenus.size() + "项):");
            writer.newLine();
            for (RechargeMenu menu : data.rechargeMenus) {
                writer.write("    - " + menu.category + ": " + menu.name);
                writer.newLine();
            }

            // 5. 费用菜单
            writer.write("\n【5. 费用查询】");
            writer.newLine();
            writer.write("  共 " + data.feeMenus.size() + " 项服务");
            writer.newLine();

            Map<String, List<String>> feeMap = new HashMap<>();
            for (FeeMenu menu : data.feeMenus) {
                feeMap.computeIfAbsent(menu.category, k -> new ArrayList<>()).add(menu.name);
            }
            for (Map.Entry<String, List<String>> entry : feeMap.entrySet()) {
                writer.write("    " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
                writer.newLine();
            }

            // 6. 业务菜单
            writer.write("\n【6. 业务办理】");
            writer.newLine();
            writer.write("  共 " + data.businessMenus.size() + " 项业务");
            writer.newLine();

            Map<String, List<String>> businessMap = new HashMap<>();
            for (BusinessMenu menu : data.businessMenus) {
                businessMap.computeIfAbsent(menu.category, k -> new ArrayList<>()).add(menu.name);
            }
            for (Map.Entry<String, List<String>> entry : businessMap.entrySet()) {
                writer.write("    " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
                writer.newLine();
            }

            // 7. 流量超市
            writer.write("\n【7. 流量超市】");
            writer.newLine();
            writer.write("  共 " + data.trafficPackages.size() + " 个流量产品");
            writer.newLine();
            for (TrafficPackage pkg : data.trafficPackages) {
                writer.write("    - " + pkg.name + " [" + pkg.type + "]");
                writer.newLine();
            }

            // 8. 智能宽带套餐
            writer.write("\n【8. 智能宽带套餐】");
            writer.newLine();
            writer.write("  共 " + data.broadbandPackages.size() + " 个宽带产品");
            writer.newLine();
            for (BroadbandPackage pkg : data.broadbandPackages) {
                writer.write("    - " + pkg.name +
                        (pkg.description != null ? ": " + pkg.description : ""));
                writer.newLine();
            }

            // 9. 欢享服务
            writer.write("\n【9. 欢享服务（快捷入口）】");
            writer.newLine();
            writer.write("    ");
            for (QuickService service : data.quickServices) {
                writer.write(service.name + "  ");
            }
            writer.newLine();

            // 10. 公告
            writer.write("\n【10. 最新公告】");
            writer.newLine();
            for (Announcement ann : data.announcements) {
                writer.write("    - " + ann.title);
                writer.newLine();
            }

            // 11. 底部导航
            writer.write("\n【11. 底部导航】");
            writer.newLine();

            Map<String, List<String>> footerMap = new HashMap<>();
            for (FooterLink link : data.footerLinks) {
                footerMap.computeIfAbsent(link.category, k -> new ArrayList<>()).add(link.name);
            }
            for (Map.Entry<String, List<String>> entry : footerMap.entrySet()) {
                writer.write("    " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
                writer.newLine();
            }

            // 12. 合作伙伴
            writer.write("\n【12. 各省电信合作伙伴】");
            writer.newLine();
            writer.write("    ");
            int count = 0;
            StringBuilder partnerSb = new StringBuilder();
            for (Partner p : data.partners) {
                partnerSb.append(p.name).append(" ");
                count++;
                if (count % 10 == 0) {
                    partnerSb.append("\n    ");
                }
            }
            writer.write(partnerSb.toString());
            writer.newLine();

            // 13. 版权信息
            writer.write("\n【13. 版权信息】");
            writer.newLine();
            writer.write("    " + data.license);
            writer.newLine();
            writer.write("    相关链接: " + String.join(" | ", data.copyrightLinks));
            writer.newLine();

            writer.newLine();
            writer.write("==========================================");
            writer.newLine();

            System.out.println("中国电信解析结果已成功写入文件: " + targetPath);

        } catch (IOException e) {
            System.err.println("写入文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== 数据类定义 ====================
    
    static class ChinaTelecomData {
        String pageTitle;
        String currentProvince = "四川";
        String currentCity = "成都";
        int provinceCount = 0;
        int cityCount = 0;
        List<ProvinceInfo> allProvinces = new ArrayList<>();
        Map<String, List<ProvinceInfo>> provinceGroups = new HashMap<>();
        List<CityInfo> cities = new ArrayList<>();
        List<ActivityInfo> activities = new ArrayList<>();
        List<ActivityBanner> activityBanners = new ArrayList<>();
        List<BroadbandMenu> broadbandMenus = new ArrayList<>();
        List<RechargeMenu> rechargeMenus = new ArrayList<>();
        List<RechargeBanner> rechargeBanners = new ArrayList<>();
        List<FeeMenu> feeMenus = new ArrayList<>();
        List<FeeBanner> feeBanners = new ArrayList<>();
        List<BusinessMenu> businessMenus = new ArrayList<>();
        List<BusinessBanner> businessBanners = new ArrayList<>();
        List<TrafficPackage> trafficPackages = new ArrayList<>();
        List<BroadbandPackage> broadbandPackages = new ArrayList<>();
        List<QuickService> quickServices = new ArrayList<>();
        List<RechargeTab> rechargeTabs = new ArrayList<>();
        List<String> rechargeMethods = new ArrayList<>();
        List<String> rechargeAmounts = new ArrayList<>();
        List<Announcement> announcements = new ArrayList<>();
        List<FooterLink> footerLinks = new ArrayList<>();
        List<Partner> partners = new ArrayList<>();
        List<String> copyrightLinks = new ArrayList<>();
        String license = "";
    }
    
    static class ProvinceInfo {
        String name;
        String code;
    }
    
    static class CityInfo {
        String name;
        String code;
        String province;
    }
    
    static class ActivityInfo {
        String name;
        String link;
        String category;
        boolean hasRedTag = false;
        String redTagText;
    }
    
    static class ActivityBanner {
        String link;
        String imageUrl;
        String alt;
    }
    
    static class BroadbandMenu {
        String name;
        String link;
        String category;
        boolean hasRedTag = false;
        String redTagText;
    }
    
    static class RechargeMenu {
        String name;
        String link;
        String category;
    }
    
    static class RechargeBanner {
        String link;
        String imageUrl;
        String alt;
    }
    
    static class FeeMenu {
        String name;
        String link;
        String category;
    }
    
    static class FeeBanner {
        String link;
        String imageUrl;
        String alt;
    }
    
    static class BusinessMenu {
        String name;
        String link;
        String category;
    }
    
    static class BusinessBanner {
        String link;
        String imageUrl;
        String alt;
    }
    
    static class TrafficPackage {
        String name;
        String link;
        String imageUrl;
        String type;
    }
    
    static class BroadbandPackage {
        String name;
        String description;
        String link;
        String imageUrl;
    }
    
    static class QuickService {
        String name;
        String link;
        String icon;
    }
    
    static class RechargeTab {
        String name;
        String link;
        boolean hasLink = false;
    }
    
    static class Announcement {
        String title;
        String link;
    }
    
    static class FooterLink {
        String name;
        String link;
        String category;
    }
    
    static class Partner {
        String name;
        String link;
    }
}