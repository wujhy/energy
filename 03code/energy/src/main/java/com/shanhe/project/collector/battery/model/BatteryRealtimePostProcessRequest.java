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

    private BatteryCollectorChannelConfig channelConfig;

    private BatteryModulePollContext pollContext;

    private BatteryModuleGroupRealtime calculation;

    private BatteryModuleRealtimeSnapshot realtimeSnapshot;
}
