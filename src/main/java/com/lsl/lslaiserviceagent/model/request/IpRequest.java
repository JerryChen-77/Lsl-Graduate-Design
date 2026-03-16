package com.lsl.lslaiserviceagent.model.request;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.Data;

@Data
public class IpRequest {
    /**
     * IP地址
     */
    @Hidden
    private String ip;
}
