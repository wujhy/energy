package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * Current alarm summary for battery state query.
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentAlarmSummary {

    private Long alarmId;
    private Integer packNum;
    private Integer modelNum;
    private String itemCode;
    private String alarmLevel;
    private String dataInfo;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
