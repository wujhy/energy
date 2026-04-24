package com.shanhe.framework.enums;

import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 协议解析类型
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum ProtocolAlgorithmEnum {
    _0(0, "无校验"),
    _1(1, "CRC16"),
    _2(2, "杉和蓄电池算法"),
    _3(3, "模256的和");

    private final Integer dictValue;

    private final String dictLabel;

    ProtocolAlgorithmEnum(Integer dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 查枚举
     */
    public static ProtocolAlgorithmEnum find(Integer dictValue) {
        if (dictValue == null) {
            return _0;
        }
        for (ProtocolAlgorithmEnum typeEnum : ProtocolAlgorithmEnum.values()) {
            if (Objects.equals(typeEnum.getDictValue(), dictValue)) {
                return typeEnum;
            }
        }
        return _0;
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
        for (ProtocolAlgorithmEnum dictEnum : ProtocolAlgorithmEnum.values()) {
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
        for (ProtocolAlgorithmEnum dictEnum : ProtocolAlgorithmEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
