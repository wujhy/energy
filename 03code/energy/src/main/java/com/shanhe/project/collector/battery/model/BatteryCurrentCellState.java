package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * Current battery cell state.
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
    private Double resistanceRateChange;
    private Double swollenVoltage;
    private Integer leakageStatus;
    private String pollBatchNo;
    private Date pollStartedAt;
}
