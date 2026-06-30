package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;

class BatteryCollectorRuntimeViewServiceTest {

    private final BatteryCollectorRuntimeViewService service = new BatteryCollectorRuntimeViewService();

    @Test
    void shouldReturnEmptyRuntimeViewsWhenStatesMissing() {
        BatteryCollectorMetrics metrics = service.getMetrics(null, null);

        Assertions.assertTrue(service.getChannelSnapshots(null).isEmpty());
        Assertions.assertNotNull(metrics.getGeneratedAt());
        Assertions.assertEquals(0, metrics.getChannelCount());
        Assertions.assertEquals(0, metrics.getTotalSnapshotCellCount());
        Assertions.assertTrue(metrics.getChannels().isEmpty());
    }

    @Test
    void shouldKeepSnapshotMetricsEmptyWhenCacheMissing() {
        BatteryCollectorChannelState state = stateWithRuntimeData();

        BatteryCollectorMetrics metrics = service.getMetrics(Collections.singletonList(state), null);

        Assertions.assertEquals(1, metrics.getChannelCount());
        Assertions.assertEquals(0, metrics.getTotalSnapshotCellCount());
        Assertions.assertEquals(0, metrics.getTotalSnapshotStaleCellCount());
        Assertions.assertEquals(0, metrics.getTotalSnapshotMissingCellCount());
        Assertions.assertNull(metrics.getChannels().get(0).getSnapshotDataReady());
    }

    @Test
    void shouldBuildChannelSnapshot() {
        BatteryCollectorChannelState state = stateWithRuntimeData();

        BatteryCollectorChannelSnapshot snapshot = service.buildSnapshot(state);

        Assertions.assertEquals("battery-group-1", snapshot.getName());
        Assertions.assertEquals("ttyS9", snapshot.getPortName());
        Assertions.assertEquals(1, snapshot.getBatteryGroup());
        Assertions.assertFalse(snapshot.getOpened());
        Assertions.assertEquals(BatteryCollectorRunState.WAIT_RESPONSE, snapshot.getRunState());
        Assertions.assertEquals(100L, snapshot.getLastSendTime());
        Assertions.assertEquals(200L, snapshot.getLastReceiveTime());
        Assertions.assertEquals(3, snapshot.getTimeoutCount());
        Assertions.assertEquals("battery-group-1-100", snapshot.getCurrentPollBatchNo());
        Assertions.assertEquals(100L, snapshot.getCurrentPollStartedAt());
        Assertions.assertEquals(8, snapshot.getCurrentPollAddress());
        Assertions.assertEquals(2L, snapshot.getPollRoundCount());
        Assertions.assertTrue(snapshot.getCurrentFullDiscovery());
        Assertions.assertEquals(90L, snapshot.getLastFullDiscoveryTime());
        Assertions.assertEquals(2, snapshot.getActiveModuleAddressCount());
        Assertions.assertEquals("8,246", snapshot.getActiveModuleAddresses());
        Assertions.assertEquals("MODULE_INFO", snapshot.getPendingCommandName());
        Assertions.assertEquals(0x01, snapshot.getPendingRequestCode());
        Assertions.assertEquals(0x81, snapshot.getPendingResponseCode());
        Assertions.assertEquals("SET_MODULE_ADDRESS", snapshot.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x88, snapshot.getLastCompletedModuleResponseCode());
        Assertions.assertTrue(snapshot.getLastCompletedModuleCommandSuccess());
        Assertions.assertEquals(300L, snapshot.getLastCompletedModuleCommandTime());
        Assertions.assertEquals(1, snapshot.getQueuedModuleCommandCount());
    }

    @Test
    void shouldBuildCollectorMetricsFromRunningChannelAndCachedSnapshot() {
        BatteryCollectorChannelState state = stateWithRuntimeData();
        Date refreshedAt = new Date(500L);
        BatteryModuleRealtimeSnapshot snapshot = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .cells(Arrays.asList(
                        new com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime(),
                        new com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime()))
                .currentBatchCellNums(new LinkedHashSet<>(Arrays.asList(1, 2)))
                .staleCellNums(new LinkedHashSet<>(Collections.singletonList(3)))
                .missingCellNums(new LinkedHashSet<>(Collections.singletonList(4)))
                .refreshedAt(refreshedAt)
                .build();
        BatteryModuleRealtimeSnapshotService realtimeSnapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        Mockito.when(realtimeSnapshotService.getCachedSnapshot(1)).thenReturn(snapshot);

        BatteryCollectorMetrics metrics = service.getMetrics(Collections.singletonList(state), realtimeSnapshotService);

        Assertions.assertEquals(1, metrics.getChannelCount());
        Assertions.assertEquals(1, metrics.getEnabledChannelCount());
        Assertions.assertEquals(0, metrics.getOpenedChannelCount());
        Assertions.assertEquals(1, metrics.getRunningChannelCount());
        Assertions.assertEquals(2, metrics.getTotalActiveModuleAddressCount());
        Assertions.assertEquals(1, metrics.getTotalQueuedModuleCommandCount());
        Assertions.assertEquals(3, metrics.getTotalTimeoutCount());
        Assertions.assertEquals(2, metrics.getTotalSnapshotCellCount());
        Assertions.assertEquals(1, metrics.getTotalSnapshotStaleCellCount());
        Assertions.assertEquals(1, metrics.getTotalSnapshotMissingCellCount());
        BatteryCollectorChannelMetrics channel = metrics.getChannels().get(0);
        Assertions.assertEquals("battery-group-1", channel.getName());
        Assertions.assertEquals(1, channel.getBatteryGroup());
        Assertions.assertTrue(channel.getEnabled());
        Assertions.assertThrows(NoSuchFieldException.class,
                () -> BatteryCollectorChannelMetrics.class.getDeclaredField("portName"));
        Assertions.assertThrows(NoSuchFieldException.class,
                () -> BatteryCollectorChannelMetrics.class.getDeclaredField("deviceAddress"));
        Assertions.assertEquals(BatteryCollectorRunState.WAIT_RESPONSE, channel.getRunState());
        Assertions.assertEquals("MODULE_INFO", channel.getPendingCommandName());
        Assertions.assertEquals("SET_MODULE_ADDRESS", channel.getLastCompletedModuleCommandName());
        Assertions.assertEquals("batch-1", channel.getSnapshotPollBatchNo());
        Assertions.assertEquals(refreshedAt, channel.getSnapshotRefreshedAt());
        Assertions.assertTrue(channel.getSnapshotDataReady());
        Mockito.verify(realtimeSnapshotService).getCachedSnapshot(1);
        Mockito.verify(realtimeSnapshotService, Mockito.never()).getSnapshot(Mockito.any());
    }

    /**
     * METRICS-001: 无 pending 命令且无队列命令时，metrics 对应字段为 null / 0。
     */
    @Test
    void shouldReportNullPendingAndZeroQueuedWhenNoCommands() {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("idle-channel");
        config.setBatteryGroup(2);
        config.setEnabled(true);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(config);
        // 不设置 pendingCommand，不添加 queue —— 模拟空闲通道

        BatteryCollectorMetrics metrics = service.getMetrics(Collections.singletonList(state), null);

        BatteryCollectorChannelMetrics channel = metrics.getChannels().get(0);
        Assertions.assertNull(channel.getPendingCommandName());
        Assertions.assertNull(channel.getPendingAutoPoll());
        Assertions.assertEquals(0, channel.getQueuedModuleCommandCount());
        Assertions.assertEquals(0, metrics.getTotalQueuedModuleCommandCount());
    }

    /**
     * METRICS-001: 通道关闭后 pending 和 queue 被清除，metrics 正确反映清空状态。
     * lastCompletedModuleCommandName/Success/Time 保留最近一次完成记录。
     */
    @Test
    void shouldReportClearedMetricsAfterChannelClose() {
        BatteryCollectorChannelState state = stateWithRuntimeData();
        // 模拟通道关闭后 pending 和队列命令被清除
        state.setPendingCommand(null);
        state.getQueuedModuleCommands().clear();

        BatteryCollectorMetrics metrics = service.getMetrics(Collections.singletonList(state), null);

        BatteryCollectorChannelMetrics channel = metrics.getChannels().get(0);
        Assertions.assertNull(channel.getPendingCommandName());
        Assertions.assertNull(channel.getPendingAutoPoll());
        Assertions.assertEquals(0, channel.getQueuedModuleCommandCount());
        Assertions.assertEquals(0, metrics.getTotalQueuedModuleCommandCount());
        // lastCompleted 不受通道关闭影响
        Assertions.assertEquals("SET_MODULE_ADDRESS", channel.getLastCompletedModuleCommandName());
        Assertions.assertTrue(channel.getLastCompletedModuleCommandSuccess());
        Assertions.assertEquals(300L, channel.getLastCompletedModuleCommandTime());
    }

    /**
     * METRICS-001: 多通道聚合 totalQueuedModuleCommandCount 正确累加。
     */
    @Test
    void shouldAggregateQueuedCommandsAcrossMultipleChannels() {
        BatteryCollectorChannelState state1 = stateWithRuntimeData(); // 1 queued command
        BatteryCollectorChannelConfig config2 = new BatteryCollectorChannelConfig();
        config2.setName("channel-2");
        config2.setBatteryGroup(3);
        config2.setEnabled(true);
        BatteryCollectorChannelState state2 = new BatteryCollectorChannelState(config2);
        state2.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(1).requestCode(0x0F).responseCode(0x8F).payload(new byte[0]).build());
        state2.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(2).requestCode(0x11).responseCode(0x91).payload(new byte[0]).build());

        BatteryCollectorMetrics metrics = service.getMetrics(Arrays.asList(state1, state2), null);

        Assertions.assertEquals(2, metrics.getChannelCount());
        Assertions.assertEquals(1, metrics.getChannels().get(0).getQueuedModuleCommandCount());
        Assertions.assertEquals(2, metrics.getChannels().get(1).getQueuedModuleCommandCount());
        Assertions.assertEquals(3, metrics.getTotalQueuedModuleCommandCount());
    }

    private BatteryCollectorChannelState stateWithRuntimeData() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setPortName("ttyS9");
        channelConfig.setBatteryGroup(1);
        channelConfig.setDeviceAddress(1);
        channelConfig.setEnabled(true);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setRunState(BatteryCollectorRunState.WAIT_RESPONSE);
        state.setLastSendTime(100L);
        state.setLastReceiveTime(200L);
        state.setLastPollTime(250L);
        state.setLastTimeoutTime(260L);
        state.setTimeoutCount(3);
        state.setCurrentRetryCount(1);
        state.setCurrentPollBatchNo("battery-group-1-100");
        state.setCurrentPollStartedAt(100L);
        state.setCurrentPollAddress(8);
        state.setPollRoundCount(2L);
        state.setCurrentFullDiscovery(true);
        state.setLastFullDiscoveryTime(90L);
        state.setLastCompletedModuleCommandName("SET_MODULE_ADDRESS");
        state.setLastCompletedModuleResponseCode(0x88);
        state.setLastCompletedModuleCommandSuccess(true);
        state.setLastCompletedModuleCommandTime(300L);
        state.getActiveModuleAddresses().add(8);
        state.getActiveModuleAddresses().add(246);
        state.setPendingCommand(BatteryPendingRequest.command(0x01, 0x81, new byte[0], "MODULE_INFO"));
        state.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build());
        return state;
    }
}
