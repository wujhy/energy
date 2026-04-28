package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端电池组计算结果。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleGroupCalculation {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 通道名称。
     */
    private String channelName;

    /**
     * 电池组编号。
     */
    private Integer batteryGroup;

    /**
     * 单体数量。
     */
    private Integer cellCount;

    /**
     * 在线单体数量。
     */
    private Integer onlineCellCount;

    /**
     * 数据陈旧单体数量。
     */
    private Integer staleCellCount;

    /**
     * 数据是否新鲜。
     */
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
