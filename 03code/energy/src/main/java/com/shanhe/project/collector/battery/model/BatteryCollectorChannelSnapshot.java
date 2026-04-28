package com.shanhe.project.collector.battery.model;

import lombok.Data;

/**
 * 采集通道状态快照。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryCollectorChannelSnapshot {

    /**
     * 通道名称。
     */
    private String name;

    /**
     * 串口名称。
     */
    private String portName;

    /**
     * 电池组编号。
     */
    private Integer batteryGroup;

    /**
     * 默认设备地址。
     */
    private Integer deviceAddress;

    /**
     * 串口是否已打开。
     */
    private Boolean opened;

    /**
     * 当前运行状态。
     */
    private BatteryCollectorRunState runState;

    /**
     * 最后接收时间戳。
     */
    private Long lastReceiveTime;

    /**
     * 最后发送时间戳。
     */
    private Long lastSendTime;

    /**
     * 最后轮询时间戳。
     */
    private Long lastPollTime;

    /**
     * 最后超时时间戳。
     */
    private Long lastTimeoutTime;

    /**
     * 累计超时次数。
     */
    private Integer timeoutCount;

    /**
     * 当前请求重试次数。
     */
    private Integer currentRetryCount;

    /**
     * 最后请求命令码。
     */
    private Integer lastRequestCode;

    /**
     * 当前期望响应命令码。
     */
    private Integer expectedResponseCode;

    /**
     * 最后响应命令码。
     */
    private Integer lastResponseCode;

    /**
     * 最后待响应请求完成时间戳。
     */
    private Long lastPendingCompletedAt;

    /**
     * 最后待响应请求是否超时。
     */
    private Boolean lastPendingTimedOut;

    /**
     * 当前轮询批次号。
     */
    private String currentPollBatchNo;

    /**
     * 当前轮询开始时间戳。
     */
    private Long currentPollStartedAt;

    /**
     * 当前轮询模块地址。
     */
    private Integer currentPollAddress;

    /**
     * 已完成轮询轮次。
     */
    private Long pollRoundCount;

    /**
     * 当前是否全量发现。
     */
    private Boolean currentFullDiscovery;

    /**
     * 最近一次全量发现时间戳。
     */
    private Long lastFullDiscoveryTime;

    /**
     * 有响应模块地址数量。
     */
    private Integer activeModuleAddressCount;

    /**
     * 有响应模块地址列表。
     */
    private String activeModuleAddresses;

    /**
     * 待响应命令名称。
     */
    private String pendingCommandName;

    /**
     * 待响应请求命令码。
     */
    private Integer pendingRequestCode;

    /**
     * 待响应期望响应码。
     */
    private Integer pendingResponseCode;

    /**
     * 待响应请求是否自动轮询产生。
     */
    private Boolean pendingAutoPoll;

    /**
     * 接收缓冲长度。
     */
    private Integer receiveBufferSize;
}
