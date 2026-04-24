package com.shanhe.framework.enums;

import lombok.Getter;

/**
 * tcp字符枚举
 *
 * @author wjh
 * @since 2025/3/17
 */
@Getter
public enum TcpCharEnum {

    HEAD_53("5354415254", "蓄电池指令53开始位置"),
    END_0D("0D", "蓄电池指令0D结束位置"),

    _FF("FF", "测试响应指令编号"),

    _AA("AA", "指令AA开始位置"),
    _55("55", "指令55结束位置");

    private final String dictValue;

    private final String dictLabel;

    TcpCharEnum(String dictValue, String dictLabel) {
        this.dictValue = dictValue;
        this.dictLabel = dictLabel;
    }
}
