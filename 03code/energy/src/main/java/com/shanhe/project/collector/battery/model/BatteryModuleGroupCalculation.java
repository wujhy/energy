package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端电池组计算结果。
 */
@Data
public class BatteryModuleGroupCalculation {

    private Long id;

    private Date createTime;

    private Date updateTime;

    private String channelName;

    private Integer batteryGroup;

    private Integer cellCount;

    private Integer onlineCellCount;

    private Integer staleCellCount;

    private Boolean dataFresh;

    private Integer maxVoltageModuleAddress;

    private Double maxCellVoltage;

    private Integer minVoltageModuleAddress;

    private Double minCellVoltage;

    private Double avgCellVoltage;

    private Double voltageRange;

    private Integer maxTemperatureModuleAddress;

    private Double maxCellTemperature;

    private Integer minTemperatureModuleAddress;

    private Double minCellTemperature;

    private Double avgCellTemperature;

    private Double temperatureRange;

    private Integer maxResistanceModuleAddress;

    private Integer maxInternalResistance;

    private Integer minResistanceModuleAddress;

    private Integer minInternalResistance;

    private Double avgInternalResistance;

    private Integer resistanceRange;

    private Double externalVoltage;

    private Double chargeDischargeCurrent;

    private Double floatCurrent;

    private Double environmentTemperature1;

    private Double environmentTemperature2;

    private Date latestCellUpdateTime;

    private Date latestGroupUpdateTime;
}
