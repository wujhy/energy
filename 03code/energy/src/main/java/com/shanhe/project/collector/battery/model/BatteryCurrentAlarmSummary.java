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

    /** 告警记录主键。 */
    private Long alarmId;
    /** 电池组编号。 */
    private Integer packNum;
    /** 单体编号。 */
    private Integer modelNum;
    /** 告警编码。 */
    private String itemCode;
    /** 告警等级。 */
    private String alarmLevel;
    /** 告警数据信息。 */
    private String dataInfo;
    /** 告警状态（1=未恢复, 0=已恢复）。 */
    private Integer status;
    /** 创建时间。 */
    private Date createTime;
    /** 更新时间。 */
    private Date updateTime;
}
