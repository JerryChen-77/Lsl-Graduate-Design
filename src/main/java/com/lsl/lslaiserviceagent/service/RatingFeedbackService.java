package com.lsl.lslaiserviceagent.service;

import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.request.rating.RateRequest;
import com.lsl.lslaiserviceagent.model.vo.ChatHistoryVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import com.lsl.lslaiserviceagent.model.entity.RatingFeedback;

/**
 * 评分反馈表 服务层。
 *
 * @author SiLin li
 */
public interface RatingFeedbackService extends IService<RatingFeedback> {

    Boolean rateAnswer(RateRequest request, Long userId);

    Page<ChatHistoryVO> wrapChatHistory(Page<ChatHistory> result);
}
