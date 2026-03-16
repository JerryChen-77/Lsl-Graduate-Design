package com.lsl.lslaiserviceagent.model.enums;

import lombok.Getter;


@Getter
public enum AIToolsEnum {
    IP_ANALYZE_UTIL("IP_ANALYZE","Ip解析工具");

    private String value;

    private String desc;

    AIToolsEnum(String value, String desc){
        this.value = value;
        this.desc = desc;
    }

    public static AIToolsEnum getEnumByValue(String value){
        for(AIToolsEnum e : AIToolsEnum.values()){
            if(e.getValue().equals(value)){
                return e;
            }
        }
        return null;
    }

}
