package com.lsl.lslaiserviceagent.mapper;

import com.mybatisflex.core.BaseMapper;
import com.lsl.lslaiserviceagent.model.entity.QuestionFingerprint;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 问题指纹表 映射层。
 *
 * @author SiLin li
 */
public interface QuestionFingerprintMapper extends BaseMapper<QuestionFingerprint> {

    void incrementHitCount(String fingerprint);

    /**
     * 根据指纹查询
     */
    @Select("SELECT * FROM question_fingerprint WHERE fingerprint = #{fingerprint} AND isDelete = 0")
    QuestionFingerprint selectByFingerprint(String fingerprint);

    /**
     * 查询所有已缓存的问题
     */
    @Select("SELECT * FROM question_fingerprint WHERE status = 1 AND isDelete = 0")
    List<QuestionFingerprint> selectCachedQuestions();
}
