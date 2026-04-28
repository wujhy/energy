package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端单体实时数据。
 */
@Data
public class BatteryModuleCellRealtime {

    private Long id;

    private Date createTime;

    private Date updateTime;

    private String channelName;

    private String portName;

    private Integer batteryGroup;

    private Integer moduleAddress;

    private Double cellVoltage;

    private Integer internalResistance;

    private Double cellTemperature;

    private Integer leakageStatus;

    private Double swollenVoltage;

    private Boolean success;

    private Integer responseFlag;

    private String pollBatchNo;

    private Date pollStartedAt;
}
