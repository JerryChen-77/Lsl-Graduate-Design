package com.lsl.lslaiserviceagent.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsl.lslaiserviceagent.model.response.AmapResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmapResponseUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析高德API响应
     */
    public static AmapResponse parseResponse(String jsonStr) {
        try {
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return createErrorResponse("EMPTY_RESPONSE", "响应为空");
            }
            
            AmapResponse response = objectMapper.readValue(jsonStr, AmapResponse.class);
            
            // 验证必要字段
            if (response.getStatus() == null) {
                return createErrorResponse("INVALID_RESPONSE", "响应格式错误");
            }
            return response;
        } catch (Exception e) {
            log.error("解析高德API响应失败: {}", e.getMessage());
            return createErrorResponse("PARSE_ERROR", "解析响应失败: " + e.getMessage());
        }
    }

    /**
     * 创建错误响应对象
     */
    private static AmapResponse createErrorResponse(String infoCode, String info) {
        AmapResponse response = new AmapResponse();
        response.setStatus("0");
        response.setInfocode(infoCode);
        response.setInfo(info);
        return response;
    }

    /**
     * 判断响应是否成功
     */
    public static boolean isSuccess(AmapResponse response) {
        return response != null && response.isSuccess();
    }

    /**
     * 获取错误信息
     */
    public static String getErrorInfo(AmapResponse response) {
        if (response == null) {
            return "未知错误";
        }
        return response.getErrorMessage();
    }
}