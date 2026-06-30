package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 当前告警摘要，用于电池状态查询
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
