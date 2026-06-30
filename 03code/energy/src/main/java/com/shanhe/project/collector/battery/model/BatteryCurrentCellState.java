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

    private Integer packNum;
    private Integer batNum;
    private Double voltage;
    private Integer resistance;
    private Double temperature;
    private Double capacity;
    private Double resistanceRageSlip;
    /**
     * 连接条测试结果显示状态：OK 表示有有效电阻值，null 表示未测试或无数据。
     * 不做临时计算，不伪造默认值。
     */
    private String connectResistanceStatus;
    private Double resistanceRateChange;
    private Double swollenVoltage;
    private Integer leakageStatus;
    private String pollBatchNo;
    private Date pollStartedAt;
}
