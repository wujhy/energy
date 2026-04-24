package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 数据类型
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum DataTypeEnum {

    _1(1, "开关量"),
    _2(2, "模拟量"),
    _3(3, "枚举量");

    private final Integer dictValue;

    private final String dictLabel;

    DataTypeEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 通过值查枚举
     */
    public static DataTypeEnum find(Integer dictValue) {
        for (DataTypeEnum dictEnum : DataTypeEnum.values()) {
            if (Objects.equals(dictEnum.getDictValue(), dictValue)) {
                return dictEnum;
            }
        }
        return _2;
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
        for (DataTypeEnum dictEnum : DataTypeEnum.values()) {
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
        for (DataTypeEnum dictEnum : DataTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
