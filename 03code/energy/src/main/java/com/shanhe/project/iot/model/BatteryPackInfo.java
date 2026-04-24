package com.shanhe.project.iot.model;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 【电池组实时数据】对象
 *
 * @author wjh
 * @since 2025/4/10
 */
@Data
public class BatteryPackInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 蓄电池主编号，1,2,3,4 */
    private Integer packNum;
    /** 电池组组压  单位：V */
    private Double packVoltage;
    /** 电池组外组压  单位：V */
    private Double batteryPackOuterVoltage;
    /** 电池组充放电电流 单位：A */
    private Double packCurrent;
    /** 电池组浮充电流 单位：A */
    private Double batteryPackFloatCurrent;
    /** 环境温度1单位：℃ */
    private Double environmentTemperature1;
    /** 环境温度2,单位：℃ */
    private Double environmentTemperature2;
    /** 最高电压电池号 */
    private Integer maxVoltageBatteryNumber;
    /** 最高电池电压值  单位：V */
    private Double batteryMaxVoltage;
    /** 最低电压电池号 */
    private Integer minVoltageBatteryNumber;
    /** 最低电池电压值  单位：V */
    private Double batteryMinVoltage;
    /** 电池平均单体电压  单位：V */
    private Double batteryAvgVoltage;
    /** 电池电压均差值单位  单位：V */
    private Double batteryVoltageDeviation;
    /** 电池电压极差值  单位：V */
    private Double batteryVoltageRange;
    /** 最高内阻电池号 */
    private Integer maxResistanceBatteryNumber;
    /** 最高电池内阻值 单位：uΩ */
    private Double batteryMaxResistance;
    /** 最低内阻电池号 */
    private Integer minResistanceBatteryNumber;
    /** 最低电池内阻值 单位：uΩ */
    private Double batteryMinEsistance;
    /** 平均电池内阻值 单位：uΩ */
    private Double batteryAvgResistance;
    /** 最高温度电池号 */
    private Integer maxTemperatureBatteryNumber;
    /** 最高电池温度值 单位：℃ */
    private Double batteryMaxTemperature;
    /** 最低温度电池号 */
    private Integer minTemperatureBatteryNumber;
    /** 最低电池温度值 单位：℃ */
    private Double batteryMinTemperature;
    /** 平均电池温度值 单位：℃ */
    private Double batteryAvgTemperature;
    /** 组SOC  单位：% */
    private Double batteryPackSoc;
    /** 组SOH  单位：% */
    private Double batteryPackSoh;
    /** 剩余放电时长  单位：小时 */
    private Integer residualDischargeDuration;
    /** 备电时长  单位：小时 */
    private Integer backupDuration;
    /** 纹波电压 单位：V */
    private Double rippleVoltage;
    /** 氢气浓度  单位：% */
    private Double hydrogenConcentration;
    /** 绝缘正电阻  单位：uΩ */
    private Double positiveinsulationResistance;
    /** 绝缘负电阻  单位：uΩ */
    private Double negativeinsulationResistance;
    /** 连接条，内阻，电池容量测试标志位0表示不在内阻测试1表示通讯故障2表示浮充电流异常4表示放电电流异常6表示正在内阻测试7表示内阻测试正常结束8表示内阻测试异常结束9表示回路电压异常 */
    private Integer resistanceTestStatus;
    /** 接地电池号下限 */
    private Integer groundingBatteryUpperLimit;
    /** 接地电池号下限 */
    private Integer groundingBatteryLowerLimit;
    /** 内阻最大变化率电池号 */
    private Integer maxResistanceRateChangeBatteryNumber;
    /** 内阻最大变化率值   单位：% */
    private Double maxResistanceRateChange;
    /** 电池状态0：监控1：充电2：停电3：核容4：未连接5：备电6：空闲 */
    private Integer batteryPackStatus;
    /** 电池组核容值 单位 AH */
    private Double bcapacity;
    /** 电池组容量 单位 AH */
    private Double capacity;
    /** 放电容量 单位 AH */
    private Double disChargeCapacity;
    /** 放电时长 分钟 */
    private Integer disChargeDuration;
    /** 设备工作状态 */
    private Integer deviceWorkStatus;
    /** 设备工作IO状态 */
    private Integer deviceWorkIoStatus;

    /** 单体电池列表 */
    private List<BatteryMonitor> batteryInfoList;
}
