package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.Date;

/**
 * 采集通道运行时指标
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCollectorChannelMetrics {

    /** 通道名称。 */
    private String name;
    /** 电池组编号。 */
    private Integer batteryGroup;
    /** 是否启用。 */
    private Boolean enabled;
    /** 串口是否已打开。 */
    private Boolean opened;
    /** 当前运行状态。 */
    private BatteryCollectorRunState runState;
    /** 最后接收时间戳。 */
    private Long lastReceiveTime;
    /** 最后发送时间戳。 */
    private Long lastSendTime;
    /** 最后轮询时间戳。 */
    private Long lastPollTime;
    /** 最近一次超时时间戳。 */
    private Long lastTimeoutTime;
    /** 累计超时次数。 */
    private Integer timeoutCount;
    /** 当前重试次数。 */
    private Integer currentRetryCount;
    /** 当前轮询批次号。 */
    private String currentPollBatchNo;
    /** 当前轮询开始时间戳。 */
    private Long currentPollStartedAt;
    /** 当前轮询已耗时（毫秒）。 */
    private Long currentPollElapsedMs;
    /** 当前轮询模块地址。 */
    private Integer currentPollAddress;
    /** 已完成轮询轮次。 */
    private Long pollRoundCount;
    /** 当前是否全量发现。 */
    private Boolean currentFullDiscovery;
    /** 最近一次全量发现时间戳。 */
    private Long lastFullDiscoveryTime;
    /** 有响应模块地址数量。 */
    private Integer activeModuleAddressCount;
    /** 等待下发的模块端控制命令数量。 */
    private Integer queuedModuleCommandCount;
    /** 待响应命令名称。 */
    private String pendingCommandName;
    /** 待响应请求是否自动轮询产生。 */
    private Boolean pendingAutoPoll;
    /** 最近完成的模块端命令名称。 */
    private String lastCompletedModuleCommandName;
    /** 最近完成的模块端命令是否成功。 */
    private Boolean lastCompletedModuleCommandSuccess;
    /** 最近完成的模块端命令完成时间戳。 */
    private Long lastCompletedModuleCommandTime;
    /** 接收缓冲长度。 */
    private Integer receiveBufferSize;
    /** 快照中的单体数量。 */
    private Integer snapshotCellCount;
    /** 快照中本轮采集到的单体数量。 */
    private Integer snapshotCurrentBatchCellCount;
    /** 快照中数据陈旧的单体数量。 */
    private Integer snapshotStaleCellCount;
    /** 快照中缺失的单体数量。 */
    private Integer snapshotMissingCellCount;
    /** 快照命中率。 */
    private Double snapshotHitRate;
    /** 快照距上次刷新的时间差（毫秒）。 */
    private Long snapshotAgeMs;
    /** 快照对应轮询批次号。 */
    private String snapshotPollBatchNo;
    /** 快照对应轮询开始时间。 */
    private Date snapshotPollStartedAt;
    /** 快照刷新时间。 */
    private Date snapshotRefreshedAt;
    /** 快照是否包含有效数据。 */
    private Boolean snapshotDataReady;
    /** 通道健康状态。 */
    private String channelHealth;
}
