package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 操作结果枚举
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum ResultEnum {

    TRUE(0, "成功"),
    FALSE(1, "失败");

    private final Integer dictValue;

    private final String dictLabel;

    ResultEnum(Integer dictValue, String dictLabel) {
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
        for (ResultEnum dictEnum : ResultEnum.values()) {
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
        for (ResultEnum dictEnum : ResultEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
