package com.lsl.lslaiserviceagent.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 兼容字符串和数组的反序列化器
 * 输入 "北京市" -> 输出 ["北京市"]
 * 输入 [] -> 输出 []
 * 输入 ["北京","上海"] -> 输出 ["北京","上海"]
 */
public class FlexibleListDeserializer extends JsonDeserializer<List<String>> {
    
    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.getCurrentToken();
        
        // 情况1：遇到数组开始，正常解析为List
        if (token == JsonToken.START_ARRAY) {
            return p.readValueAs(new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        }
        
        // 情况2：遇到字符串，包装成单元素List
        if (token == JsonToken.VALUE_STRING) {
            String value = p.getText();
            List<String> list = new ArrayList<>();
            if (value != null && !value.isEmpty()) {
                list.add(value);
            }
            return list;
        }
        
        // 情况3：其他情况（如null）返回空列表
        return new ArrayList<>();
    }
}