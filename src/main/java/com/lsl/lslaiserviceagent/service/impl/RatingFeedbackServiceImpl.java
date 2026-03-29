package com.lsl.lslaiserviceagent.service.impl;

import com.lsl.lslaiserviceagent.exception.BusinessException;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.mapper.QuestionFingerprintMapper;
import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.entity.QuestionFingerprint;
import com.lsl.lslaiserviceagent.model.request.rating.RateRequest;
import com.lsl.lslaiserviceagent.model.vo.ChatHistoryVO;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import com.lsl.lslaiserviceagent.service.QuestionFingerprintService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lsl.lslaiserviceagent.model.entity.RatingFeedback;
import com.lsl.lslaiserviceagent.mapper.RatingFeedbackMapper;
import com.lsl.lslaiserviceagent.service.RatingFeedbackService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评分反馈表 服务层实现。
 *
 * @author SiLin li
 */
@Service
public class RatingFeedbackServiceImpl extends ServiceImpl<RatingFeedbackMapper, RatingFeedback>  implements RatingFeedbackService{

    @Autowired
    private RatingFeedbackMapper ratingFeedbackMapper;

    @Autowired
    private QuestionFingerprintMapper questionFingerprintMapper;

    @Autowired
    private QuestionFingerprintService questionFingerprintService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    @Override
    public Boolean rateAnswer(RateRequest request, Long userId) {
        long chatId = request.getChatId();
        Integer score = request.getScore();
        // 找fatherId
        ChatHistory answerChat = chatHistoryService.getById(chatId);
        // chatId获取 questionFinger
        Long questionChatId = answerChat.getFatherId();
        ChatHistory questionChat = chatHistoryService.getById(questionChatId);

        QueryWrapper qw = new QueryWrapper();
        qw.eq("chatId", chatId)
                .eq("userId",userId);
        RatingFeedback ratingFeedback = this.getOne(qw);
        if(ratingFeedback != null){
            throw new BusinessException(ErrorCode.REPEAT_RATE_ERROR);
        }
        return rate(questionChat.getFingerprint(),userId,chatId,score,answerChat.getMessage());
    }

    @Override
    public Page<ChatHistoryVO> wrapChatHistory(Page<ChatHistory> result) {
        List<ChatHistoryVO> wrappedRecords = result.getRecords().stream()
                .map(chatHistory -> {
                    ChatHistoryVO chatHistoryVO = new ChatHistoryVO();
                    BeanUtils.copyProperties(chatHistory, chatHistoryVO);
                    // fill Rate
                    QueryWrapper qw = new QueryWrapper();
                    qw.eq("chatId", chatHistory.getId());
                    RatingFeedback ratingFeedback = this.getOne(qw);
                    if(ratingFeedback != null){
                        chatHistoryVO.setRates(ratingFeedback.getRating());
                    }
                    return chatHistoryVO;
                }).collect(Collectors.toList());
        return new Page<>(wrappedRecords,result.getPageNumber(),result.getPageSize(), result.getTotalRow());
    }

    // 用户评分（可选）
    private boolean rate(String fingerprint, Long userId,Long chatId, Integer rating,String cachedAnswer) {
        // 1. 保存评分记录
        RatingFeedback feedback = new RatingFeedback();
        feedback.setFingerprint(fingerprint);
        feedback.setUserId(userId);
        feedback.setRating(rating);
        feedback.setChatId(chatId);
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setUpdateTime(LocalDateTime.now());
        feedback.setIsDelete(0);
        ratingFeedbackMapper.insert(feedback);

        // 2. 更新指纹表的评分统计
        QuestionFingerprint qf = questionFingerprintMapper.selectByFingerprint(fingerprint);
        // 正确写法
        BigDecimal newAvg;
        if (qf.getAvgRating() == null) {
            newAvg = BigDecimal.valueOf(rating);
        } else {
            // 计算总分 = 平均分 * 次数
            BigDecimal totalScore = qf.getAvgRating()
                    .multiply(BigDecimal.valueOf(qf.getTotalRatings()))
                    .add(BigDecimal.valueOf(rating));

            // 计算新平均分 = 总分 / (次数 + 1)
            newAvg = totalScore.divide(
                    BigDecimal.valueOf(qf.getTotalRatings() + 1),
                    2,  // 保留2位小数
                    RoundingMode.HALF_UP  // 四舍五入
            );
        }
        qf.setAvgRating(newAvg);
        qf.setTotalRatings(qf.getTotalRatings() + 1);

        // 3. 判断是否达到缓存条件且暂时无缓存
        if (qf.getTotalRatings() >= 3 && qf.getAvgRating().compareTo(BigDecimal.valueOf(4.0)) >= 0 && qf.getStatus() == 0) {
            qf.setCachedAnswer(cachedAnswer);
            qf.setStatus(1); // 已缓存
        }else if(qf.getTotalRatings() >= 3 && qf.getStatus() == 1 && qf.getAvgRating().compareTo(BigDecimal.valueOf(4.0)) < 0){
            // 如果评价分数下降，需要修改状态，改成不缓存
            qf.setStatus(0);
        }
        int update = questionFingerprintMapper.update(qf);
        return update == 1;
    }
}
