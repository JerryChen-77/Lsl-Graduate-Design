package com.lsl.lslaiserviceagent.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息 id
     */
    private Long id;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息类型：user/ai
     */
    private String messageType;

    /**
     * 对话 id
     */
    private Long chatId;


    /**
     * 关联用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}


