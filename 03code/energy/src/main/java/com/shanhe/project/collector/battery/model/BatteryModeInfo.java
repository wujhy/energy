package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Battery work mode status shared by the collector/current-state pipeline.
 */
@Data
public class BatteryModeInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer packNum;
    private Integer result;
    private Integer mode;
    private Integer status;
    private Integer address;
    private Long businessOptLogId;
    private Integer lastPackNum;
    private Integer lastMode;
    private Integer lastAddress;
}