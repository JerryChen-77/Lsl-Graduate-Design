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
 * 评分反馈表 实体类。
 *
 * @author SiLin li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rating_feedback")
public class RatingFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 问题指纹
     */
    private String fingerprint;

    /**
     * 评分用户ID
     */
    @Column("userId")
    private Long userId;

    @Column("chatId")
    private Long chatId;

    /**
     * 评分:1-5分
     */
    private Integer rating;

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
