package com.lsl.lslaiserviceagent.utils;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsl.lslaiserviceagent.model.response.AmapResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;

@Component
@Slf4j
@Data
public class IpUtils {

    @Value("${amap.api.key}")
    private static String amap_key = "a76dd99d151f6d5ddaff987770aa3e1f";

    // 常见的代理IP头部名称
    private static final String[] IP_HEADERS = {
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR",
        "X-Real-IP"  // Nginx代理专用
    };

    public AmapResponse getAddressBy(HttpServletRequest request) {
        String currentIp = getClientIp(request);
        return getAddressByIp(currentIp);
    }
    /**
     * 获取客户端真实IP地址
     * @param request HttpServletRequest
     * @return 客户端IP地址
     */
    public String getClientIp(HttpServletRequest request) {
        String ip = null;
        
        // 1. 遍历常见的代理头部
        for (String header : IP_HEADERS) {
            ip = request.getHeader(header);
            if (isValidIp(ip)) {
                break;
            }
        }
        
        // 2. 如果从头部没有获取到，则使用getRemoteAddr()
        if (!isValidIp(ip)) {
            ip = request.getRemoteAddr();
            
            // 3. 处理本地回环地址（开发环境常见）
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                // 尝试获取本机真实IP（仅用于开发调试）
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    // 忽略，保持原值
                }
            }
        }
        
        // 4. 处理X-Forwarded-For多级代理的情况（取第一个IP）
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String strIp : ips) {
                if (isValidIp(strIp.trim())) {
                    ip = strIp.trim();
                    break;
                }
            }
        }
        
        return ip;
    }

    // wrong :{"status":"0","info":"INVALID_USER_KEY","infocode":"10001"}
    // success: {"status":"1","info":"OK","infocode":"10000","province":"北京市","city":"北京市","adcode":"110000","rectangle":"116.0119343,39.66127144;116.7829835,40.2164962"}
    /**
     * 根据IP获取地址信息（返回AmapResponse对象）
     */
    public AmapResponse getAddressByIp(String ip) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("key", amap_key);
        //"114.247.50.2" 北京ip
        params.put("ip", ip);

        try {
            String jsonStr = HttpUtil.get("https://restapi.amap.com/v3/ip", params);
            log.debug("高德API响应: {}", jsonStr);

            // 使用响应工具类解析
            return AmapResponseUtil.parseResponse(jsonStr);

        } catch (Exception e) {
            log.error("调用高德IP定位API失败", e);
            AmapResponse errorResponse = new AmapResponse();
            errorResponse.setStatus("0");
            errorResponse.setInfocode("API_ERROR");
            errorResponse.setInfo("API调用失败: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * 根据IP获取地址信息（返回JSON字符串，兼容原有方法）
     */
    public String getAddressByIpAsString(String ip) {
        AmapResponse response = getAddressByIp(ip);
        try {
            return new ObjectMapper().writeValueAsString(response);
        } catch (Exception e) {
            log.error("转换响应为JSON失败", e);
            return "{\"status\":\"0\",\"info\":\"响应转换失败\"}";
        }
    }


    /**
     * 判断IP是否有效
     * @param ip IP地址字符串
     * @return 是否有效
     */
    private static boolean isValidIp(String ip) {
        return ip != null && ip.length() > 0 
                && !"unknown".equalsIgnoreCase(ip)
                && !"null".equalsIgnoreCase(ip);
    }
}