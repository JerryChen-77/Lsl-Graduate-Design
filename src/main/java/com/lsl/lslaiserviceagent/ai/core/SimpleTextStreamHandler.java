package com.lsl.lslaiserviceagent.ai.core;


import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.enums.ChatHistoryMessageTypeEnum;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistorySaveRequest;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 简单文本流处理器
 */
@Slf4j
@Component
public class SimpleTextStreamHandler {

    /**
     * 处理普通AI返回流
     * 直接收集完整的文本响应
     *
     * @param originFlux         原始流
     * @param chatHistoryService 聊天历史服务
     * @param chatId              对话Id
     * @param loginUser          登录用户
     * @return 处理后的流
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long chatId, User loginUser,long fatherId) {
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // 收集AI响应内容
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // 流式响应完成后，添加AI消息到对话历史
                    String aiResponse = aiResponseBuilder.toString();
                    ChatHistorySaveRequest req = ChatHistorySaveRequest.builder()
                            .chatId(chatId)
                            .fatherId(fatherId)
                            .message(aiResponse)
                            .userId(loginUser.getId())
                            .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                            .build();
                    chatHistoryService.saveChatMessage(req);
                })
                .doOnError(error -> {
                    // 如果AI回复失败，也要记录错误消息
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    ChatHistorySaveRequest req = ChatHistorySaveRequest.builder()
                            .chatId(chatId)
                            .fatherId(fatherId)
                            .message(errorMessage)
                            .userId(loginUser.getId())
                            .messageType(ChatHistoryMessageTypeEnum.AI.getValue())
                            .build();
                    chatHistoryService.saveChatMessage(req);
                });
    }
}
