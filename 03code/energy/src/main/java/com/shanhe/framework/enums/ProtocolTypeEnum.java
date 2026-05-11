package com.shanhe.framework.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 协议解析类型
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum ProtocolTypeEnum {

    _1(1, "自定义协议"),
    _2(2, "标准modbus协议"),
    _3(3, "DL/T645-2007"),
    _99(99, "其他");

    private final Integer dictValue;

    private final String dictLabel;

    ProtocolTypeEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 查枚举
     */
    public static ProtocolTypeEnum find(Integer dictValue) {
        if (dictValue == null) {
            return _99;
        }
        for (ProtocolTypeEnum typeEnum : ProtocolTypeEnum.values()) {
            if (Objects.equals(typeEnum.getDictValue(), dictValue)) {
                return typeEnum;
            }
        }
        return _99;
    }

}
