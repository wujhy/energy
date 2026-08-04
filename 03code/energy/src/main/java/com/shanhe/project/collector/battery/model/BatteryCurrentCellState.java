package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 当前单体电池状态
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentCellState {

    /** 电池组编号。 */
    private Integer packNum;
    /** 单体编号。 */
    private Integer batNum;
    /** 单体电压（V）。 */
    private Double voltage;
    /** 单体内阻（μΩ）。 */
    private Integer resistance;
    /** 单体温度（℃）。 */
    private Double temperature;
    /** 单体容量。 */
    private Double capacity;
    /** 连接条电阻变化率。 */
    private Double resistanceRageSlip;
    /**
     * 连接条测试结果显示状态：OK 表示有有效电阻值，null 表示未测试或无数据。
     * 不做临时计算，不伪造默认值。
     */
    private String connectResistanceStatus;
    /** 内阻变化率。 */
    private Double resistanceRateChange;
    /** 鼓包电压（V）。 */
    private Double swollenVoltage;
    /** 漏液状态。 */
    private Integer leakageStatus;
    /** 创建时间。 */
    private Date createTime;
    /** 轮询批次号。 */
    private String pollBatchNo;
    /** 轮询开始时间。 */
    private Date pollStartedAt;
}
