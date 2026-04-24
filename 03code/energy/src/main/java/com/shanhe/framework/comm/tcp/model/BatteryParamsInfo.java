package com.shanhe.framework.comm.tcp.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 电池报警参数信息
 *
 * @author wjh
 * @since 2025/4/15
 */
@Data
public class BatteryParamsInfo implements Serializable
{
    private static final long serialVersionUID = 1L;
    // 设备ID（5字节）（无小数）（ID为全数字，大小在0-2^32之间）
    private String imei;
    //电池组编号
    private Integer batteryPackNumber;
    //单体电压过充告警值
    private String batteryOverchargeAlarm;
    //单体电压过充告警恢复值
    private String batteryOverchargeRestore;
    //单体电压过放告警值
    private String batteryOverdischargeAlarm;
    //单体电压过放告警恢复值
    private String batteryOverdischargeRestore;
    //单体浮充电压过高告警值
    private String batteryFloatChargeOvertopAlarm;
    //单体浮充电压过高告警恢复值
    private String batteryFloatChargeOvertopRestore;
    //单体浮充电压过低告警值
    private String batteryFloatChargeTooLowAlarm;
    //单体浮充电压过低告警恢复值
    private String batteryFloatChargeTooLowRestore;
    //单体电压不均告警值
    private String batteryVoltageUnevennessAlarm;
    //单体电压不均告警恢复值
    private String batteryVoltageUnevennessRestore;
    //单体电压极差值告警值
    private String batteryVoltageRangeAlarm;
    //单体电压极差值告警恢复值
    private String batteryVoltageRangeRestore;
    //总体电压过充告警值
    private String batteryPackOverchargeAlarm;
    //总体电压过充告警恢复值
    private String batteryPackOverchargeRestore;
    //总体电压过放告警值
    private String batteryPackOverdischargeAlarm;
    //总体电压过放告警恢复值
    private String batteryPackOverdischargeRestore;
    //总体浮充电压过高保护警值
    private String batteryPackFloatChargeOvertopAlarm;
    //总体浮充电压过高保护警恢复值
    private String batteryPackFloatChargeOvertopRestore;
    //总体浮充电压过低告警值
    private String batteryPackFloatChargeTooLowAlarm;
    //总体浮充电压过低告警恢复值
    private String batteryPackFloatChargeTooLowRestore;
    //充过流告警值
    private String chargeOvercurrentAlarm;
    //充过流告警恢复值
    private String chargeOvercurrentRestore;
    //放过流告警值
    private String dischargeOvercurrentAlarm;
    //放过流告警恢复值
    private String dischargeOvercurrentRestore;
    //环境高温告警值
    private String  environmentHighTemperatureAlarm;
    //环境高温告警恢复值
    private String  environmentHighTemperatureRestore;
    //环境低温告警值
    private String  environmentLowTemperatureAlarm;
    //环境低温告警恢复值
    private String  environmentLowTemperatureRestore;
    //电池高温告警值
    private String  batteryHighTemperatureAlarm;
    //电池高温告警恢复值
    private String  batteryHighTemperatureRestore;
    //电池低温告警值
    private String  batteryLowTemperatureAlarm;
    //电池低温告警恢复值
    private String  batteryLowTemperatureRestore;
    //电池温度不均告警值
    private String batteryTemperatureUnevennessAlarm;
    //电池温度不均告警恢复值
    private String batteryTemperatureUnevennessRestore;
    //内阻过大告警系数
    private String resistanceTooBigAlarm;
    //内阻过大告警系数恢复值
    private String resistanceTooBigRestore;
    //内阻不均告警系数
    private String resistanceUnevennessAlarm;
    //内阻不均告警恢复系数
    private String resistanceUnevennessRestore;
    //内阻过小告警系数
    private String resistanceTooSmallAlarm;
    //内阻过小告警恢复系数
    private String resistanceTooSmallRestore;
    //连接条、氢气比例上限告警值
    private String hydrogenPercentageUpperLimitAlarm;
    //连接条、氢气比例上限告警恢复值
    private String hydrogenPercentageUpperLimitRestore;
    //SOC低告警值
    private String socLowAlarm;
    //SOC低告警恢复值
    private String socLowRestore;
    //SOH低告警值
    private String sohLowAlarm;
    //SOH低告警恢复值
    private String sohLowRestore;
    //内阻基准值
    private String resistanceReferenceValue;
    //参数报警等级
    private String alarmLevel;
    //参数号
    private String paramNumber;
}
