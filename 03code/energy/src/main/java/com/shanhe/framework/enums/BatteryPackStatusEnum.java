package com.shanhe.framework.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 电池组状态枚举
 *
 * @author wjh
 * @since 2026-05-26
 */
@Getter
public enum BatteryPackStatusEnum {

    MONITOR("0", "监控"),
    CHARGE("1", "充电"),
    POWER_OFF("2", "停电"),
    CAPACITY_TEST("3", "核容"),
    DISCONNECTED("4", "未连接"),
    BACKUP("5", "备电"),
    IDLE("6", "空闲");

    private final String code;
    private final String label;

    BatteryPackStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static BatteryPackStatusEnum find(String code) {
        for (BatteryPackStatusEnum status : values()) {
            if (Objects.equals(status.code, code)) {
                return status;
            }
        }
        return null;
    }

    public static boolean isCode(String code, BatteryPackStatusEnum expected) {
        return Objects.equals(code, expected.code);
    }
}
