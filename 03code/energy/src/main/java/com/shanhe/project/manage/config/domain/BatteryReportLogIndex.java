package com.shanhe.project.manage.config.domain;

import com.shanhe.framework.web.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BatteryReportLogIndex extends BaseEntity {
    /** 主键 */
    private Long id;
    /** 设备ID */
    private Long configId;
    /** 包序号 */
    private Integer packNum;

    /** 组电压。 */
    private Double packVoltage;
    /** 外组压。 */
    private Double externalVoltage;
    /** 充放电电流。 */
    private Double chargeDischargeCurrent;
    /** 浮充电流。 */
    private Double floatCurrent;
    /** 环境温度1。 */
    private Double environmentTemperature1;
    /** 环境温度2。 */
    private Double environmentTemperature2;
    /** 平均单体温度。 */
    private Double batteryAvgTemperature;
    /** 电池组 SOC。 */
    private Double batteryPackSoc;
    /** 电池组 SOH。 */
    private Double batteryPackSoh;
    /** 电池组状态。 */
    private Integer batteryPackStatus;
    /** 内阻测试状态。 */
    private Integer resistanceTestStatus;
    /** 设备工作状态。 */
    private Integer deviceWorkStatus;
    /** 设备工作 IO 状态。 */
    private Integer deviceWorkIoStatus;
    /** 放电容量。 */
    private Double disChargeCapacity;
    /** 放电时长。 */
    private Integer disChargeDuration;
    /** 剩余放电时长。 */
    private Integer residualDischargeDuration;
    /** 备电时长。 */
    private Integer backupDuration;
    /** 电池组核容值。 */
    private Double bcapacity;
    /** 电池组容量。 */
    private Double capacity;
    /** 包参数 */
    private Map<String, Object> packParam;

    /** 是否告警 0-是，1-否 */
    private Integer alarm;
}