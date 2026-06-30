package com.shanhe.project.collector.battery.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集器运行时指标
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCollectorMetrics {

    private Long generatedAt;
    private Integer channelCount;
    private Integer enabledChannelCount;
    private Integer openedChannelCount;
    private Integer runningChannelCount;
    private Integer totalActiveModuleAddressCount;
    private Integer totalQueuedModuleCommandCount;
    private Integer totalTimeoutCount;
    private Integer totalSnapshotCellCount;
    private Integer totalSnapshotStaleCellCount;
    private Integer totalSnapshotMissingCellCount;
    private List<BatteryCollectorChannelMetrics> channels = new ArrayList<>();
}
