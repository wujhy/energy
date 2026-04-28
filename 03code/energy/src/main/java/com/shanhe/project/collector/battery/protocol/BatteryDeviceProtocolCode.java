package com.shanhe.project.collector.battery.protocol;

import lombok.Getter;

/**
 * 600节采集模块端协议码表。
 * <p>
 * 仅保留与蓄电池设备直连采集相关的码值，不承载 980 节 PC/网络上层控制命令。
 */
@Getter
public enum BatteryDeviceProtocolCode {

    MODULE_INFO(0x01, 0x81, false, "获取采集模块信息"),
    SINGLE_BATTERY_IR_TEST(0x02, 0x82, true, "单电池监测模块内阻测试"),
    SINGLE_BATTERY_BALANCE(0x03, 0x83, true, "单电池监测模块均衡"),
    SET_MODULE_ADDRESS(0x08, 0x88, true, "设置模块地址"),
    CLEAR_SINGLE_DEBUG_DATA(0x0A, null, false, "清除电池组单体调试数据"),
    CONNECT_STRIP_RESISTANCE_TEST(0x0F, null, false, "连接条电阻测试"),
    GET_CONNECT_STRIP_RESISTANCE_VOLTAGE(0x11, 0x91, false, "获取连接条电阻测试电压"),
    SET_INTERNAL_RESISTANCE_COEFFICIENT(0x12, 0x92, true, "设置内阻系数"),
    AUTO_SET_MODULE_ADDRESS(0x18, 0xA8, false, "自动设置模块地址"),
    SET_CALIBRATION_PARAMETER(0x76, 0xF6, true, "设置校准参数（工厂用）");

    private final int requestCode;
    private final Integer responseCode;
    private final boolean statusResponse;
    private final String description;

    BatteryDeviceProtocolCode(int requestCode, Integer responseCode, boolean statusResponse, String description) {
        this.requestCode = requestCode;
        this.responseCode = responseCode;
        this.statusResponse = statusResponse;
        this.description = description;
    }

    public boolean matches(int code) {
        return requestCode == code || (responseCode != null && responseCode == code);
    }

    public boolean isRequest(int code) {
        return requestCode == code;
    }

    public boolean isResponse(int code) {
        return responseCode != null && responseCode == code;
    }

    public boolean hasResponseCode() {
        return responseCode != null;
    }

    public boolean isStatusResponse() {
        return statusResponse;
    }

    public static BatteryDeviceProtocolCode find(int code) {
        for (BatteryDeviceProtocolCode value : values()) {
            if (value.matches(code)) {
                return value;
            }
        }
        return null;
    }

    public static boolean isKnown(int code) {
        return find(code) != null;
    }
}
