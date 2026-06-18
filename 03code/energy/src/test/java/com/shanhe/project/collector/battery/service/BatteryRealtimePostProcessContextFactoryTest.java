package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryRealtimePostProcessRequest;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;

class BatteryRealtimePostProcessContextFactoryTest {

    private final BatteryRealtimePostProcessContextFactory factory = new BatteryRealtimePostProcessContextFactory();

    @Test
    void shouldSnapshotPostProcessRequestBeforeAsyncExecution() {
        BatteryModuleCellRealtime cell = cell(3, 0);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        BatteryModulePollContext pollContext = BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .pollStartedAt(new Date(1000L))
                .cells(new java.util.ArrayList<>(Collections.singletonList(cell)))
                .groups(new java.util.ArrayList<>(Collections.singletonList(group)))
                .build();

        BatteryRealtimePostProcessRequest snapshotRequest = factory.snapshotRequest(
                BatteryRealtimePostProcessRequest.builder()
                        .channelConfig(channelConfig())
                        .pollContext(pollContext)
                        .calculation(group)
                        .build());
        pollContext.getCells().clear();
        pollContext.getGroups().clear();

        Assertions.assertNotNull(snapshotRequest);
        Assertions.assertNotSame(pollContext, snapshotRequest.getPollContext());
        Assertions.assertEquals("batch-1", snapshotRequest.getPollContext().getPollBatchNo());
        Assertions.assertEquals(1, snapshotRequest.getPollContext().getCells().size());
        Assertions.assertEquals(1, snapshotRequest.getPollContext().getGroups().size());
    }

    @Test
    void shouldBuildPostProcessContextFromPollContextWhenSnapshotMissing() {
        BatteryModuleCellRealtime cell = cell(3, 0);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        BatteryModulePollContext pollContext = BatteryModulePollContext.builder()
                .pollBatchNo("batch-2")
                .pollStartedAt(new Date(2000L))
                .cells(Collections.singletonList(cell))
                .groups(Collections.singletonList(group))
                .build();

        BatteryRealtimePostProcessContext postContext = factory.buildContext(
                BatteryRealtimePostProcessRequest.builder()
                        .channelConfig(channelConfig())
                        .pollContext(pollContext)
                        .calculation(group)
                        .build());

        Assertions.assertEquals(1, postContext.getPackNum());
        Assertions.assertEquals("collector", postContext.getSource());
        Assertions.assertEquals("batch-2", postContext.getPollBatchNo());
        Assertions.assertSame(pollContext.getCells(), postContext.getCells());
        Assertions.assertSame(group, postContext.getGroup());
        Assertions.assertNull(postContext.getRealtimeSnapshot());
    }

    @Test
    void shouldPreferRealtimeSnapshotWhenBuildingPostProcessContext() {
        BatteryModuleCellRealtime pollCell = cell(3, 0);
        BatteryModuleCellRealtime snapshotCell = cell(4, 1);
        BatteryModuleGroupRealtime calculation = new BatteryModuleGroupRealtime();
        calculation.setPackNum(1);
        BatteryModuleGroupRealtime snapshotGroup = new BatteryModuleGroupRealtime();
        snapshotGroup.setPackNum(1);
        BatteryModuleRealtimeSnapshot realtimeSnapshot = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .cells(Collections.singletonList(snapshotCell))
                .group(snapshotGroup)
                .build();
        BatteryModulePollContext pollContext = BatteryModulePollContext.builder()
                .pollBatchNo("batch-3")
                .cells(Collections.singletonList(pollCell))
                .build();

        BatteryRealtimePostProcessContext postContext = factory.buildContext(
                BatteryRealtimePostProcessRequest.builder()
                        .channelConfig(channelConfig())
                        .pollContext(pollContext)
                        .calculation(calculation)
                        .realtimeSnapshot(realtimeSnapshot)
                        .build());

        Assertions.assertSame(realtimeSnapshot.getCells(), postContext.getCells());
        Assertions.assertSame(snapshotGroup, postContext.getGroup());
        Assertions.assertSame(realtimeSnapshot, postContext.getRealtimeSnapshot());
    }

    private BatteryCollectorChannelConfig channelConfig() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setPortName("ttyS9");
        channelConfig.setBatteryGroup(1);
        return channelConfig;
    }

    private BatteryModuleCellRealtime cell(int batNum, Integer leakageStatus) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setLeakageStatus(leakageStatus);
        return cell;
    }
}
