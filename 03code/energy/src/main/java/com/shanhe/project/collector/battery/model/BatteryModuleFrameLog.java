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

    /**
     * 主键。
     */
    private Long id;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 通道名称。
     */
    private String channelName;

    /**
     * 串口名称。
     */
    private String portName;

    /**
     * 电池组编号。
     */
    private Integer batteryGroup;

    /**
     * 模块地址。
     */
    private Integer moduleAddress;

    /**
     * 命令码。
     */
    private String commandCode;

    /**
     * 是否已知协议。
     */
    private Boolean known;

    /**
     * 响应是否成功。
     */
    private Boolean success;

    /**
     * 原始应答标志。
     */
    private Integer responseFlag;

    /**
     * 信息域长度。
     */
    private Integer payloadLength;

    /**
     * 信息域十六进制内容。
     */
    private String payloadHex;

    /**
     * 完整帧十六进制内容。
     */
    private String frameHex;

    /**
     * 解析数据类型。
     */
    private String parsedType;

    private Double cellVoltage;

    private Integer internalResistance;

    private Double cellTemperature;

    private Integer leakageStatus;

    private Double swollenVoltage;

    private Double chargeDischargeCurrent;

    private Double floatCurrent;

    private Double externalVoltage;

    private Double environmentTemperature1;

    private Double environmentTemperature2;

    private Double connectBatteryVoltage;

    private Double connectTestVoltage;
}
