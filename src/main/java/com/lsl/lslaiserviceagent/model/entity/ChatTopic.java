package com.lsl.lslaiserviceagent.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对话主题表 实体类。
 *
 * @author SiLin li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_topic")
public class ChatTopic implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 对话主题id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 对话内容概括
     */
    private String content;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
