package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 设备类型枚举
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum DeviceTypeEnum {

    _0(0, "主机"),
    _1(1, "蓄电池"),
    _2(2, "UPS"),
    _3(3, "温湿度计"),
    _4(4, "空调"),
    _5(5, "漏水"),
    _6(6, "开关"),
    _7(7, "烟感"),
    _8(8, "市电"),
    _9(9, "红外感应"),
    _11(11, "精密空调"),
    _12(12, "空调控制器"),
    _13(13, "电量仪"),
    _14(14, "氢气"),
    _99(99, "其他");

    private final Integer dictValue;

    private final String dictLabel;

    DeviceTypeEnum(Integer dictValue, String dictLabel) {
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
        for (DeviceTypeEnum dictEnum : DeviceTypeEnum.values()) {
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
        for (DeviceTypeEnum dictEnum : DeviceTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
