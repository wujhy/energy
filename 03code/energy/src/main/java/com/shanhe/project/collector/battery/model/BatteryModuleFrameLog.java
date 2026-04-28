package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端采集帧日志。
 */
@Data
public class BatteryModuleFrameLog {

    private Long id;

    private Date createTime;

    private String channelName;

    private String portName;

    private Integer batteryGroup;

    private Integer moduleAddress;

    private String commandCode;

    private Boolean known;

    private Boolean success;

    private Integer responseFlag;

    private Integer payloadLength;

    private String payloadHex;

    private String frameHex;

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
