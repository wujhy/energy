package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * Current battery group state.
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentGroupState {

    private Integer packNum;
    private Double packVoltage;
    private Double packCurrent;
    private Double chargeDischargeCurrent;
    private Double floatCurrent;
    private Double externalVoltage;
    private Double environmentTemperature1;
    private Double environmentTemperature2;
    private String pollBatchNo;
    private Date pollStartedAt;
    private Integer cellCount;
    private Integer onlineCellCount;
    private Integer staleCellCount;
    private Boolean dataFresh;
    private Date latestCellUpdateTime;
    private Date latestGroupUpdateTime;
    private Boolean groupModuleFresh;
    private Integer batteryPackStatus;
    private Integer resistanceTestStatus;
    private Integer deviceWorkStatus;
}
