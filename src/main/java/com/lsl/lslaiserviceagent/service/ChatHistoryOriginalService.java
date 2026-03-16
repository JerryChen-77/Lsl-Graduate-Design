package com.lsl.lslaiserviceagent.service;

import com.lsl.lslaiserviceagent.model.entity.ChatHistoryOriginal;
import com.mybatisflex.core.service.IService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 原始对话历史 服务层。
 * 为 vue 工程模式恢复对话记忆(包含工具调用信息)
 *
 * @author agx
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {

    /**
     * 添加对话历史
     * @param chatId
     * @param message
     * @param messageType
     * @param userId
     * @return
     */
    boolean addOriginalChatMessage(Long chatId, String message, String messageType, Long userId);

    /**
     * 批量添加对话历史
     * @param chatHistoryOriginalList
     * @return
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据 chatId 关联删除对话历史记录
     * @param chatId
     * @return
     */
    boolean deleteByChatId(Long chatId);

    /**
     * 将 某个对话 的对话历史加载到缓存中
     * @param chatId
     * @param chatMemory
     * @param maxCount
     * @return
     */
    int loadOriginalChatHistoryToMemory(Long chatId, MessageWindowChatMemory chatMemory, int maxCount);
}
