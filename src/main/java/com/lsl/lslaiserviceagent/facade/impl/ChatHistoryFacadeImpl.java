package com.lsl.lslaiserviceagent.facade.impl;


import cn.hutool.core.util.StrUtil;
import com.lsl.lslaiserviceagent.ai.core.StreamHandlerExecutor;
import com.lsl.lslaiserviceagent.ai.service.AiGeneratorFacade;
import com.lsl.lslaiserviceagent.constant.UserConstant;
import com.lsl.lslaiserviceagent.exception.BusinessException;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.facade.ChatHistroyFacade;
import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.entity.ChatTopic;
import com.lsl.lslaiserviceagent.model.entity.User;
import com.lsl.lslaiserviceagent.model.enums.AiGenTypeEnum;
import com.lsl.lslaiserviceagent.model.enums.ChatHistoryMessageTypeEnum;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistoryQueryRequest;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistorySaveRequest;
import com.lsl.lslaiserviceagent.service.ChatHistoryOriginalService;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import com.lsl.lslaiserviceagent.service.ChatTopicService;
import com.lsl.lslaiserviceagent.utils.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ChatHistoryFacadeImpl implements ChatHistroyFacade {
    @Autowired
    private ChatHistoryService chatHistoryService;

    @Autowired
    private ChatTopicService chatTopicService;

    @Autowired
    private ChatHistoryOriginalService chatHistoryOriginalService;

    @Autowired
    private AiGeneratorFacade aiGeneratorFacade;

    @Autowired
    private StreamHandlerExecutor streamHandlerExecutor;
    @Override
    public boolean deleteChatByChatId(Long chatId) {
        // 必须删掉topic
        chatTopicService.removeByChatId(chatId);
        chatHistoryService.removeByChatId(chatId);
        chatHistoryOriginalService.deleteByChatId(chatId);
        return true;
    }


    @Override
    public Page<ChatHistory> listAppChatHistoryByPage(Long chatId, int pageSize, LocalDateTime lastCreateTime, User loginUser) {
            ThrowUtils.throwIf(chatId == null || chatId <= 0, ErrorCode.PARAMS_ERROR, "对话ID不能为空");
            ThrowUtils.throwIf(pageSize <= 0 || pageSize > 50, ErrorCode.PARAMS_ERROR, "页面大小必须在1-50之间");
            ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

            ChatTopic chatTopic = chatTopicService.getById(chatId);
            ThrowUtils.throwIf(chatTopic == null, ErrorCode.NOT_FOUND_ERROR, "对话不存在");
            boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
            boolean isCreator = chatTopic.getUserId().equals(loginUser.getId());
            ThrowUtils.throwIf(!isAdmin && !isCreator, ErrorCode.NO_AUTH_ERROR, "无权查看该对话历史");
            // 构建查询条件
            ChatHistoryQueryRequest queryRequest = new ChatHistoryQueryRequest();
            queryRequest.setChatId(chatId);
            queryRequest.setLastCreateTime(lastCreateTime);
            QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(queryRequest);
            // 查询数据
            return chatHistoryService.page(Page.of(1, pageSize), queryWrapper);
    }

    @Override
    public Page<ChatHistory> listAllChatHistoryByPageForAdmin(long pageNum, long pageSize, ChatHistoryQueryRequest chatHistoryQueryRequest) {
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        return chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);
    }


    @Override
    public int loadChatHistoryToMemory(Long chatId, MessageWindowChatMemory chatMemory, int maxCount) {
        return chatHistoryOriginalService.loadOriginalChatHistoryToMemory(chatId, chatMemory, maxCount);
    }

    @Override
    public Flux<String> chat(Long chatId, String message, User loginUser) {
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        ThrowUtils.throwIf(chatId == null||chatId<0, ErrorCode.NOT_FOUND_ERROR, "对话不存在");
        // 校验完成后保存用户对话
        ChatHistorySaveRequest chatHistorySaveRequest = ChatHistorySaveRequest.builder()
                .chatId(chatId)
                .message(message)
                .messageType(ChatHistoryMessageTypeEnum.USER.getValue())
                .userId(loginUser.getId())
                .build();
        chatHistoryService.saveChatMessage(chatHistorySaveRequest);
        chatHistoryOriginalService.addOriginalChatMessage(chatId,message, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
        // AI生成对话
        Flux<String> originFlux = aiGeneratorFacade.generateAiAnswerStream(chatId, message, AiGenTypeEnum.COMMON_CONVERSATION);
        return streamHandlerExecutor.doExecute(originFlux,chatHistoryService,chatHistoryOriginalService,chatId,loginUser,AiGenTypeEnum.COMMON_CONVERSATION);
    }
}
