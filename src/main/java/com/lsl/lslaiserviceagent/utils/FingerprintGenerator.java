package com.lsl.lslaiserviceagent.utils;

import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Component
public class FingerprintGenerator {
    
    // SimHash位数
    private static final int HASH_BITS = 64;
    
    // 特征权重（可根据词性调整）
    private static final Map<String, Integer> WORD_WEIGHT = new HashMap<>();
    static {
        // 名词权重高
        WORD_WEIGHT.put("n", 5);
        WORD_WEIGHT.put("nr", 5);  // 人名
        WORD_WEIGHT.put("ns", 5);  // 地名
        WORD_WEIGHT.put("nt", 5);  // 机构名
        // 动词中等权重
        WORD_WEIGHT.put("v", 3);
        // 形容词权重低
        WORD_WEIGHT.put("a", 1);
    }
    
    /**
     * 生成问题指纹（SimHash）
     */
    public String generateFingerprint(String normalizedText) {
        if (normalizedText == null || normalizedText.isEmpty()) {
            return "";
        }
        
        // 1. 分词并获取特征
        List<String> words = segmentText(normalizedText);
        
        // 2. 初始化64位向量
        int[] vector = new int[HASH_BITS];
        
        // 3. 对每个特征计算hash并累加
        for (String word : words) {
            BigInteger hash = hashWord(word);
            int weight = getWordWeight(word);
            
            for (int i = 0; i < HASH_BITS; i++) {
                BigInteger bitmask = BigInteger.ONE.shiftLeft(HASH_BITS - i - 1);
                if (hash.and(bitmask).signum() != 0) {
                    vector[i] += weight;
                } else {
                    vector[i] -= weight;
                }
            }
        }
        
        // 4. 降维，生成最终指纹
        BigInteger fingerprint = BigInteger.ZERO;
        for (int i = 0; i < HASH_BITS; i++) {
            if (vector[i] > 0) {
                fingerprint = fingerprint.or(BigInteger.ONE.shiftLeft(HASH_BITS - i - 1));
            }
        }
        
        // 5. 返回16进制字符串
        return fingerprint.toString(16);
    }
    
    /**
     * 计算词的哈希值
     */
    private BigInteger hashWord(String word) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(word.getBytes(StandardCharsets.UTF_8));
            return new BigInteger(1, bytes);
        } catch (Exception e) {
            return BigInteger.valueOf(word.hashCode());
        }
    }
    
    /**
     * 获取词权重
     */
    private int getWordWeight(String word) {
        // 简单实现：根据长度和词性返回权重
        // 实际可使用词性标注结果
        if (word.length() >= 4) {
            return 4; // 长词权重高
        } else if (word.length() >= 2) {
            return 2;
        }
        return 1;
    }
    
    /**
     * 分词（简化版）
     */
    private List<String> segmentText(String text) {
        List<String> words = new ArrayList<>();
        
        // 简单按字符分割并组合成词
        // 实际项目中可使用 HanLP、IK Analyzer 等分词工具
        char[] chars = text.toCharArray();
        StringBuilder currentWord = new StringBuilder();
        
        for (char c : chars) {
            if (isChinese(c) || isEnglish(c) || isDigit(c)) {
                currentWord.append(c);
            } else {
                if (currentWord.length() > 0) {
                    words.add(currentWord.toString());
                    currentWord = new StringBuilder();
                }
            }
        }
        
        if (currentWord.length() > 0) {
            words.add(currentWord.toString());
        }
        
        return words;
    }
    
    /**
     * 计算两个指纹的汉明距离
     */
    public int hammingDistance(String fingerprint1, String fingerprint2) {
        BigInteger hash1 = new BigInteger(fingerprint1, 16);
        BigInteger hash2 = new BigInteger(fingerprint2, 16);
        
        BigInteger xor = hash1.xor(hash2);
        return xor.bitCount();
    }
    
    /**
     * 判断两个问题是否相似（汉明距离 <= 3）
     */
    public boolean isSimilar(String fingerprint1, String fingerprint2, int threshold) {
        if (fingerprint1 == null || fingerprint2 == null) {
            return false;
        }
        return hammingDistance(fingerprint1, fingerprint2) <= threshold;
    }
    
    private boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS;
    }
    
    private boolean isEnglish(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}