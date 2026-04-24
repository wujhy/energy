package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 告警等级
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum AlarmLevelEnum {

    _0("0", "不告警"),
    _1("1", "一般"),
    _2("2", "严重"),
    _3("3", "紧急");

    private final String dictValue;

    private final String dictLabel;

    AlarmLevelEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 通过值查标签名
     */
    public static String findByValue(Object value) {
        for (AlarmLevelEnum dictEnum : AlarmLevelEnum.values()) {
            if (Objects.equals(dictEnum.getDictValue(), String.valueOf(value))) {
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
        for (AlarmLevelEnum dictEnum : AlarmLevelEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
