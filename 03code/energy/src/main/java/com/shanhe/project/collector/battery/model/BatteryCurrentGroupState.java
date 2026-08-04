package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 当前电池组状态
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentGroupState {

    /** 电池组编号。 */
    private Integer packNum;
    /** 组总电压（V）。 */
    private Double packVoltage;
    /** 组总电流（A）。 */
    private Double packCurrent;
    /** 充放电电流（A）。 */
    private Double chargeDischargeCurrent;
    /** 浮充电流（A）。 */
    private Double floatCurrent;
    /** 外组压（V）。 */
    private Double externalVoltage;
    /** 环境温度1（℃）。 */
    private Double environmentTemperature1;
    /** 环境温度2（℃）。 */
    private Double environmentTemperature2;
    /** 轮询批次号。 */
    private String pollBatchNo;
    /** 轮询开始时间。 */
    private Date pollStartedAt;
    /** 单体数量。 */
    private Integer cellCount;
    /** 在线单体数量。 */
    private Integer onlineCellCount;
    /** 数据陈旧单体数量。 */
    private Integer staleCellCount;
    /** 数据是否新鲜。 */
    private Boolean dataFresh;
    /** 最新单体更新时间。 */
    private Date latestCellUpdateTime;
    /** 最新 246 组模块更新时间。 */
    private Date latestGroupUpdateTime;
    /** 246 组模块数据是否为本轮新鲜数据。 */
    private Boolean groupModuleFresh;
    /** 最高电压单体编号。 */
    private Integer maxVoltageBatNum;
    /** 最高单体电压。 */
    private Double maxCellVoltage;
    /** 最低电压单体编号。 */
    private Integer minVoltageBatNum;
    /** 最低单体电压。 */
    private Double minCellVoltage;
    /** 平均单体电压。 */
    private Double avgCellVoltage;
    /** 电压极差。 */
    private Double voltageRange;
    /** 最高温度单体编号。 */
    private Integer maxTemperatureBatNum;
    /** 最高单体温度。 */
    private Double maxCellTemperature;
    /** 最低温度单体编号。 */
    private Integer minTemperatureBatNum;
    /** 最低单体温度。 */
    private Double minCellTemperature;
    /** 平均单体温度。 */
    private Double avgCellTemperature;
    /** 温度极差。 */
    private Double temperatureRange;
    /** 最高内阻单体编号。 */
    private Integer maxResistanceBatNum;
    /** 最高单体内阻。 */
    private Integer maxInternalResistance;
    /** 最低内阻单体编号。 */
    private Integer minResistanceBatNum;
    /** 最低单体内阻。 */
    private Integer minInternalResistance;
    /** 平均单体内阻。 */
    private Double avgInternalResistance;
    /** 内阻极差。 */
    private Integer resistanceRange;
    /** 电池组 SOC。 */
    private Double batteryPackSoc;
    /** 电池组 SOH。 */
    private Double batteryPackSoh;
    /** 备电时长。 */
    private Integer backupDuration;
    /** 电池组核容值。 */
    private Double bcapacity;
    /** 电池组容量。 */
    private Double capacity;
    /** 放电容量。 */
    private Double disChargeCapacity;
    /** 放电时长。 */
    private Integer disChargeDuration;
    /** 剩余放电时长。 */
    private Integer residualDischargeDuration;
    /** 纹波电压。 */
    private Double rippleVoltage;
    /** 氢气浓度。 */
    private Double hydrogenConcentration;
    /** 绝缘正电阻。 */
    private Double positiveInsulationResistance;
    /** 绝缘负电阻。 */
    private Double negativeInsulationResistance;
    /** 接地电池号上限。 */
    private Integer groundingBatteryUpperLimit;
    /** 接地电池号下限。 */
    private Integer groundingBatteryLowerLimit;
    /** 最大内阻变化率单体编号。 */
    private Integer maxResistanceRateChangeBatNum;
    /** 最大内阻变化率。 */
    private Double maxResistanceRateChange;
    /** 电池组状态码。 */
    private Integer batteryPackStatus;
    /** 内阻测试状态码。 */
    private Integer resistanceTestStatus;
    /** 设备工作状态码。 */
    private Integer deviceWorkStatus;
}
