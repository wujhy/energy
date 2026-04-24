package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 停止位
 *
 * @author wjh
 * @since 2025/3/17
 */
@Getter
public enum ParityBitsEnum {

    _0(0, "0：None"),
    _1(1, "1：Odd"),
    _2(2, "2：Even"),
    _3(3, "3：Mark"),
    _4(4, "4：Space");

    private final Integer dictValue;

    private final String dictLabel;

    ParityBitsEnum(Integer dictValue, String dictLabel) {
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
        for (ParityBitsEnum dictEnum : ParityBitsEnum.values()) {
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
        for (ParityBitsEnum dictEnum : ParityBitsEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
