package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 校验类型
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum CheckTypeEnum {

    _1(1, "CRC校验"),
    _2(2, "LRC校验");

    private final Integer dictValue;

    private final String dictLabel;

    CheckTypeEnum(Integer dictValue, String dictLabel) {
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
        for (CheckTypeEnum dictEnum : CheckTypeEnum.values()) {
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
        for (CheckTypeEnum dictEnum : CheckTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
