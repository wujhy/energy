package com.shanhe.framework.enums;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.bean.Dict;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * IP地址类型
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum IpAddrEnum {

    _0("eth0", "ETH2", "有线连接 1"),
    _1("eth1", "ETH1", "有线连接 2");

    private final String dictValue;

    private final String dictLabel;
    private final String label;

    IpAddrEnum(String dictValue, String dictLabel, String label) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
        this.label = label;
    }

    /**
     * 通过值查标签名
     */
    public static String findLabelByValue(Object value) {
        String dictValue = (String) value;
        for (IpAddrEnum dictEnum : IpAddrEnum.values()) {
            if (StrUtil.equals(dictEnum.getDictValue(), dictValue)) {
                return dictEnum.getLabel();
            }
        }
        return null;
    }

    /**
     * 通过值查标签名
     */
    public static String findByValue(Object value) {
        String dictValue = (String) value;
        for (IpAddrEnum dictEnum : IpAddrEnum.values()) {
            if (StrUtil.equals(dictEnum.getDictValue(), dictValue)) {
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
        for (IpAddrEnum dictEnum : IpAddrEnum.values()) {
            list.add(new Dict(dictEnum.getDictLabel(), dictEnum.getDictValue()));
        }
        return list;
    }
}
