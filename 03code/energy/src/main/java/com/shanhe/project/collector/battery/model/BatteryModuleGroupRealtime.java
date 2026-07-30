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

    /** 主键。 */
    private Long id;

    /** 创建时间。 */
    private Date createTime;

    /** 电池组编号。 */
    private Integer packNum;

    /** 组总电压。 */
    private Double packVoltage;

    /** 兼容旧 pack_data 的组充放电电流。 */
    private Double packCurrent;

    /** 充放电电流。 */
    private Double chargeDischargeCurrent;

    /** 浮充电流。 */
    private Double floatCurrent;

    /** 外组压。 */
    private Double externalVoltage;

    /** 环境温度1。 */
    private Double environmentTemperature1;

    /** 环境温度2。 */
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

    /** 电池组 SOC，算法来源未明确时保持为空。 */
    private Double batteryPackSoc;

    /** 电池组 SOH，算法来源未明确时保持为空。 */
    private Double batteryPackSoh;

    /** 兼容旧 pack_data 的电池组状态。 */
    private Integer batteryPackStatus;

    /** 兼容旧 pack_data 的电池组核容值。 */
    private Double bcapacity;

    /** 兼容旧 pack_data 的电池组容量。 */
    private Double capacity;

    /** 兼容旧 pack_data 的备电时长。 */
    private Integer backupDuration;

    /** 兼容旧 pack_data 的放电容量。 */
    private Double disChargeCapacity;

    /** 兼容旧 pack_data 的放电时长。 */
    private Integer disChargeDuration;

    /** 兼容旧 pack_data 的电池平均温度。 */
    private Double batteryAvgTemperature;

    /** 兼容旧 pack_data 的电压均差值。 */
    private Double batteryVoltageDeviation;

    /** 兼容旧 pack_data 的电压极差值。 */
    private Double batteryVoltageRange;

    /** 兼容旧 pack_data 的剩余放电时长。 */
    private Integer residualDischargeDuration;

    /** 兼容旧 pack_data 的纹波电压。 */
    private Double rippleVoltage;

    /** 兼容旧 pack_data 的氢气浓度。 */
    private Double hydrogenConcentration;

    /** 兼容旧 pack_data 的绝缘正电阻。 */
    private Double positiveInsulationResistance;

    /** 兼容旧 pack_data 的绝缘负电阻。 */
    private Double negativeInsulationResistance;

    /** 兼容旧 pack_data 的接地电池号上限。 */
    private Integer groundingBatteryUpperLimit;

    /** 兼容旧 pack_data 的接地电池号下限。 */
    private Integer groundingBatteryLowerLimit;

    /** 兼容旧 pack_data 的最大内阻变化率单体编号。 */
    private Integer maxResistanceRateChangeBatNum;

    /** 兼容旧 pack_data 的最大内阻变化率。 */
    private Double maxResistanceRateChange;

    /** 兼容旧 pack_data 的内阻测试状态。 */
    private Integer resistanceTestStatus;

    /** 兼容旧 pack_data 的设备工作状态。 */
    private Integer deviceWorkStatus;

    /** 兼容旧 pack_data 的设备工作IO状态。 */
    private Integer deviceWorkIoStatus;

    /** 最新单体更新时间。 */
    private Date latestCellUpdateTime;

    /** 最新 246 组模块更新时间。 */
    private Date latestGroupUpdateTime;

    /** 246 组模块数据是否为本轮新鲜数据。 */
    private Boolean groupModuleFresh;
}
