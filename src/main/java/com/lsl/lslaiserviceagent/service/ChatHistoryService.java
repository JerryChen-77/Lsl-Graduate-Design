package com.lsl.lslaiserviceagent.service;


import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistoryQueryRequest;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistorySaveRequest;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

/**
 * 对话历史 服务层。
 *
 * @author Jiaxuan Chen
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 保存对话消息
     *
     * @param request 保存请求
     * @return 消息 id
     */
    Long saveChatMessage(ChatHistorySaveRequest request);


    boolean removeByChatId(Long chatId);

    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
