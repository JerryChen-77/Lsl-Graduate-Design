package com.lsl.lslaiserviceagent.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapResponse {

    /**
     * 返回状态：1为成功，0为失败
     */
    private String status;

    /**
     * 返回信息
     */
    private String info;

    /**
     * 返回编码
     */
    private String infocode;

    /**
     * 省份名称（成功时返回）
     */
    private String province;

    /**
     * 城市名称（成功时返回）
     */
    private String city;

    /**
     * 城市编码（成功时返回）
     */
    private String adcode;

    /**
     * 矩形区域范围（成功时返回）
     * 格式：左下角坐标;右上角坐标
     */
    private String rectangle;

    /**
     * 国家（部分接口可能返回）
     */
    private String country;

    /**
     * 详细地址（部分接口可能返回）
     */
    @JsonProperty("formatted_address")
    private String formattedAddress;

    /**
     * 业务状态码
     */
    @JsonProperty("code")
    private String businessCode;

    /**
     * 判断请求是否成功
     */
    public boolean isSuccess() {
        return "1".equals(status);
    }

    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        if (isSuccess()) {
            return null;
        }
        return String.format("错误码: %s, 信息: %s", infocode, info);
    }
}