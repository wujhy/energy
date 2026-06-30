package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端采集帧日志。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleFrameLog {

    /** 主键。 */
    private Long id;

    /** 创建时间。 */
    private Date createTime;

    /** 通道名称。 */
    private String channelName;

    /** 串口名称。 */
    private String portName;

    /** 电池组编号。 */
    private Integer batteryGroup;

    /** 模块地址。 */
    private Integer moduleAddress;

    /** 命令码。 */
    private String commandCode;

    /** 是否已知协议。 */
    private Boolean known;

    /** 响应是否成功。 */
    private Boolean success;

    /** 原始应答标志。 */
    private Integer responseFlag;

    /** 信息域长度。 */
    private Integer payloadLength;

    /** 信息域十六进制内容。 */
    private String payloadHex;

    /** 完整帧十六进制内容。 */
    private String frameHex;

    /** 解析数据类型。 */
    private String parsedType;

    /** 单体电压。 */
    private Double cellVoltage;

    /** 单体内阻。 */
    private Integer internalResistance;

    /** 单体温度。 */
    private Double cellTemperature;

    /** 漏液状态。 */
    private Integer leakageStatus;

    /** 膨胀电压。 */
    private Double swollenVoltage;

    /** 充放电电流。 */
    private Double chargeDischargeCurrent;

    /** 浮充电流。 */
    private Double floatCurrent;

    /** 外部电压。 */
    private Double externalVoltage;

    /** 环境温度1。 */
    private Double environmentTemperature1;

    /** 环境温度2。 */
    private Double environmentTemperature2;

    /** 连接条电池电压。 */
    private Double connectBatteryVoltage;

    /** 连接条测试电压。 */
    private Double connectTestVoltage;
}
