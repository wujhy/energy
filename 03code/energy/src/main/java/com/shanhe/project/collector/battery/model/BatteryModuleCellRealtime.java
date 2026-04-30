package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端单体实时数据。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleCellRealtime {

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
     * 单体编号，对应600节模块端单体地址。
     */
    private Integer batNum;

    /**
     * 兼容旧 monitor_data 的单体电压。
     */
    private Double voltage;

    /**
     * 兼容旧 monitor_data 的单体内阻。
     */
    private Integer resistance;

    /**
     * 兼容旧 monitor_data 的单体温度。
     */
    private Double temperature;

    /**
     * 容量字段，600节协议暂不直接提供。
     */
    private Double capacity;

    /**
     * 连接条电阻字段，600节协议默认轮询暂不直接提供。
     */
    private Double resistanceRageSlip;

    /**
     * 兼容旧 monitor_data 的内阻变化率字段，600节协议暂不直接提供。
     */
    private Double resistanceRateChange;

    /**
     * 鼓包电压字段。
     */
    private Double swollenVoltage;

    /**
     * 漏液状态。
     */
    private Integer leakageStatus;

    /**
     * 轮询批次号。
     */
    private String pollBatchNo;

    /**
     * 轮询开始时间。
     */
    private Date pollStartedAt;
}
