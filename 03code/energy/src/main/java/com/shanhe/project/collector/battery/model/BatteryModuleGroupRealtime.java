package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端电流温度模块实时数据。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleGroupRealtime {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 电池组编号。
     */
    private Integer packNum;

    /**
     * 兼容旧 pack_data 的组总电压。
     */
    private Double packVoltage;

    /**
     * 兼容旧 pack_data 的组充放电电流。
     */
    private Double packCurrent;

    /**
     * 兼容旧 pack_data 的电池组浮充电流。
     */
    private Double batteryPackFloatCurrent;

    /**
     * 兼容旧 pack_data 的电池组外组压。
     */
    private Double batteryPackOuterVoltage;

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
     * 轮询批次号。
     */
    private String pollBatchNo;

    /**
     * 轮询开始时间。
     */
    private Date pollStartedAt;

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

    private Integer maxVoltageBatNum;

    private Double maxCellVoltage;

    private Integer minVoltageBatNum;

    private Double minCellVoltage;

    private Double avgCellVoltage;

    private Double voltageRange;

    private Integer maxTemperatureBatNum;

    private Double maxCellTemperature;

    private Integer minTemperatureBatNum;

    private Double minCellTemperature;

    private Double avgCellTemperature;

    private Double temperatureRange;

    private Integer maxResistanceBatNum;

    private Integer maxInternalResistance;

    private Integer minResistanceBatNum;

    private Integer minInternalResistance;

    private Double avgInternalResistance;

    private Integer resistanceRange;

    /**
     * 电池组 SOC，算法来源未明确时保持为空。
     */
    private Double batteryPackSoc;

    /**
     * 电池组 SOH，算法来源未明确时保持为空。
     */
    private Double batteryPackSoh;

    /**
     * 最新单体更新时间。
     */
    private Date latestCellUpdateTime;

    /**
     * 最新 246 组模块更新时间。
     */
    private Date latestGroupUpdateTime;

    /**
     * 246 组模块数据是否为本轮新鲜数据。
     */
    private Boolean groupModuleFresh;
}
