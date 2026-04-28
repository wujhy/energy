package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端电流温度模块实时数据。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleGroupRealtime {

    /**
     * 主键。
     */
    private Long id;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 通道名称。
     */
    private String channelName;

    /**
     * 串口名称。
     */
    private String portName;

    /**
     * 电池组编号。
     */
    private Integer batteryGroup;

    /**
     * 电流温度模块地址。
     */
    private Integer moduleAddress;

    /**
     * 充放电电流。
     */
    private Double chargeDischargeCurrent;

    /**
     * 浮充电流。
     */
    private Double floatCurrent;

    /**
     * 外组压。
     */
    private Double externalVoltage;

    /**
     * 环境温度1。
     */
    private Double environmentTemperature1;

    /**
     * 环境温度2。
     */
    private Double environmentTemperature2;

    /**
     * 响应是否成功。
     */
    private Boolean success;

    /**
     * 原始应答标志。
     */
    private Integer responseFlag;

    /**
     * 轮询批次号。
     */
    private String pollBatchNo;

    /**
     * 轮询开始时间。
     */
    private Date pollStartedAt;
}
