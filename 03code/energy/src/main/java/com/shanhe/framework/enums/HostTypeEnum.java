package com.shanhe.framework.enums;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 主机类型
 *
 * @author wjh
 * @since 2025/4/18
 */
@Getter
public enum HostTypeEnum {

    _3CM02N("3CM02N", "3CM02N透传版动环主机"),
    _2CM03N("2CM03N", "2CM03N透传版动环主机");

    private final String dictValue;

    private final String dictLabel;

    HostTypeEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }

    /**
     * 转枚举
     */
    public static HostTypeEnum fromCode(String code) {
        if (StrUtil.isBlank(code)) {
            return _2CM03N;
        }
        for (HostTypeEnum item : values()) {
            if (item.getDictValue().equals(code)) {
                return item;
            }
        }
        return _2CM03N;
    }

    /**
     * 通过值查标签名
     */
    public static String findByValue(Object value) {
        String dictValue;
        if (value instanceof String) {
            dictValue = (String) value;
        } else {
            dictValue = value + "";
        }
        for (HostTypeEnum dictEnum : HostTypeEnum.values()) {
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
        for (HostTypeEnum dictEnum : HostTypeEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
