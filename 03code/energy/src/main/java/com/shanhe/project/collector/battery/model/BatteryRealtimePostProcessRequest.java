package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

/**
 * Request object for the realtime post-process pipeline.
 *
 * @author wjh
 * @since 2026-06-16
 */
@Data
@Builder
public class BatteryRealtimePostProcessRequest {

    private BatteryCollectorChannelConfig channelConfig;

    private BatteryModulePollContext pollContext;

    private BatteryModuleGroupRealtime calculation;

    private BatteryModuleRealtimeSnapshot realtimeSnapshot;
}
