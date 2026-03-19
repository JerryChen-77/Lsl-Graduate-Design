package com.lsl.lslaiserviceagent.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
public enum OperatorEnum {

    ChinaMobile("ChinaMobile", "https://www.10086.cn/", "中国移动"),
    ChinaUnicom("ChinaUnicom", "https://mall.10010.com/", "中国联通"),
    ChinaTelecom("ChinaTelecom", null, "中国电信");

    private final String type;

    private final String url;

    private final String name;

    OperatorEnum(String type, String url, String name) {
        this.type = type;
        this.url = url;
        this.name = name;
    }

    public static OperatorEnum getOperatorEnum(String type) {
        for (OperatorEnum operatorEnum : OperatorEnum.values()) {
            if (operatorEnum.getType().equals(type)) {
                return operatorEnum;
            }
        }
        return null;
    }

    public static OperatorEnum getOperatorEnumByUrl(String url) {
        if(StringUtils.isBlank(url)){
            return ChinaTelecom;
        }
        for (OperatorEnum operatorEnum : OperatorEnum.values()) {
            if (operatorEnum.getUrl().equals(url)) {
                return operatorEnum;
            }
        }
        return null;
    }

    public static List<String> getOperatorUrls(){
        return List.of(ChinaMobile.getUrl(), ChinaUnicom.getUrl(), ChinaTelecom.getUrl());
    }

}
