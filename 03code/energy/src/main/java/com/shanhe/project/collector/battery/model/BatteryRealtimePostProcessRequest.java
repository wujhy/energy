package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

/**
 * 实时数据后处理流水线请求对象
 *
 * @author wjh
 * @since 2026-06-16
 */
@Data
@Builder
public class BatteryRealtimePostProcessRequest {

    /** 采集通道配置。 */
    private BatteryCollectorChannelConfig channelConfig;

    /** 当前轮询批次上下文。 */
    private BatteryModulePollContext pollContext;

    /** 后处理用组级计算数据。 */
    private BatteryModuleGroupRealtime calculation;

    /** 当前标准实时快照。 */
    private BatteryModuleRealtimeSnapshot realtimeSnapshot;
}
