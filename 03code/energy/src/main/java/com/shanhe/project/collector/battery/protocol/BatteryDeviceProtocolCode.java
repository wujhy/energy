package com.shanhe.project.collector.battery.protocol;

import lombok.Getter;

/**
 * 600节采集模块端协议码表。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Getter
public enum BatteryDeviceProtocolCode {

    // 常规自动轮询只使用 01/81；其他命令需由显式控制流程触发。
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

    /**
     * 请求命令码。
     */
    private final int requestCode;

    /**
     * 响应命令码；无明确响应的广播命令为空。
     */
    private final Integer responseCode;

    /**
     * 是否为 1 字节状态应答。
     */
    private final boolean statusResponse;

    /**
     * 协议功能说明。
     */
    private final String description;

    BatteryDeviceProtocolCode(int requestCode, Integer responseCode, boolean statusResponse, String description) {
        this.requestCode = requestCode;
        this.responseCode = responseCode;
        this.statusResponse = statusResponse;
        this.description = description;
    }

    /**
     * 判断命令码是否匹配请求或响应。
     *
     * @param code 命令码
     * @return 是否匹配
     */
    public boolean matches(int code) {
        return requestCode == code || (responseCode != null && responseCode == code);
    }

    /**
     * 判断是否为请求命令码。
     *
     * @param code 命令码
     * @return 是否请求
     */
    public boolean isRequest(int code) {
        return requestCode == code;
    }

    /**
     * 判断是否为响应命令码。
     *
     * @param code 命令码
     * @return 是否响应
     */
    public boolean isResponse(int code) {
        return responseCode != null && responseCode == code;
    }

    /**
     * 是否存在明确响应码。
     *
     * @return 是否有响应码
     */
    public boolean hasResponseCode() {
        return responseCode != null;
    }

    /**
     * 是否为通用状态响应。
     *
     * @return 是否状态响应
     */
    public boolean isStatusResponse() {
        return statusResponse;
    }

    /**
     * 按请求或响应命令码查找协议定义。
     *
     * @param code 命令码
     * @return 协议定义
     */
    public static BatteryDeviceProtocolCode find(int code) {
        for (BatteryDeviceProtocolCode value : values()) {
            if (value.matches(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 判断命令码是否为已知 600 节模块端协议。
     *
     * @param code 命令码
     * @return 是否已知
     */
    public static boolean isKnown(int code) {
        return find(code) != null;
    }
}
