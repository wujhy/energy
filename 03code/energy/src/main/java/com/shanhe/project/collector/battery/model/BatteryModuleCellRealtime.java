package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 600节模块端单体实时数据。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryModuleCellRealtime {

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
     * 单体模块地址。
     */
    private Integer moduleAddress;

    /**
     * 单体电压。
     */
    private Double cellVoltage;

    /**
     * 单体内阻。
     */
    private Integer internalResistance;

    /**
     * 单体温度。
     */
    private Double cellTemperature;

    /**
     * 漏液状态。
     */
    private Integer leakageStatus;

    /**
     * 鼓包电压。
     */
    private Double swollenVoltage;

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
