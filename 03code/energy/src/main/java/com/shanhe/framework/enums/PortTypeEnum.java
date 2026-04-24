package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 是否枚举
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum PortTypeEnum {

    _0(0, "无效"),
    _1(1, "RS485"),
    _2(2, "RS232"),
    _3(3, "DI"),
    _4(4, "DO"),
    _5(5, "AI"),
    _6(6, "AO"),
    _7(7, "IR红外");

    private final Integer dictValue;

    private final String dictLabel;

    PortTypeEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 通过值查标签名
     */
    public static String findByValue(Object value) {
        Integer dictValue;
        if (value instanceof String) {
            dictValue = Integer.valueOf((String) value);
        } else {
            dictValue = (Integer) value;
        }
        for (PortTypeEnum dictEnum : PortTypeEnum.values()) {
            if (Objects.equals(dictEnum.getDictValue(), dictValue)) {
                return dictEnum.getDictLabel();
            }
        }
        return null;
    }

    /**
     * 转list
     */
    public static List<Dict> getDictList() {
        List<Dict> list = new ArrayList<>();
        for (PortTypeEnum dictEnum : PortTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
