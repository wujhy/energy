package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 电池规格
 *
 * @author wjh
 * @since 2025/3/17
 */
@Getter
public enum BatteryModelEnum {

    // 浮充阶段统计计算的是电池组中 最高单体电压 与 最低单体电压 之间的差值
    //      2V      异常 > 90 mV （24节以下） 异常 > 200 mV（24节以上）
    //      12V     异常 > 120 mV （24节以下） 异常 > 480 mV （24节以上）

    //    _1(1, "1.2V"),
    _2(2, "2V", 90, 200),
    //    _3(3, "2.4V"),
//    _4(4, "3.2V"),
//    _5(5, "3.9V"),
//    _6(6, "4.8V"),
//    _7(7, "6V"),
    _8(8, "12V", 120, 480);

    private final Integer dictValue;

    private final String dictLabel;
    // 浮充 24 节以下下限
    private final Integer floatingVoltage24Below;
    // 浮充 24 节以上上限
    private final Integer floatingVoltage24Above;

    BatteryModelEnum(Integer dictValue, String dictLabel, Integer floatingVoltage24Below, Integer floatingVoltage24Above) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
        this.floatingVoltage24Below = floatingVoltage24Below;
        this.floatingVoltage24Above = floatingVoltage24Above;
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
        for (BatteryModelEnum dictEnum : BatteryModelEnum.values()) {
            if (Objects.equals(dictEnum.getDictValue(), dictValue)) {
                return dictEnum.getDictLabel();
            }
        }
        return null;
    }


    public static BatteryModelEnum find(Integer dictValue) {
        for (BatteryModelEnum testEnum : BatteryModelEnum.values()) {
            if (Objects.equals(testEnum.getDictValue(), dictValue)) {
                return testEnum;
            }
        }
        return _2;
    }
    /**
     * 转list
     */
    public static List<Dict> getDictList() {
        List<Dict> list = new ArrayList<>();
        for (BatteryModelEnum dictEnum : BatteryModelEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
