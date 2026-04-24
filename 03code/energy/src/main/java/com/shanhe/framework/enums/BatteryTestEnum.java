package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 蓄电池测试类型
 *
 * @author wjh
 * @since 2025/8/7
 */
@Getter
public enum BatteryTestEnum {

    _1(1, "内阻测试"),
    _2(2, "连接条电阻测试"),
    _3(3, "核容测试"),
    _4(4, "浮充测试"),
    _5(5, "备电时长测试"),
    _6(6, "单节内阻测试"),
    _7(7, "充电测试"),
    _99(99, "其他");

    private final Integer dictValue;

    private final String dictLabel;

    BatteryTestEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    public static BatteryTestEnum find(Integer dictValue) {
        for (BatteryTestEnum testEnum : BatteryTestEnum.values()) {
            if (Objects.equals(testEnum.getDictValue(), dictValue)) {
                return testEnum;
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
        for (BatteryTestEnum dictEnum : BatteryTestEnum.values()) {
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
        for (BatteryTestEnum dictEnum : BatteryTestEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
