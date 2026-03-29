package com.lsl.lslaiserviceagent.utils;

import org.springframework.stereotype.Component;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class QuestionNormalizer {
    
    // 停用词列表
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", 
        "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", 
        "没有", "看", "好", "什么", "怎么", "为什么", "如何", "哪", "哪个",
        "请问", "想", "知道", "问", "一下", "吧", "吗", "呢", "啊", "哦"
    ));
    
    // 标点符号正则
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}。，、；：？！“”‘’【】（）《》]");
    
    // 数字正则
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    
    // 空白字符正则
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    
    /**
     * 问题标准化
     */
    public String normalize(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "";
        }
        
        String normalized = question;
        
        // 1. 去除首尾空白
        normalized = normalized.trim();
        
        // 2. Unicode标准化（NFKC形式，统一字符表示）
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        
        // 3. 转为小写（英文部分）
        normalized = normalized.toLowerCase(Locale.ROOT);
        
        // 4. 去除标点符号
        normalized = PUNCTUATION_PATTERN.matcher(normalized).replaceAll("");
        
        // 5. 数字归一化（所有数字替换为0）
        normalized = NUMBER_PATTERN.matcher(normalized).replaceAll("0");
        
        // 6. 中文分词 + 去除停用词
        normalized = segmentAndRemoveStopWords(normalized);
        
        // 7. 同义词替换（可选）
        normalized = replaceSynonyms(normalized);
        
        // 8. 去除多余空白
        normalized = WHITESPACE_PATTERN.matcher(normalized).replaceAll(" ");
        
        return normalized.trim();
    }
    
    /**
     * 中文分词并去除停用词
     */
    private String segmentAndRemoveStopWords(String text) {
        // 这里使用简单的按字符分割，实际项目中可以用 HanLP 或 IK Analyzer
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        
        for (char c : chars) {
            String word = String.valueOf(c);
            // 保留中文、英文、数字
            if (isChinese(c) || isEnglish(c) || isDigit(c)) {
                // 去除停用词
                if (!STOP_WORDS.contains(word)) {
                    result.append(c);
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * 同义词替换（简化版，实际可配置同义词库）
     */
    private String replaceSynonyms(String text) {
        // 示例：天气 -> 天气（保持不变），可以扩展为配置表
        Map<String, String> synonyms = new HashMap<>();
        synonyms.put("气温", "温度");
        synonyms.put("气候", "天气");
        synonyms.put("价钱", "价格");
        synonyms.put("金额", "价格");
        
        for (Map.Entry<String, String> entry : synonyms.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        
        return text;
    }
    
    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
    }
    
    private boolean isEnglish(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}