package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 当前电池组状态
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentGroupState {

    /** 电池组编号。 */
    private Integer packNum;
    /** 组总电压（V）。 */
    private Double packVoltage;
    /** 组总电流（A）。 */
    private Double packCurrent;
    /** 充放电电流（A）。 */
    private Double chargeDischargeCurrent;
    /** 浮充电流（A）。 */
    private Double floatCurrent;
    /** 外组压（V）。 */
    private Double externalVoltage;
    /** 环境温度1（℃）。 */
    private Double environmentTemperature1;
    /** 环境温度2（℃）。 */
    private Double environmentTemperature2;
    /** 轮询批次号。 */
    private String pollBatchNo;
    /** 轮询开始时间。 */
    private Date pollStartedAt;
    /** 单体数量。 */
    private Integer cellCount;
    /** 在线单体数量。 */
    private Integer onlineCellCount;
    /** 数据陈旧单体数量。 */
    private Integer staleCellCount;
    /** 数据是否新鲜。 */
    private Boolean dataFresh;
    /** 最新单体更新时间。 */
    private Date latestCellUpdateTime;
    /** 最新 246 组模块更新时间。 */
    private Date latestGroupUpdateTime;
    /** 246 组模块数据是否为本轮新鲜数据。 */
    private Boolean groupModuleFresh;
    /** 电池组状态码。 */
    private Integer batteryPackStatus;
    /** 内阻测试状态码。 */
    private Integer resistanceTestStatus;
    /** 设备工作状态码。 */
    private Integer deviceWorkStatus;
}
