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
public enum BaudRateEnum {

    _1(1200, "1200"),
    _2(2400, "2400"),
    _3(4800, "4800"),
    _4(9600, "9600"),
    _5(19200, "19200"),
    _6(38400, "38400"),
    _7(57600, "57600"),
    _8(115200, "115200");

    private final Integer dictValue;

    private final String dictLabel;

    BaudRateEnum(Integer dictValue, String dictLabel) {
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
        for (BaudRateEnum dictEnum : BaudRateEnum.values()) {
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
        for (BaudRateEnum dictEnum : BaudRateEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
