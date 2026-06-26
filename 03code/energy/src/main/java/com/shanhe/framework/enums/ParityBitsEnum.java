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

    /** 无校验 */
    _0(0, "0：None"),
    /** 奇校验 */
    _1(1, "1：Odd"),
    /** 偶校验 */
    _2(2, "2：Even"),
    /** 标记校验 */
    _3(3, "3：Mark"),
    /** 空格校验 */
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
