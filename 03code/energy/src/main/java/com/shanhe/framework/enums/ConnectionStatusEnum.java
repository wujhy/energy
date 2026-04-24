package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 连接状态
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum ConnectionStatusEnum {

    _0(0, "未连接"),
    _1(1, "已连接"),
    _2(2, "连接异常");

    private final Integer dictValue;

    private final String dictLabel;

    ConnectionStatusEnum(Integer dictValue, String dictLabel) {
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
        } else if (value instanceof Long) {
            dictValue =((Long) value).intValue();
        } else {
            dictValue = (Integer) value;
        }
        for (ConnectionStatusEnum dictEnum : ConnectionStatusEnum.values()) {
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
        for (ConnectionStatusEnum dictEnum : ConnectionStatusEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
