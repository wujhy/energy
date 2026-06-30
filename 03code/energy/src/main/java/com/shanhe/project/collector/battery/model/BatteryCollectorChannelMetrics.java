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

    private String name;
    private Integer batteryGroup;
    private Boolean enabled;
    private Boolean opened;
    private BatteryCollectorRunState runState;
    private Long lastReceiveTime;
    private Long lastSendTime;
    private Long lastPollTime;
    private Long lastTimeoutTime;
    private Integer timeoutCount;
    private Integer currentRetryCount;
    private String currentPollBatchNo;
    private Long currentPollStartedAt;
    private Integer currentPollAddress;
    private Long pollRoundCount;
    private Boolean currentFullDiscovery;
    private Long lastFullDiscoveryTime;
    private Integer activeModuleAddressCount;
    private Integer queuedModuleCommandCount;
    private String pendingCommandName;
    private Boolean pendingAutoPoll;
    private String lastCompletedModuleCommandName;
    private Boolean lastCompletedModuleCommandSuccess;
    private Long lastCompletedModuleCommandTime;
    private Integer receiveBufferSize;
    private Integer snapshotCellCount;
    private Integer snapshotCurrentBatchCellCount;
    private Integer snapshotStaleCellCount;
    private Integer snapshotMissingCellCount;
    private String snapshotPollBatchNo;
    private Date snapshotPollStartedAt;
    private Date snapshotRefreshedAt;
    private Boolean snapshotDataReady;
}
