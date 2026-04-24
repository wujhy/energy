package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据精度
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum DataDecimalEnum {

    _0(0, "十六进制"),
    _1(1, "十进制"),
    _2(2, "二进制"),
    _3(3, "十六进制ASCII码"),
    _4(4, "浮点数（大端序）"),
    _5(5, "浮点数（小端序）"),
    _6(6, "U32"),
    _7(7, "U16"),
    _8(8, "I32"),
    _9(9, "I16"),
    _99(99, "其他");

    private final Integer dictValue;

    private final String dictLabel;

    DataDecimalEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 查枚举
     */
    public static DataDecimalEnum find(Integer dictValue) {
        if (dictValue == null) {
            return _99;
        }
        for (DataDecimalEnum typeEnum : DataDecimalEnum.values()) {
            if (Objects.equals(typeEnum.getDictValue(), dictValue)) {
                return typeEnum;
            }
        }
        return _99;
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
        for (DataDecimalEnum dictEnum : DataDecimalEnum.values()) {
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
        for (DataDecimalEnum dictEnum : DataDecimalEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
