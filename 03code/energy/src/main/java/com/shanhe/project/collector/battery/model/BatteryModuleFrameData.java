package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

/**
 * 600节模块端标准解析数据。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryModuleFrameData {

    /**
     * 解析数据类型。
     */
    private BatteryModuleDataType type;

    /**
     * 模块地址。
     */
    private int moduleAddress;

    /**
     * 响应是否成功。
     */
    private boolean success;

    /**
     * 原始应答标志。
     */
    private int responseFlag;

    /**
     * 协议码名称。
     */
    private String protocolCode;

    /**
     * 状态应答码。
     */
    private Integer statusCode;

    /**
     * 自动设置地址分配结果。
     */
    private Integer assignedModuleAddress;

    /**
     * 自动设置地址步骤。
     */
    private Integer autoSetAddressStep;

    /**
     * 单体电压。
     */
    private Double cellVoltage;

    /**
     * 单体内阻。
     */
    private Integer internalResistance;

    /**
     * 单体温度。
     */
    private Double cellTemperature;

    /**
     * 漏液状态。
     */
    private Integer leakageStatus;

    /**
     * 鼓包电压。
     */
    private Double swollenVoltage;

    /**
     * 充放电电流。
     */
    private Double chargeDischargeCurrent;

    /**
     * 浮充电流。
     */
    private Double floatCurrent;

    /**
     * 外组压。
     */
    private Double externalVoltage;

    /**
     * 环境温度1。
     */
    private Double environmentTemperature1;

    /**
     * 环境温度2。
     */
    private Double environmentTemperature2;

    /**
     * 连接条测试电池电压。
     */
    private Double connectBatteryVoltage;

    /**
     * 连接条测试电压。
     */
    private Double connectTestVoltage;
}
