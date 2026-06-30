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

    /** 指标生成时间戳。 */
    private Long generatedAt;
    /** 通道总数。 */
    private Integer channelCount;
    /** 已启用通道数。 */
    private Integer enabledChannelCount;
    /** 已打开串口通道数。 */
    private Integer openedChannelCount;
    /** 运行中通道数。 */
    private Integer runningChannelCount;
    /** 所有通道有响应模块地址总数。 */
    private Integer totalActiveModuleAddressCount;
    /** 所有通道等待下发的模块端命令总数。 */
    private Integer totalQueuedModuleCommandCount;
    /** 所有通道累计超时总数。 */
    private Integer totalTimeoutCount;
    /** 所有通道快照单体总数。 */
    private Integer totalSnapshotCellCount;
    /** 所有通道快照陈旧单体总数。 */
    private Integer totalSnapshotStaleCellCount;
    /** 所有通道快照缺失单体总数。 */
    private Integer totalSnapshotMissingCellCount;
    /** 各通道指标明细。 */
    private List<BatteryCollectorChannelMetrics> channels = new ArrayList<>();
}
