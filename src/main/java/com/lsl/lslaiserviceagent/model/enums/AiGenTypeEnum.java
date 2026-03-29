package com.lsl.lslaiserviceagent.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum AiGenTypeEnum {

    COMMON_CONVERSATION("普通对话模式", "common-conversation"),
    CACHED_CONVERSATION("缓存模式","cache-model");
    private final String text;
    private final String value;

    AiGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static AiGenTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AiGenTypeEnum anEnum : AiGenTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
