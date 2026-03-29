package com.lsl.lslaiserviceagent.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.lsl.lslaiserviceagent.utils.FingerprintGenerator;
import com.lsl.lslaiserviceagent.utils.QuestionNormalizer;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lsl.lslaiserviceagent.model.entity.QuestionFingerprint;
import com.lsl.lslaiserviceagent.mapper.QuestionFingerprintMapper;
import com.lsl.lslaiserviceagent.service.QuestionFingerprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 问题指纹表 服务层实现。
 *
 * @author SiLin li
 */
@Service
public class QuestionFingerprintServiceImpl extends ServiceImpl<QuestionFingerprintMapper, QuestionFingerprint>  implements QuestionFingerprintService{

    // 相似度阈值（汉明距离）
    private static final int SIMILARITY_THRESHOLD = 3;

    @Autowired
    private QuestionFingerprintMapper fingerprintMapper;

    @Autowired
    private FingerprintGenerator fingerprintGenerator;

    @Autowired
    private QuestionNormalizer questionNormalizer;
    @Override
    public String saveUserQuestion(String question, Long userId) {
        // 1. 标准化+生成指纹
        String normalized = questionNormalizer.normalize(question);
        String fingerprint = fingerprintGenerator.generateFingerprint(normalized);

        // 2. 查MySQL缓存
        QuestionFingerprint qf = findQfByFinger(fingerprint);

        // 3. 命中缓存直接返回
        if (qf != null && qf.getStatus() == 1) {
            // 异步更新统计
            fingerprintMapper.incrementHitCount(qf.getFingerprint());
            return qf.getCachedAnswer();
        }

        // 4. 检查相似问题（SimHash汉明距离）
        if (qf == null && hasSimilarCached(fingerprint)) {
            QuestionFingerprint similar = findSimilarCached(fingerprint);
            return similar.getCachedAnswer();
        }

        // 6. 保存或更新指纹记录
        if (qf == null) {
            // 新问题
            qf = new QuestionFingerprint();
            qf.setFingerprint(fingerprint);
            qf.setNormalizedText(normalized);
            qf.setTotalRatings(0);
            qf.setAvgRating(BigDecimal.ZERO);
            qf.setAskCount(1);
            qf.setStatus(0); // 待评分
            qf.setCreateTime(LocalDateTimeUtil.now());
            qf.setUpdateTime(LocalDateTimeUtil.now());
            qf.setIsDelete(0);
            fingerprintMapper.insert(qf);
        } else {
            // 已存在的问题，增加被问次数
            qf.setAskCount(qf.getAskCount() + 1);
            qf.setLastAskedTime(LocalDateTimeUtil.now());
            fingerprintMapper.update(qf);
        }

        return "";
    }

    @Override
    public QuestionFingerprint findQfByFinger(String fingerprint) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("fingerprint", fingerprint);
        QuestionFingerprint qf = fingerprintMapper.selectOneByQuery(queryWrapper);
        return qf;
    }


    @Override
    public String genFingerPrint(String message) {
        // 1. 标准化+生成指纹
        String normalized = questionNormalizer.normalize(message);
        return fingerprintGenerator.generateFingerprint(normalized);
    }

    /**
     * 检查是否有相似的已缓存问题
     */
    public boolean hasSimilarCached(String fingerprint) {
        return findSimilarCached(fingerprint) != null;
    }

    /**
     * 查找相似的已缓存问题
     */
    public QuestionFingerprint findSimilarCached(String fingerprint) {
        // 方案1：从数据库查询所有已缓存的问题指纹（适用于数据量不大的情况）
        List<QuestionFingerprint> cachedQuestions = fingerprintMapper.selectCachedQuestions();

        for (QuestionFingerprint cached : cachedQuestions) {
            // 计算汉明距离
            int distance = fingerprintGenerator.hammingDistance(fingerprint, cached.getFingerprint());
            if (distance <= SIMILARITY_THRESHOLD) {
                return cached;
            }
        }

        return null;
    }

    /**
     * 优化版：使用Redis缓存相似度索引
     */
    public QuestionFingerprint findSimilarCachedOptimized(String fingerprint) {
        // 1. 先查Redis是否有该指纹的相似索引
        // Set<String> similarFingerprints = redisTemplate.opsForSet().members("qa:similar:" + fingerprint);

        // 2. 如果有，直接返回
        // if (similarFingerprints != null && !similarFingerprints.isEmpty()) {
        //     String similarFp = similarFingerprints.iterator().next();
        //     return fingerprintMapper.selectByFingerprint(similarFp);
        // }

        // 3. 如果没有，计算相似度并缓存结果
        List<QuestionFingerprint> cachedQuestions = fingerprintMapper.selectCachedQuestions();

        for (QuestionFingerprint cached : cachedQuestions) {
            int distance = fingerprintGenerator.hammingDistance(fingerprint, cached.getFingerprint());
            if (distance <= SIMILARITY_THRESHOLD) {
                // 将结果缓存到Redis，TTL 7天
                // redisTemplate.opsForSet().add("qa:similar:" + fingerprint, cached.getFingerprint());
                // redisTemplate.expire("qa:similar:" + fingerprint, 7, TimeUnit.DAYS);
                return cached;
            }
        }

        return null;
    }
}
