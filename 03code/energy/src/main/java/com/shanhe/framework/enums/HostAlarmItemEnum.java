package com.shanhe.framework.enums;

import lombok.Getter;

/**
 * 主机告警项
 *
 * @author wjh
 * @since 2024/12/19
 */
@Getter
public enum HostAlarmItemEnum {

    _1("txzt", "主机故障！", "2"),
    _2("lsgj", "漏水告警", "2"),
    _3("yggj", "冒烟告警", "2"),
    _4("sdjcgj", "市电异常告警", "2"),
    _5("kggj", "开关异常告警", "2"),
    _6("txzt", "通讯异常", "2");

    private final String code;

    private final String name;
    private final String level;

    HostAlarmItemEnum(String code, String name, String level) {
        this.code = code;
        this.name = name;
        this.level = level;
    }
}
