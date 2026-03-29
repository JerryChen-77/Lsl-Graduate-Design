package com.lsl.lslaiserviceagent.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 问题指纹表 实体类。
 *
 * @author SiLin li
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("question_fingerprint")
public class QuestionFingerprint implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 问题指纹(SHA256/SimHash)
     */
    private String fingerprint;

    /**
     * 标准化后的问题文本
     */
    @Column("normalizedText")
    private String normalizedText;

    /**
     * 缓存的答案内容(仅status=1时有值)
     */
    @Column("cachedAnswer")
    private String cachedAnswer;

    /**
     * 状态:0-待评分,1-已缓存,2-不合格,3-已过期
     */
    private Integer status;

    /**
     * 当前平均评分
     */
    @Column("avgRating")
    private BigDecimal avgRating;

    /**
     * 总评分次数
     */
    @Column("totalRatings")
    private Integer totalRatings;

    /**
     * 最后被问时间
     */
    @Column("lastAskedTime")
    private LocalDateTime lastAskedTime;

    /**
     * 被问总次数
     */
    @Column("askCount")
    private Integer askCount;

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
