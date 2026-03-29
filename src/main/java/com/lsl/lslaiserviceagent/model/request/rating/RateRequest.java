package com.lsl.lslaiserviceagent.model.request.rating;

import lombok.Data;

import java.io.Serializable;

@Data
public class RateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * answer from ai
     */
    private long chatId;

    /**
     * 1-5 point
     */
    private Integer score;
}
