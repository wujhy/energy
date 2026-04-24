package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 电池品牌
 *
 * @author wjh
 * @since 2025/7/15
 */
@Getter
public enum BatteryBrandEnum {

    _1(1, "理士"),
    _2(2, "杰士"),
    _3(3, "风帆"),
    _4(4, "天能"),
    _5(5, "圣阳"),
    _6(6, "科华"),
    _7(7, "瓦尔塔"),
    _8(8, "骆驼"),
    _9(9, "汤浅"),
    _10(10, "超威"),
    _11(11, "科士达"),
    _99(99, "其他");

    private final Integer dictValue;

    private final String dictLabel;

    BatteryBrandEnum(Integer dictValue, String dictLabel) {
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
        for (BatteryBrandEnum dictEnum : BatteryBrandEnum.values()) {
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
        for (BatteryBrandEnum dictEnum : BatteryBrandEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
