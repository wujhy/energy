package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端电流温度模块实时数据。
 */
@Data
public class BatteryModuleGroupRealtime {

    private Long id;

    private Date createTime;

    private Date updateTime;

    private String channelName;

    private String portName;

    private Integer batteryGroup;

    private Integer moduleAddress;

    private Double chargeDischargeCurrent;

    private Double floatCurrent;

    private Double externalVoltage;

    private Double environmentTemperature1;

    private Double environmentTemperature2;

    private Boolean success;

    private Integer responseFlag;

    private String pollBatchNo;

    private Date pollStartedAt;
}
