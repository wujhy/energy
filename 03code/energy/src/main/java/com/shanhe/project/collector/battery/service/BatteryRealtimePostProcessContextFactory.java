package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryRealtimePostProcessRequest;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

/**
 * 构造实时数据后处理流水线的输入
 *
 * @author wjh
 * @since 2026-06-16
 */
@Service
public class BatteryRealtimePostProcessContextFactory {

    public BatteryRealtimePostProcessRequest snapshotRequest(BatteryRealtimePostProcessRequest request) {
        if (request == null || request.getPollContext() == null) {
            return null;
        }
        BatteryModulePollContext context = request.getPollContext();
        BatteryModulePollContext snapshot = BatteryModulePollContext.builder()
                .pollBatchNo(context.getPollBatchNo())
                .pollStartedAt(context.getPollStartedAt())
                .cells(new ArrayList<>(context.getCells()))
                .groups(new ArrayList<>(context.getGroups()))
                .build();
        return BatteryRealtimePostProcessRequest.builder()
                .channelConfig(request.getChannelConfig())
                .pollContext(snapshot)
                .calculation(request.getCalculation())
                .realtimeSnapshot(request.getRealtimeSnapshot())
                .build();
    }

    public BatteryRealtimePostProcessContext buildContext(BatteryRealtimePostProcessRequest request) {
        BatteryCollectorChannelConfig channelConfig = request.getChannelConfig();
        BatteryModulePollContext context = request.getPollContext();
        BatteryModuleRealtimeSnapshot realtimeSnapshot = request.getRealtimeSnapshot();
        return BatteryRealtimePostProcessContext.builder()
                .packNum(channelConfig == null ? null : channelConfig.getBatteryGroup())
                .source("collector")
                .pollBatchNo(context.getPollBatchNo())
                .cells(realtimeSnapshot == null ? context.getCells() : realtimeSnapshot.getCells())
                .group(realtimeSnapshot == null ? request.getCalculation() : realtimeSnapshot.getGroup())
                .channelConfig(channelConfig)
                .realtimeSnapshot(realtimeSnapshot)
                .build();
    }
}
