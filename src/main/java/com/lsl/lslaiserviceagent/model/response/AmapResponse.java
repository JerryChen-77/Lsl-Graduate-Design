package com.lsl.lslaiserviceagent.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lsl.lslaiserviceagent.utils.FlexibleListDeserializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AmapResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private String info;
    private String infocode;

    /**
     * 省份名称（兼容字符串和数组）
     */
    @JsonDeserialize(using = FlexibleListDeserializer.class)
    private List<String> province;

    /**
     * 城市名称（兼容字符串和数组）
     */
    @JsonDeserialize(using = FlexibleListDeserializer.class)
    private List<String> city;

    /**
     * 城市编码（兼容字符串和数组）
     */
    @JsonDeserialize(using = FlexibleListDeserializer.class)
    private List<String> adcode;

    /**
     * 矩形区域范围（兼容字符串和数组）
     */
    @JsonDeserialize(using = FlexibleListDeserializer.class)
    private List<String> rectangle;

    private String country;

    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("code")
    private String businessCode;

    public boolean isSuccess() {
        return "1".equals(status);
    }

    public String getErrorMessage() {
        if (isSuccess()) {
            return null;
        }
        return String.format("错误码: %s, 信息: %s", infocode, info);
    }

    // ========== 便捷方法 ==========

    /**
     * 获取省份名称
     */
    public String getProvinceString() {
        return (province != null && !province.isEmpty()) ? province.get(0) : null;
    }

    /**
     * 获取城市名称
     */
    public String getCityString() {
        return (city != null && !city.isEmpty()) ? city.get(0) : null;
    }

    /**
     * 获取城市编码
     */
    public String getAdcodeString() {
        return (adcode != null && !adcode.isEmpty()) ? adcode.get(0) : null;
    }

    /**
     * 获取矩形范围
     */
    public String getRectangleString() {
        return (rectangle != null && !rectangle.isEmpty()) ? rectangle.get(0) : null;
    }

    /**
     * 判断是否为海外IP（province为空数组）
     */
    public boolean isOverseas() {
        return province != null && province.isEmpty();
    }

    /**
     * 判断是否为局域网
     */
    public boolean isLan() {
        return "局域网".equals(getProvinceString());
    }
}