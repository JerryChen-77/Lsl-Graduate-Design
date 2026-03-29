package com.lsl.lslaiserviceagent.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lsl.lslaiserviceagent.exception.ErrorCode;
import com.lsl.lslaiserviceagent.mapper.ChatHistoryMapper;
import com.lsl.lslaiserviceagent.model.entity.ChatHistory;
import com.lsl.lslaiserviceagent.model.enums.ChatHistoryMessageTypeEnum;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistoryQueryRequest;
import com.lsl.lslaiserviceagent.model.request.chathistory.ChatHistorySaveRequest;
import com.lsl.lslaiserviceagent.model.vo.ChatMessageVO;
import com.lsl.lslaiserviceagent.service.ChatHistoryService;
import com.lsl.lslaiserviceagent.service.UserService;
import com.lsl.lslaiserviceagent.utils.common.ThrowUtils;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 对话历史 服务层实现。
 *
 * @author Jiaxuan Chen
 */
@Service
@Slf4j
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService {

    @Resource
    private UserService userService;

    @Override
    public Long saveChatMessage(ChatHistorySaveRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        ThrowUtils.throwIf(request.getChatId() == null, ErrorCode.PARAMS_ERROR, "对话标识不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getMessage()), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(request.getMessageType()), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(request.getUserId() == null, ErrorCode.PARAMS_ERROR, "用户信息缺失");

        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(request.getMessageType());
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.PARAMS_ERROR, "消息类型非法");


        ChatHistory chatHistory = ChatHistory.builder()
                .fingerprint(request.getFingerPrint())
                .chatId(request.getChatId())
                .message(request.getMessage())
                .messageType(messageTypeEnum.getValue())
                .userId(request.getUserId())
                .fatherId(request.getFatherId())
                .build();

        boolean saveResult = this.save(chatHistory);
        ThrowUtils.throwIf(!saveResult, ErrorCode.DB_OPERATION_ERROR, "保存对话历史失败");
        return chatHistory.getId();
    }


    @Override
    public boolean removeByChatId(Long chatId) {
        try{
            ThrowUtils.throwIf(chatId == null, ErrorCode.PARAMS_ERROR, "对话记录不能为空");
            QueryWrapper queryWrapper = QueryWrapper.create()
                    .eq("chatId", chatId);
            boolean removeResult = this.remove(queryWrapper);
            if (!removeResult) {
                long remain = this.count(queryWrapper);
                ThrowUtils.throwIf(remain > 0, ErrorCode.DB_OPERATION_ERROR, "对话历史清理失败");
            }
        }catch (Exception e){
            log.error("removeByChat清理对话历史失败", e);
        }
        return true;
    }

    @Override
    public String getChatAnswer(Long fatherChatId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("fatherId",fatherChatId);
        return this.getOne(queryWrapper).getMessage();
    }

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        if (chatHistoryQueryRequest == null) {
            return queryWrapper;
        }
        Long chatId = chatHistoryQueryRequest.getChatId();
        Long id = chatHistoryQueryRequest.getId();
        String message = chatHistoryQueryRequest.getMessage();
        String messageType = chatHistoryQueryRequest.getMessageType();
        Long userId = chatHistoryQueryRequest.getUserId();
        LocalDateTime lastCreateTime = chatHistoryQueryRequest.getLastCreateTime();
        String sortField = chatHistoryQueryRequest.getSortField();
        String sortOrder = chatHistoryQueryRequest.getSortOrder();
        // 拼接查询条件
        queryWrapper.eq("chatId", chatId)
                .eq("id", id)
                .like("message", message)
                .eq("messageType", messageType)
                .eq("userId", userId);
        // 游标查询逻辑 - 只使用 createTime 作为游标
        if (lastCreateTime != null) {
            queryWrapper.lt("createTime", lastCreateTime);
        }
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(sortField, "ascend".equals(sortOrder));
        } else {
            // 默认按创建时间降序排列
            queryWrapper.orderBy("createTime", false);
        }
        return queryWrapper;
    }


    private ChatMessageVO buildChatMessageVO(ChatHistory chatHistory) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(chatHistory.getId());
        vo.setMessage(chatHistory.getMessage());
        vo.setMessageType(chatHistory.getMessageType());
        vo.setChatId(chatHistory.getChatId());
        vo.setUserId(chatHistory.getUserId());
        vo.setCreateTime(chatHistory.getCreateTime());
        return vo;
    }

    private Page<ChatMessageVO> emptyPage(Page<ChatHistory> historyPage) {
        Page<ChatMessageVO> voPage = new Page<>(historyPage.getPageNumber(), historyPage.getPageSize(), historyPage.getTotalRow());
        voPage.setTotalPage(historyPage.getTotalPage());
        voPage.setRecords(Collections.emptyList());
        return voPage;
    }

    private Page<ChatMessageVO> buildPage(Page<ChatHistory> historyPage, List<ChatMessageVO> records) {
        Page<ChatMessageVO> voPage = new Page<>(historyPage.getPageNumber(), historyPage.getPageSize(), historyPage.getTotalRow());
        voPage.setTotalPage(historyPage.getTotalPage());
        voPage.setRecords(records);
        return voPage;
    }
}
