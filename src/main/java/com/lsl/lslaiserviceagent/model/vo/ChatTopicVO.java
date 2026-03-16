package com.lsl.lslaiserviceagent.model.vo;

import com.mybatisflex.annotation.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatTopicVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对话id
     */
    private Long chatId;

    /**
     * 对话内容概括
     */
    private String content;


    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;
}
