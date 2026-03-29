package com.lsl.lslaiserviceagent.service;

import com.mybatisflex.core.service.IService;
import com.lsl.lslaiserviceagent.model.entity.QuestionFingerprint;

/**
 * 问题指纹表 服务层。
 *
 * @author SiLin li
 */
public interface QuestionFingerprintService extends IService<QuestionFingerprint> {

    String saveUserQuestion(String question,Long userId);

    QuestionFingerprint findQfByFinger(String fingerprint);

    String genFingerPrint(String message);
}
