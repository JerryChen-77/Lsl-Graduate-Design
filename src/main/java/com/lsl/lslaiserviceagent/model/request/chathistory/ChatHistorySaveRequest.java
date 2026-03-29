package com.lsl.lslaiserviceagent.model.request.chathistory;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
public class ChatHistorySaveRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 对话 id
     */
    private Long chatId;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型：user/ai
     */
    private String messageType;

    private String fingerPrint;

    /**
     * 操作用户 id（由切面注入）
     */
    private Long userId;

    private Long fatherId;
}


