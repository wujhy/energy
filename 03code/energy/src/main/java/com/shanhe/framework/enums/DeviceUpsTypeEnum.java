package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * UPS类型枚举
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum DeviceUpsTypeEnum {

    _1(1, "单相UPS"),
    _2(2, "三相UPS");

    private final Integer dictValue;

    private final String dictLabel;

    DeviceUpsTypeEnum(Integer dictValue, String dictLabel) {
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
        for (DeviceUpsTypeEnum dictEnum : DeviceUpsTypeEnum.values()) {
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
        for (DeviceUpsTypeEnum dictEnum : DeviceUpsTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
