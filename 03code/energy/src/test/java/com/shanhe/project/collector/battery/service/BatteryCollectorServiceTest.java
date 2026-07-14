package com.shanhe.project.collector.battery.service;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.runtime.BatteryCollectorFrameIoService;
import com.shanhe.project.collector.battery.runtime.BatteryCollectorPollingService;
import com.shanhe.project.collector.battery.command.BatteryCollectorCommandQueueService;
import com.shanhe.project.collector.battery.command.BatteryConnectResistanceCommandProcessor;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.state.BatteryCollectorDeviceStateService;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.mapper.OptLogMapper;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

class BatteryCollectorServiceTest {

    private final BatteryCollectorService service = new BatteryCollectorService();
    private final BatteryCollectorCommandLogService commandLogService = new BatteryCollectorCommandLogService();
    private final BatteryCollectorDeviceStateService collectorDeviceStateService = new BatteryCollectorDeviceStateService();

    BatteryCollectorServiceTest() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        ReflectionTestUtils.setField(service, "runtimeViewService", new BatteryCollectorRuntimeViewService());
        ReflectionTestUtils.setField(service, "batteryCollectorCacheService", new BatteryCollectorCacheService());
        ReflectionTestUtils.setField(service, "protocolLogService", new BatteryCollectorProtocolLogService());
        ReflectionTestUtils.setField(service, "commandLogService", commandLogService);
        ReflectionTestUtils.setField(service, "collectorDeviceStateService", collectorDeviceStateService);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        BatteryCollectorPollingService pollingService = new BatteryCollectorPollingService();
        ReflectionTestUtils.setField(pollingService, "properties", new BatteryCollectorProperties());
        ReflectionTestUtils.setField(pollingService, "protocolLogService", new BatteryCollectorProtocolLogService());
        ReflectionTestUtils.setField(pollingService, "realtimeConsumer", Mockito.mock(BatteryModuleRealtimeConsumer.class));
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);
        ReflectionTestUtils.setField(service, "pollingService", pollingService);
        BatteryCollectorCommandQueueService commandQueueService = newCommandQueueService(modeStatusService, commandLogService);
        ReflectionTestUtils.setField(service, "commandQueueService", commandQueueService);
        injectConnectResistanceProcessor(service, commandQueueService, null, null);
    }

    private void injectCommandLogMapper(OptLogMapper optLogMapper) {
        ReflectionTestUtils.setField(commandLogService, "optLogMapper", optLogMapper);
    }

    private void injectCollectorDeviceStateService(BatteryDeviceStateService stateService) {
        ReflectionTestUtils.setField(collectorDeviceStateService, "batteryDeviceStateService", stateService);
    }

    private BatteryCollectorDeviceStateService newCollectorDeviceStateService(BatteryDeviceStateService stateService) {
        BatteryCollectorDeviceStateService service = new BatteryCollectorDeviceStateService();
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", stateService);
        return service;
    }

    private BatteryModeStatusService newModeStatusService() {
        BatteryModeStatusService modeStatusService = new BatteryModeStatusService();
        ReflectionTestUtils.setField(modeStatusService, "cacheAccessor", new TestCacheAccessor());
        return modeStatusService;
    }

    private BatteryCollectorPollingService newPollingService(BatteryCollectorProperties properties) {
        BatteryCollectorPollingService pollingService = new BatteryCollectorPollingService();
        ReflectionTestUtils.setField(pollingService, "properties", properties);
        ReflectionTestUtils.setField(pollingService, "protocolLogService", new BatteryCollectorProtocolLogService());
        ReflectionTestUtils.setField(pollingService, "realtimeConsumer", Mockito.mock(BatteryModuleRealtimeConsumer.class));
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);
        return pollingService;
    }

    private BatteryCollectorCommandQueueService newCommandQueueService(BatteryModeStatusService modeStatusService,
                                                                      BatteryCollectorCommandLogService commandLogService) {
        BatteryCollectorCommandQueueService commandQueueService = new BatteryCollectorCommandQueueService();
        ReflectionTestUtils.setField(commandQueueService, "batteryModeStatusService", modeStatusService);
        ReflectionTestUtils.setField(commandQueueService, "commandLogService", commandLogService);
        ReflectionTestUtils.setField(commandQueueService, "frameCodec", new BatteryCollectorFrameCodec());
        return commandQueueService;
    }

    private BatteryConnectResistanceCommandProcessor newConnectResistanceProcessor(
            BatteryCollectorCommandQueueService commandQueueService,
            BatteryModuleCellCompatibilityFillService compatibilityFillService,
            BatteryModuleRealtimeMapper realtimeMapper) {
        BatteryConnectResistanceCommandProcessor processor = new BatteryConnectResistanceCommandProcessor();
        ReflectionTestUtils.setField(processor, "compatibilityFillService", compatibilityFillService);
        ReflectionTestUtils.setField(processor, "realtimeMapper", realtimeMapper);
        ReflectionTestUtils.setField(processor, "snapshotService", Mockito.mock(BatteryModuleRealtimeSnapshotService.class));
        ReflectionTestUtils.setField(processor, "commandLogService", commandLogService);
        ReflectionTestUtils.setField(processor, "commandQueueService", commandQueueService);
        ReflectionTestUtils.setField(processor, "statisticsRefreshService",
                Mockito.mock(BatteryConnectResistanceStatisticsRefreshService.class));
        return processor;
    }

    private void injectConnectResistanceProcessor(BatteryCollectorService target,
                                                  BatteryCollectorCommandQueueService commandQueueService,
                                                  BatteryModuleCellCompatibilityFillService compatibilityFillService,
                                                  BatteryModuleRealtimeMapper realtimeMapper) {
        ReflectionTestUtils.setField(target, "connectResistanceCommandProcessor",
                newConnectResistanceProcessor(commandQueueService, compatibilityFillService, realtimeMapper));
    }

    private void injectModeStatusService(BatteryModeStatusService modeStatusService) {
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        BatteryCollectorCommandQueueService commandQueueService = newCommandQueueService(modeStatusService, commandLogService);
        ReflectionTestUtils.setField(service, "commandQueueService", commandQueueService);
        injectConnectResistanceProcessor(service, commandQueueService, null, null);
    }

    private static class TestCacheAccessor implements BatteryModeStatusService.CacheAccessor {
        private final Map<String, Object> cache = new HashMap<>();

        @Override
        public Object get(String cacheName, String key) {
            return cache.get(cacheName + ":" + key);
        }

        @Override
        public void put(String cacheName, String key, Object value) {
            cache.put(cacheName + ":" + key, value);
        }

        @Override
        public void remove(String cacheName, String key) {
            cache.remove(cacheName + ":" + key);
        }
    }

    private static class RecordingBatteryCollectorService extends BatteryCollectorService {
        private final List<Integer> writtenCommands = new ArrayList<>();
        private BatteryCollectorChannelState commandQueueTarget;
        private int enqueueCommandAfterWriteCount = -1;
        private boolean failWrites;

        @Override
        protected boolean isSerialPortOpen(SerialPort serialPort) {
            return true;
        }

        @Override
        protected int writeSerialBytes(SerialPort serialPort, byte[] bytes) {
            writtenCommands.add(bytes[6] & 0xFF);
            if (commandQueueTarget != null && writtenCommands.size() == enqueueCommandAfterWriteCount) {
                commandQueueTarget.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                        .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                        .address(8)
                        .requestCode(0x02)
                        .responseCode(0x82)
                        .payload(new byte[0])
                        .build());
            }
            return failWrites ? 0 : bytes.length;
        }
    }

    @Test
    void shouldUseSafeDefaultsForInvalidTimingConfig() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setLoopDelayMs(0L);
        properties.setRequestGapMs(null);
        properties.setModuleAddressMissThreshold(0);
        ReflectionTestUtils.setField(service, "properties", properties);
        injectModeStatusService(newModeStatusService());
        BatteryCollectorPollingService pollingService = newPollingService(properties);

        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setPollIntervalMs(null);
        channelConfig.setReadBufferSize(0);
        channelConfig.setReceiveBufferLimit(1);
        channelConfig.setResponseTimeoutMs(-1L);
        channelConfig.setMaxRetryCount(-1);
        channelConfig.setBaudRate(0);
        channelConfig.setDataBits(9);
        channelConfig.setStopBits(4);
        channelConfig.setParity(5);
        channelConfig.setTimeoutMs(0);
        channelConfig.setModuleAddressStart(0);
        channelConfig.setModuleAddressEnd(300);

        Assertions.assertEquals(300, service.resolveLoopDelayMs());
        Assertions.assertEquals(120, service.resolveRequestGapMs());
        List<Integer> addresses = pollingService.resolvePollingAddresses(
                new BatteryCollectorChannelState(channelConfig), true);
        Assertions.assertEquals(0, addresses.get(0));
        Assertions.assertEquals(300, addresses.get(addresses.size() - 1));
        Assertions.assertEquals(2048, service.resolveReadBufferSize(channelConfig));
        Assertions.assertEquals(64, service.resolveReceiveBufferLimit(channelConfig));
        BatteryCollectorChannelState addressCacheState = new BatteryCollectorChannelState(channelConfig);
        addressCacheState.getActiveModuleAddresses().add(8);
        pollingService.updateModuleAddressCache(addressCacheState, 8, false);
        pollingService.updateModuleAddressCache(addressCacheState, 8, false);
        Assertions.assertTrue(addressCacheState.getActiveModuleAddresses().contains(8));
        pollingService.updateModuleAddressCache(addressCacheState, 8, false);
        Assertions.assertFalse(addressCacheState.getActiveModuleAddresses().contains(8));
    }

    @Test
    void shouldReturnEmptyCollectorMetrics() {
        BatteryCollectorMetrics metrics = service.getMetrics();

        Assertions.assertNotNull(metrics.getGeneratedAt());
        Assertions.assertEquals(0, metrics.getChannelCount());
        Assertions.assertEquals(0, metrics.getEnabledChannelCount());
        Assertions.assertEquals(0, metrics.getOpenedChannelCount());
        Assertions.assertEquals(0, metrics.getTotalQueuedModuleCommandCount());
        Assertions.assertTrue(metrics.getChannels().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldBuildCollectorMetricsFromRunningChannelAndCachedSnapshot() {
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
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

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
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", realtimeSnapshotService);

        BatteryCollectorMetrics metrics = service.getMetrics();

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
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldResetModuleAddressCacheForSelectedChannel() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryModuleRealtimeSnapshotService realtimeSnapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", realtimeSnapshotService);
        state.getActiveModuleAddresses().add(8);
        state.getModuleAddressMissCounts().put(8, 2);
        state.getFullDiscoveryRequested().set(false);

        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

        Assertions.assertTrue(service.resetModuleAddressCache("battery-group-1"));
        Assertions.assertTrue(state.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(state.getModuleAddressMissCounts().isEmpty());
        Assertions.assertTrue(state.getFullDiscoveryRequested().get());
        Mockito.verify(realtimeSnapshotService).evict(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldResetModuleAddressCacheForSelectedBatteryGroupOnly() {
        BatteryCollectorChannelConfig groupOneConfig = new BatteryCollectorChannelConfig();
        groupOneConfig.setBatteryGroup(1);
        groupOneConfig.setName("battery-group-1");
        BatteryCollectorChannelState groupOneState = new BatteryCollectorChannelState(groupOneConfig);
        groupOneState.getActiveModuleAddresses().add(8);
        groupOneState.getModuleAddressMissCounts().put(8, 2);
        groupOneState.getFullDiscoveryRequested().set(false);

        BatteryCollectorChannelConfig groupTwoConfig = new BatteryCollectorChannelConfig();
        groupTwoConfig.setBatteryGroup(2);
        groupTwoConfig.setName("battery-group-2");
        BatteryCollectorChannelState groupTwoState = new BatteryCollectorChannelState(groupTwoConfig);
        groupTwoState.getActiveModuleAddresses().add(9);
        groupTwoState.getModuleAddressMissCounts().put(9, 1);
        groupTwoState.getFullDiscoveryRequested().set(false);

        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(groupOneState);
        channelStates.add(groupTwoState);
        BatteryModuleRealtimeSnapshotService realtimeSnapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", realtimeSnapshotService);

        Assertions.assertTrue(service.resetModuleAddressCacheByBatteryGroup(1));

        Assertions.assertTrue(groupOneState.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(groupOneState.getModuleAddressMissCounts().isEmpty());
        Assertions.assertTrue(groupOneState.getFullDiscoveryRequested().get());
        Assertions.assertFalse(groupTwoState.getActiveModuleAddresses().isEmpty());
        Assertions.assertFalse(groupTwoState.getModuleAddressMissCounts().isEmpty());
        Assertions.assertFalse(groupTwoState.getFullDiscoveryRequested().get());
        Mockito.verify(realtimeSnapshotService).evict(1);
        Mockito.verify(realtimeSnapshotService, Mockito.never()).evict(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldClearDeviceStateDedupCacheForSelectedBatteryGroupOnly() {
        BatteryCollectorChannelConfig groupOneConfig = new BatteryCollectorChannelConfig();
        groupOneConfig.setBatteryGroup(1);
        groupOneConfig.setName("battery-group-1");
        BatteryCollectorChannelState groupOneState = new BatteryCollectorChannelState(groupOneConfig);

        BatteryCollectorChannelConfig groupTwoConfig = new BatteryCollectorChannelConfig();
        groupTwoConfig.setBatteryGroup(2);
        groupTwoConfig.setName("battery-group-2");
        BatteryCollectorChannelState groupTwoState = new BatteryCollectorChannelState(groupTwoConfig);

        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(groupOneState);
        channelStates.add(groupTwoState);

        Map<String, String> lastStateValues =
                (Map<String, String>) ReflectionTestUtils.getField(collectorDeviceStateService, "lastStateValues");
        lastStateValues.put("1:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-1:CHANNEL_OPEN", "open");
        lastStateValues.put("battery-group-1:8:MODULE_ACTIVE", "active");
        lastStateValues.put("2:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-2:CHANNEL_OPEN", "open");

        int removed = service.clearDeviceStateDedupCacheByBatteryGroup(1);

        Assertions.assertEquals(3, removed);
        Assertions.assertFalse(lastStateValues.containsKey("1:GROUP_246_FRESHNESS"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:CHANNEL_OPEN"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:8:MODULE_ACTIVE"));
        Assertions.assertTrue(lastStateValues.containsKey("2:GROUP_246_FRESHNESS"));
        Assertions.assertTrue(lastStateValues.containsKey("battery-group-2:CHANNEL_OPEN"));
    }

    @Test
    void shouldRequireFullDiscoveryWhenOnlyGroupModuleAddressIsCached() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(246);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(246);
        state.getFullDiscoveryRequested().set(false);

        Assertions.assertTrue(pollingService.shouldRunFullDiscovery(state, System.currentTimeMillis()));
    }

    @Test
    void shouldDetectActiveCellModuleAddress() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(246);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);
        state.getActiveModuleAddresses().add(246);
        state.getFullDiscoveryRequested().set(false);

        Assertions.assertFalse(pollingService.shouldRunFullDiscovery(state, System.currentTimeMillis()));
    }

    @Test
    void shouldMatchCurrentPendingResponseByCommandAndAddress() {
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        state.setExpectedResponseCode(0x81);
        state.setPendingCommand(BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.MODULE_INFO,
                8,
                new byte[0],
                true));
        BatteryCollectorFrame currentResponse = BatteryCollectorFrame.builder()
                .address(8)
                .command(0x81)
                .payload(new byte[0])
                .build();
        BatteryCollectorFrame lateResponse = BatteryCollectorFrame.builder()
                .address(7)
                .command(0x81)
                .payload(new byte[0])
                .build();

        Boolean currentMatched = ReflectionTestUtils.invokeMethod(service,
                "isCurrentPendingResponse", state, currentResponse);
        Boolean lateMatched = ReflectionTestUtils.invokeMethod(service,
                "isCurrentPendingResponse", state, lateResponse);

        Assertions.assertTrue(Boolean.TRUE.equals(currentMatched));
        Assertions.assertFalse(Boolean.TRUE.equals(lateMatched));
    }

    @Test
    void shouldNotCompletePendingRequestWhenOnlyCommandOrAddressMatches() {
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        state.setExpectedResponseCode(0x81);
        state.setPendingCommand(BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.MODULE_INFO,
                8,
                new byte[0],
                true));
        BatteryCollectorFrame sameCommandDifferentAddress = BatteryCollectorFrame.builder()
                .address(7)
                .command(0x81)
                .payload(new byte[0])
                .build();
        BatteryCollectorFrame sameAddressDifferentCommand = BatteryCollectorFrame.builder()
                .address(8)
                .command(0x91)
                .payload(new byte[0])
                .build();

        Boolean sameCommandMatched = ReflectionTestUtils.invokeMethod(service,
                "isCurrentPendingResponse", state, sameCommandDifferentAddress);
        Boolean sameAddressMatched = ReflectionTestUtils.invokeMethod(service,
                "isCurrentPendingResponse", state, sameAddressDifferentCommand);

        Assertions.assertFalse(Boolean.TRUE.equals(sameCommandMatched));
        Assertions.assertFalse(Boolean.TRUE.equals(sameAddressMatched));
    }

    @Test
    void commandQueueShouldMatchPendingResponseByCommandAndAddress() {
        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), commandLogService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        state.setExpectedResponseCode(0x82);
        state.setPendingCommand(BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false));
        BatteryCollectorFrame matched = BatteryCollectorFrame.builder()
                .address(8)
                .command(0x82)
                .payload(new byte[0])
                .build();
        BatteryCollectorFrame latePollResponse = BatteryCollectorFrame.builder()
                .address(7)
                .command(0x82)
                .payload(new byte[0])
                .build();

        Assertions.assertTrue(commandQueueService.isCurrentPendingResponse(state, matched));
        Assertions.assertFalse(commandQueueService.isCurrentPendingResponse(state, latePollResponse));
    }
    @Test
    void shouldRemoveCachedModuleAddressAfterConsecutiveMisses() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        properties.setModuleAddressMissThreshold(2);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);
        BatteryCollectorPollingService pollingService = newPollingService(properties);

        pollingService.updateModuleAddressCache(state, 8, false);
        Assertions.assertTrue(state.getActiveModuleAddresses().contains(8));

        pollingService.updateModuleAddressCache(state, 8, false);

        Assertions.assertFalse(state.getActiveModuleAddresses().contains(8));
        Assertions.assertFalse(state.getModuleAddressMissCounts().containsKey(8));
    }

    @Test
    void shouldPersistGroupModuleFreshnessStaleWhen246HasNoResponseBeforeCached() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);

        pollingService.updateModuleAddressCache(state, 246, false);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService).upsert(captor.capture());
        BatteryDeviceState deviceState = captor.getValue();
        Assertions.assertEquals(BatteryDeviceStateConstants.ScopeType.PACK, deviceState.getScopeType());
        Assertions.assertEquals("1", deviceState.getScopeKey());
        Assertions.assertEquals(1, deviceState.getPackNum());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, deviceState.getStateCode());
        Assertions.assertEquals("stale", deviceState.getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.WARN, deviceState.getStateLevel());
        Assertions.assertFalse(state.getActiveModuleAddresses().contains(246));
    }

    @Test
    void shouldPersistGroupModuleFreshnessWhenAddressCacheDisabled() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(false);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);

        pollingService.updateModuleAddressCache(state, 246, true);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService).upsert(captor.capture());
        BatteryDeviceState deviceState = captor.getValue();
        Assertions.assertEquals(BatteryDeviceStateConstants.ScopeType.PACK, deviceState.getScopeType());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, deviceState.getStateCode());
        Assertions.assertEquals("fresh", deviceState.getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.NORMAL, deviceState.getStateLevel());
        Assertions.assertFalse(state.getActiveModuleAddresses().contains(246));
    }

    @Test
    void shouldDeduplicateChannelOpenAndPersistRecoveryBoundary() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());

        collectorDeviceStateService.persistSerialPortState(state, true);
        collectorDeviceStateService.persistSerialPortState(state, true);
        collectorDeviceStateService.persistSerialPortState(state, false);
        collectorDeviceStateService.persistSerialPortState(state, false);
        collectorDeviceStateService.persistSerialPortState(state, true);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(3)).upsert(captor.capture());
        List<BatteryDeviceState> states = captor.getAllValues();
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, states.get(0).getStateCode());
        Assertions.assertEquals("open", states.get(0).getStateValue());
        Assertions.assertEquals("closed", states.get(1).getStateValue());
        Assertions.assertEquals("open", states.get(2).getStateValue());
    }

    @Test
    void shouldDeduplicateChannelErrorAndClearAfterOpen() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());

        collectorDeviceStateService.persistChannelError(state, new IllegalStateException("open failed"));
        collectorDeviceStateService.persistChannelError(state, new IllegalStateException("open failed"));
        collectorDeviceStateService.persistSerialPortState(state, true);
        collectorDeviceStateService.persistSerialPortState(state, true);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(3)).upsert(captor.capture());
        List<BatteryDeviceState> states = captor.getAllValues();
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, states.get(0).getStateCode());
        Assertions.assertEquals("open failed", states.get(0).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, states.get(1).getStateCode());
        Assertions.assertEquals("open", states.get(1).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, states.get(2).getStateCode());
        Assertions.assertEquals("cleared", states.get(2).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.NORMAL, states.get(2).getStateLevel());
    }

    @Test
    void shouldDeduplicateModuleTimeoutAndPersistRecoveredOnResponse() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.MODULE_INFO,
                8,
                new byte[0],
                true);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);

        collectorDeviceStateService.persistModuleTimeout(state, pendingRequest);
        collectorDeviceStateService.persistModuleTimeout(state, pendingRequest);
        pollingService.updateModuleAddressCache(state, 8, true);
        pollingService.updateModuleAddressCache(state, 8, true);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(3)).upsert(captor.capture());
        List<BatteryDeviceState> states = captor.getAllValues();
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, states.get(0).getStateCode());
        Assertions.assertEquals("01/81", states.get(0).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, states.get(1).getStateCode());
        Assertions.assertEquals("active", states.get(1).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, states.get(2).getStateCode());
        Assertions.assertEquals("recovered", states.get(2).getStateValue());
    }

    @Test
    void shouldDeduplicateModuleActiveAndPersistInactiveAfterMissThreshold() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        properties.setModuleAddressMissThreshold(2);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);

        pollingService.updateModuleAddressCache(state, 8, true);
        pollingService.updateModuleAddressCache(state, 8, true);
        pollingService.updateModuleAddressCache(state, 8, false);
        pollingService.updateModuleAddressCache(state, 8, false);
        pollingService.updateModuleAddressCache(state, 8, false);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(2)).upsert(captor.capture());
        List<BatteryDeviceState> states = captor.getAllValues();
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, states.get(0).getStateCode());
        Assertions.assertEquals("active", states.get(0).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, states.get(1).getStateCode());
        Assertions.assertEquals("inactive", states.get(1).getStateValue());
    }

    @Test
    void shouldDeduplicateGroup246FreshnessAndPersistFreshRecovery() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(false);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectCollectorDeviceStateService(stateService);
        BatteryCollectorPollingService pollingService = newPollingService(properties);
        ReflectionTestUtils.setField(pollingService, "collectorDeviceStateService", collectorDeviceStateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());

        pollingService.updateModuleAddressCache(state, 246, false);
        pollingService.updateModuleAddressCache(state, 246, false);
        pollingService.updateModuleAddressCache(state, 246, true);
        pollingService.updateModuleAddressCache(state, 246, true);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(2)).upsert(captor.capture());
        List<BatteryDeviceState> states = captor.getAllValues();
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, states.get(0).getStateCode());
        Assertions.assertEquals("stale", states.get(0).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.WARN, states.get(0).getStateLevel());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, states.get(1).getStateCode());
        Assertions.assertEquals("fresh", states.get(1).getStateValue());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.NORMAL, states.get(1).getStateLevel());
    }

    @Test
    void shouldOnlyTreatModuleAndPackStateCacheKeysAsHighCardinality() {
        Boolean channelOpen = collectorDeviceStateService.isHighCardinalityStateCacheKey("battery-group-1:CHANNEL_OPEN");
        Boolean channelError = collectorDeviceStateService.isHighCardinalityStateCacheKey("battery-group-1:CHANNEL_ERROR");
        Boolean moduleTimeout = collectorDeviceStateService.isHighCardinalityStateCacheKey("battery-group-1:8:MODULE_TIMEOUT");
        Boolean moduleActive = collectorDeviceStateService.isHighCardinalityStateCacheKey("battery-group-1:8:MODULE_ACTIVE");
        Boolean group246 = collectorDeviceStateService.isHighCardinalityStateCacheKey("1:GROUP_246_FRESHNESS");

        Assertions.assertFalse(Boolean.TRUE.equals(channelOpen));
        Assertions.assertFalse(Boolean.TRUE.equals(channelError));
        Assertions.assertTrue(Boolean.TRUE.equals(moduleTimeout));
        Assertions.assertTrue(Boolean.TRUE.equals(moduleActive));
        Assertions.assertTrue(Boolean.TRUE.equals(group246));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAlwaysPollGroupModuleAddressEvenWhenCellRangeEndsAt245() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        BatteryCollectorPollingService pollingService = newPollingService(properties);

        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(245);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);

        List<Integer> fullDiscoveryAddresses = pollingService.resolvePollingAddresses(state, true);
        List<Integer> cachedAddresses = pollingService.resolvePollingAddresses(state, false);

        Assertions.assertNotNull(fullDiscoveryAddresses);
        Assertions.assertTrue(fullDiscoveryAddresses.contains(246));
        Assertions.assertEquals(246, fullDiscoveryAddresses.get(fullDiscoveryAddresses.size() - 1));
        Assertions.assertNotNull(cachedAddresses);
        Assertions.assertEquals(Arrays.asList(8, 246), cachedAddresses);
    }

    @Test
    void shouldSkipRemainingCellDiscoveryAfterExpectedCellResponsesButKeepGroupModule() {
        BatteryCollectorPollingService pollingService = newPollingService(new BatteryCollectorProperties());

        Assertions.assertFalse(pollingService.shouldSkipRemainingCellDiscovery(true, 8, 23, 24));
        Assertions.assertTrue(pollingService.shouldSkipRemainingCellDiscovery(true, 24, 24, 24));
        Assertions.assertFalse(pollingService.shouldSkipRemainingCellDiscovery(true, 246, 24, 24));
        Assertions.assertFalse(pollingService.shouldSkipRemainingCellDiscovery(false, 24, 24, 24));
        Assertions.assertFalse(pollingService.shouldSkipRemainingCellDiscovery(true, 24, 24, 0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueueModuleCommandForActiveChannel() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setConfigId(1L);
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);

        boolean queued = service.submitModuleCommand("battery-group-1", BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .build());

        Assertions.assertTrue(queued);
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand queuedCommand = state.getQueuedModuleCommands().peek();
        Assertions.assertNotNull(queuedCommand);
        Assertions.assertEquals(1L, queuedCommand.getConfigId());
        Assertions.assertEquals(2, queuedCommand.getBatteryGroup());
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(8, modeInfo.getAddress());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateCommandOptLogWithExplicitModuleCommandFields() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setConfigId(1L);
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

        boolean queued = service.submitModuleCommand("battery-group-1", BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[]{0x01, 0x23})
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .build());

        Assertions.assertTrue(queued);
        ArgumentCaptor<OptLog> captor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(captor.capture());
        OptLog optLog = captor.getValue();
        Assertions.assertNotNull(optLog.getId());
        Assertions.assertEquals(1L, optLog.getConfigId());
        Assertions.assertEquals(2, optLog.getPackNum());
        Assertions.assertEquals(BatteryTestEnum._99.getDictValue(), optLog.getType());
        Assertions.assertEquals(BatteryDeviceStateConstants.Source.COLLECTOR, optLog.getSource());
        Assertions.assertEquals("battery-group-1", optLog.getChannelName());
        Assertions.assertEquals("module", optLog.getTargetType());
        Assertions.assertEquals(8, optLog.getTargetAddress());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, optLog.getMode());
        Assertions.assertEquals(BatteryDeviceStateConstants.CommandStatus.PENDING, optLog.getStatus());
        Assertions.assertEquals(0x02, optLog.getRequestCode());
        Assertions.assertEquals(0x82, optLog.getResponseCode());
        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", optLog.getProtocolCode());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getDescription(), optLog.getCommandName());
        Assertions.assertEquals("0123", optLog.getRequestPayload());
        Assertions.assertNull(optLog.getResponsePayload());
        Assertions.assertNull(optLog.getErrorMessage());
        Assertions.assertNotNull(optLog.getStartedAt());
        Assertions.assertNotNull(state.getQueuedModuleCommands().peek().getOptLogId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPersistRunningWorkModeWithCommandOptLogId() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        ReflectionTestUtils.setField(modeStatusService, "batteryDeviceStateService", stateService);
        injectModeStatusService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setConfigId(1L);
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

        boolean queued = service.submitModuleCommand("battery-group-1", BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[]{0x01})
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .build());

        Assertions.assertTrue(queued);
        ArgumentCaptor<OptLog> optLogCaptor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(optLogCaptor.capture());
        ArgumentCaptor<BatteryDeviceState> stateCaptor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService).upsert(stateCaptor.capture());
        Assertions.assertEquals(optLogCaptor.getValue().getId(), stateCaptor.getValue().getOptLogId());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.WORK_MODE, stateCaptor.getValue().getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.RUNNING, stateCaptor.getValue().getStateLevel());
    }

    @Test
    void shouldCreateCommandOptLogWithNullRequestPayloadForEmptyPayload() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        injectModeStatusService(newModeStatusService());
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        @SuppressWarnings("unchecked")
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

        Assertions.assertTrue(service.submitModuleCommand("battery-group-1", BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build()));

        ArgumentCaptor<OptLog> captor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(captor.capture());
        Assertions.assertNull(captor.getValue().getRequestPayload());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRequeueModuleCommandWhenSerialPortIsUnavailable() {
        ReflectionTestUtils.setField(service, "frameCodec", new BatteryCollectorFrameCodec());
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build());
        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), commandLogService);

        commandQueueService.processNextQueuedCommand(
                state,
                (frame, pendingRequest, waitingState) -> false,
                (frame, command) -> false);

        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
    }

    @Test
    void shouldBreakImmediateModuleCommandProcessingWhenWriteFails() {
        RecordingBatteryCollectorService recordingService = new RecordingBatteryCollectorService();
        ReflectionTestUtils.setField(recordingService, "frameCodec", new BatteryCollectorFrameCodec());
        ReflectionTestUtils.setField(recordingService, "commandQueueService",
                newCommandQueueService(newModeStatusService(), commandLogService));
        ReflectionTestUtils.setField(recordingService, "running", true);
        recordingService.failWrites = true;
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setSerialPort(SerialPort.getCommPort("battery-test"));
        state.getQueuedModuleCommands().offer(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build());

        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), commandLogService);
        commandQueueService.processNextQueuedCommand(
                state,
                (frame, pendingRequest, waitingState) -> {
                    recordingService.writeSerialBytes(state.getSerialPort(), frame.toByteArray());
                    return false;
                },
                (frame, command) -> {
                    recordingService.writeSerialBytes(state.getSerialPort(), frame.toByteArray());
                    return false;
                });

        Assertions.assertEquals(Arrays.asList(0x02), recordingService.writtenCommands);
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        Assertions.assertNull(state.getPendingCommand());
    }

    @Test
    void shouldRunQueuedModuleCommandBetweenAutoPollAddresses() {
        RecordingBatteryCollectorService recordingService = new RecordingBatteryCollectorService();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setRequestGapMs(1L);
        ReflectionTestUtils.setField(recordingService, "properties", properties);
        ReflectionTestUtils.setField(recordingService, "frameCodec", new BatteryCollectorFrameCodec());
        ReflectionTestUtils.setField(recordingService, "realtimeConsumer", Mockito.mock(BatteryModuleRealtimeConsumer.class));
        ReflectionTestUtils.setField(recordingService, "protocolLogService", new BatteryCollectorProtocolLogService());
        ReflectionTestUtils.setField(recordingService, "collectorDeviceStateService",
                newCollectorDeviceStateService(Mockito.mock(BatteryDeviceStateService.class)));
        BatteryCollectorCommandLogService recordingCommandLogService = new BatteryCollectorCommandLogService();
        ReflectionTestUtils.setField(recordingCommandLogService, "optLogMapper", Mockito.mock(OptLogMapper.class));
        ReflectionTestUtils.setField(recordingService, "commandLogService", recordingCommandLogService);
        ReflectionTestUtils.setField(recordingService, "commandQueueService",
                newCommandQueueService(newModeStatusService(), recordingCommandLogService));
        ReflectionTestUtils.setField(recordingService, "running", true);

        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(2);
        channelConfig.setResponseTimeoutMs(1L);
        channelConfig.setMaxRetryCount(0);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setSerialPort(SerialPort.getCommPort("battery-test"));
        recordingService.commandQueueTarget = state;
        recordingService.enqueueCommandAfterWriteCount = 1;

        BatteryCollectorPollingService pollingService = newPollingService(properties);
        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), recordingCommandLogService);
        pollingService.pollOnce(
                state,
                address -> {
                    BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(address, 0x01, new byte[0]);
                    recordingService.writeSerialBytes(state.getSerialPort(), frame.toByteArray());
                    state.setPendingCommand(BatteryPendingRequest.fromProtocolCode(
                            BatteryDeviceProtocolCode.MODULE_INFO, address, new byte[0], true));
                },
                () -> {
                    if (state.getPendingCommand() != null) {
                        BatteryPendingRequest pendingRequest = state.getPendingCommand();
                        state.setLastPendingTimedOut(true);
                        state.setPendingCommand(null);
                        if (!pendingRequest.isAutoPoll()) {
                            commandQueueService.completeTimedOutExplicitCommand(state, pendingRequest);
                        }
                    }
                },
                queuedState -> commandQueueService.processNextQueuedCommand(
                        queuedState,
                        (frame, pendingRequest, waitingState) -> {
                            recordingService.writeSerialBytes(queuedState.getSerialPort(), frame.toByteArray());
                            queuedState.setPendingCommand(pendingRequest);
                            return true;
                        },
                        (frame, command) -> {
                            recordingService.writeSerialBytes(queuedState.getSerialPort(), frame.toByteArray());
                            return true;
                        }),
                () -> {
                });

        Assertions.assertEquals(Arrays.asList(0x01, 0x02, 0x01, 0x01), recordingService.writtenCommands);
        Assertions.assertTrue(state.getQueuedModuleCommands().isEmpty());
        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", state.getLastCompletedModuleCommandName());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
    }

    @Test
    void shouldResetAddressCacheAfterSuccessfulAddressCommandResponse() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);
        state.getModuleAddressMissCounts().put(8, 2);
        state.getFullDiscoveryRequested().set(false);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SET_MODULE_ADDRESS,
                8,
                new byte[]{9},
                false);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x88, new byte[]{0});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("SET_MODULE_ADDRESS", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x88, state.getLastCompletedModuleResponseCode());
        Assertions.assertTrue(state.isLastCompletedModuleCommandSuccess());
        Assertions.assertTrue(state.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(state.getModuleAddressMissCounts().isEmpty());
        Assertions.assertTrue(state.getFullDiscoveryRequested().get());
    }

    @Test
    void shouldStopModeCacheAfterCommandResponse() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);
        pendingRequest.setConfigId(1L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x82, new byte[]{0});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);
        BatteryModeInfo modeInfo = modeStatusService.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getLastMode());
        Assertions.assertEquals(8, modeInfo.getLastAddress());
    }

    @Test
    void shouldKeepAddressCacheAfterFailedAddressCommandResponse() {
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        state.getActiveModuleAddresses().add(8);
        state.getFullDiscoveryRequested().set(false);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                246,
                new byte[]{1, 2, 3, 4, 5, 6, 7},
                false);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(246, 0xA8, new byte[]{0, 5, 2});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("AUTO_SET_MODULE_ADDRESS", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0xA8, state.getLastCompletedModuleResponseCode());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
        Assertions.assertFalse(state.getActiveModuleAddresses().isEmpty());
        Assertions.assertFalse(state.getFullDiscoveryRequested().get());
    }

    @Test
    void shouldQueueNextAutoAddressStepAfterGroupStartResponse() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setConfigId(1L);
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                246,
                new byte[]{0, 0, 0, 0, 0, 0, 1},
                false);
        pendingRequest.setConfigId(1L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
        pendingRequest.setAutoAddressBatteryCount(2);
        pendingRequest.setAutoAddressBatterySpecification(2);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(246, 0xA8, new byte[]{1, 0, 0});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand command = state.getQueuedModuleCommands().peek();
        Assertions.assertNotNull(command);
        Assertions.assertEquals(1, command.getAddress());
        Assertions.assertEquals(Integer.valueOf(0xA8), command.getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0, 20, 2, 0, 0, 0, 1}, command.getPayload());
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getAddress());
    }

    @Test
    void shouldQueueStopFramesAndKeepModeRunningAfterLastAutoAddressResponse() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_AUTO_MODEL_NUM, 2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        state.getActiveModuleAddresses().add(1);
        state.getFullDiscoveryRequested().set(false);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                2,
                new byte[]{0, 20, 2, 0, 0, 0, 1},
                false);
        pendingRequest.setConfigId(1L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
        pendingRequest.setAutoAddressBatteryCount(2);
        pendingRequest.setAutoAddressBatterySpecification(2);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(2, 0xA8, new byte[]{0, 21, 2});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals(2, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand stopCell = state.getQueuedModuleCommands().poll();
        BatteryModuleControlCommand stopGroup = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(stopCell);
        Assertions.assertNotNull(stopGroup);
        Assertions.assertEquals(2, stopCell.getAddress());
        Assertions.assertEquals(246, stopGroup.getAddress());
        Assertions.assertNull(stopCell.getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0, 21, 2, 0, 0, 0, 2}, stopCell.getPayload());
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(2, modeInfo.getAddress());
        Assertions.assertTrue(state.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(state.getFullDiscoveryRequested().get());
    }

    @Test
    void shouldStopAutoAddressModeAfterStopGroupFrameWritten() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorCommandQueueService commandQueueService = newCommandQueueService(modeStatusService, commandLogService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_AUTO_MODEL_NUM, 2);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryModuleControlCommand stopGroup = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS)
                .address(246)
                .requestCode(0x18)
                .payload(new byte[]{0, 21, 2, 0, 0, 0, 2})
                .batteryGroup(2)
                .mode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM)
                .autoAddressBatteryCount(2)
                .build();

        commandQueueService.markModeStopped(stopGroup, true);
        BatteryModeInfo modeInfo = modeStatusService.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM, modeInfo.getLastMode());
        Assertions.assertEquals(2, modeInfo.getLastAddress());
    }

    @Test
    void shouldStopModeOnAutoAddressStartFailure() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_AUTO_MODEL_NUM, 246);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                246,
                new byte[]{0, 0, 0, 0, 0, 0, 1},
                false);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
        // 246 启动失败：payload[0] != START_SET_ADDRESS(1)
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(246, 0xA8, new byte[]{0, 0, 0});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
    }

    @Test
    void shouldStopModeOnAutoAddressMidCellTimeout() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorCommandQueueService commandQueueService = newCommandQueueService(modeStatusService, commandLogService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_AUTO_MODEL_NUM, 1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                1,
                new byte[]{0, 20, 2, 0, 0, 0, 1},
                false);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);

        // 超时场景：由队列服务完成显式命令超时收尾。
        commandQueueService.completeTimedOutExplicitCommand(state, pendingRequest);

        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
    }

    @Test
    void shouldQueueStopFramesAfterLastAutoAddressCellResponse() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_AUTO_MODEL_NUM, 2);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        // 最后一个单体（地址 2）的响应
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                2,
                new byte[]{0, 20, 2, 0, 0, 0, 1},
                false);
        pendingRequest.setConfigId(1L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
        pendingRequest.setAutoAddressBatteryCount(2);
        pendingRequest.setAutoAddressBatterySpecification(2);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(2, 0xA8, new byte[]{0, 21, 2});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        // 应排队两个停止帧：单体停止 + 组停止
        Assertions.assertEquals(2, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand stopCell = state.getQueuedModuleCommands().poll();
        BatteryModuleControlCommand stopGroup = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(stopCell);
        Assertions.assertNotNull(stopGroup);
        Assertions.assertEquals(2, stopCell.getAddress());
        Assertions.assertEquals(246, stopGroup.getAddress());
        Assertions.assertNull(stopCell.getResponseCode());
        Assertions.assertNull(stopGroup.getResponseCode());
    }

    @Test
    void shouldKeepConnectResistanceModeRunningAfterStartFrameWritten() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(2);
        BatteryModuleControlCommand command = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(0)
                .requestCode(0x0F)
                .batteryGroup(2)
                .mode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE)
                .build();

        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(modeStatusService, commandLogService);
        Assertions.assertFalse(commandQueueService.shouldStopModeAfterNoResponseCommand(command));
        modeStatusService.markRunning(command.getBatteryGroup(), command.getMode(), command.getAddress());
        BatteryModeInfo modeInfo = modeStatusService.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(0, modeInfo.getAddress());
    }

    @Test
    void shouldTreatMatchedDataResponseAsModuleCommandSuccess() {
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("GET_CONNECT_STRIP_RESISTANCE_VOLTAGE", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x91, state.getLastCompletedModuleResponseCode());
        Assertions.assertTrue(state.isLastCompletedModuleCommandSuccess());
    }

    @Test
    void shouldNotWriteConnectResistanceCacheFrom91VoltageResponseWhenCurrentIsMissing() {
        BatteryModuleCellCompatibilityFillService compatibilityFillService =
                Mockito.mock(BatteryModuleCellCompatibilityFillService.class);
        injectConnectResistanceProcessor(service,
                newCommandQueueService(newModeStatusService(), commandLogService),
                compatibilityFillService,
                null);
        
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setConnectResistanceNextAddress(9);
        pendingRequest.setConnectResistanceMaxAddress(8);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x00, 0x01, (byte) 0xD4, (byte) 0xC0, 0x00, 0x01, (byte) 0xD8, (byte) 0xA8});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("GET_CONNECT_STRIP_RESISTANCE_VOLTAGE", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x91, state.getLastCompletedModuleResponseCode());
        Assertions.assertTrue(state.isLastCompletedModuleCommandSuccess());
        Mockito.verify(compatibilityFillService, Mockito.never()).putConnectResistance(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldWriteConnectResistanceCacheFrom91VoltageResponseWhenCurrentIsAvailable() {
        BatteryModuleCellCompatibilityFillService compatibilityFillService =
                Mockito.mock(BatteryModuleCellCompatibilityFillService.class);
        
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        injectConnectResistanceProcessor(service,
                newCommandQueueService(newModeStatusService(), commandLogService),
                compatibilityFillService,
                realtimeMapper);

        // Mock current in realtime snapshot.
        com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime groupRealtime =
                new com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime();
        groupRealtime.setChargeDischargeCurrent(10.0d); // 10.0 A
        BatteryModuleRealtimeSnapshotService snapshotService = (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(
                ReflectionTestUtils.getField(service, "connectResistanceCommandProcessor"), "snapshotService");
        Mockito.when(snapshotService.getCachedSnapshot(2)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(2)
                .group(groupRealtime)
                .build());


        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setConnectResistanceNextAddress(9);
        pendingRequest.setConnectResistanceMaxAddress(8);
        
        // batteryVoltage = 12.0V (0x0001D4C0 = 120000 0.1mV)
        // testVoltage = 12.1V (0x0001D8A8 = 121000 0.1mV)
        // deltaV = 0.1V, current = 10.0A -> resistance = 0.1/10.0 * 1,000,000 = 10,000 uΩ
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x00, 0x01, (byte) 0xD4, (byte) 0xC0, 0x00, 0x01, (byte) 0xD8, (byte) 0xA8});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("GET_CONNECT_STRIP_RESISTANCE_VOLTAGE", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x91, state.getLastCompletedModuleResponseCode());
        Assertions.assertTrue(state.isLastCompletedModuleCommandSuccess());
        
        // Verify cache fill is invoked with 10000.0 uΩ
        Mockito.verify(compatibilityFillService).putConnectResistance(2, 8, 10000.0d);
        
        // Verify mapper writes the target cell directly without querying current cells.
        Mockito.verify(realtimeMapper, Mockito.never()).selectCells(2);
        Mockito.verify(realtimeMapper).upsertCell(Mockito.argThat(cell ->
                cell.getPackNum() == 2 && cell.getBatNum() == 8 && cell.getResistanceRageSlip() == 10000.0d));
    }

    @Test
    void shouldNotWriteConnectResistanceCacheFromShort91VoltageResponse() {
        BatteryModuleCellCompatibilityFillService compatibilityFillService =
                Mockito.mock(BatteryModuleCellCompatibilityFillService.class);
        injectConnectResistanceProcessor(service,
                newCommandQueueService(newModeStatusService(), commandLogService),
                compatibilityFillService,
                null);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(newChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        pendingRequest.setBatteryGroup(2);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x01, 0x02, 0x03});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals("GET_CONNECT_STRIP_RESISTANCE_VOLTAGE", state.getLastCompletedModuleCommandName());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
        Mockito.verify(compatibilityFillService, Mockito.never()).putConnectResistance(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldQueueNextConnectResistanceReadWithoutUpdatingOptLogForIntermediate91Response() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        pendingRequest.setOptLogId(10L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        pendingRequest.setConnectResistanceNextAddress(9);
        pendingRequest.setConnectResistanceMaxAddress(10);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Mockito.verifyNoInteractions(optLogMapper);
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand nextCommand = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(nextCommand);
        Assertions.assertEquals(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                nextCommand.getProtocolCode());
        Assertions.assertEquals(9, nextCommand.getAddress());
        Assertions.assertEquals(10L, nextCommand.getOptLogId());
        Assertions.assertEquals(Integer.valueOf(10), nextCommand.getConnectResistanceNextAddress());
        Assertions.assertEquals(Integer.valueOf(10), nextCommand.getConnectResistanceMaxAddress());
    }

    @Test
    void shouldCarryConnectResistanceFailureAcrossQueuedReads() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryConnectResistanceStatisticsRefreshService refreshService =
                Mockito.mock(BatteryConnectResistanceStatisticsRefreshService.class);
        ReflectionTestUtils.setField(
                ReflectionTestUtils.getField(service, "connectResistanceCommandProcessor"),
                "statisticsRefreshService",
                refreshService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 10);
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest firstRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                8,
                new byte[0],
                false);
        firstRequest.setOptLogId(10L);
        firstRequest.setBatteryGroup(2);
        firstRequest.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        firstRequest.setConnectResistanceNextAddress(9);
        firstRequest.setConnectResistanceMaxAddress(9);
        BatteryCollectorFrame shortFrame = new BatteryCollectorFrameCodec().buildRequest(8, 0x91,
                new byte[]{0x01, 0x02, 0x03});

        service.handleCompletedPendingResponse(state, shortFrame, firstRequest);

        Mockito.verifyNoInteractions(optLogMapper);
        BatteryModuleControlCommand nextCommand = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(nextCommand);
        Assertions.assertTrue(nextCommand.isConnectResistanceFailed());

        BatteryPendingRequest finalRequest = BatteryPendingRequest.fromProtocolCode(
                nextCommand.getProtocolCode(),
                nextCommand.getAddress(),
                nextCommand.getPayload(),
                false);
        finalRequest.setOptLogId(nextCommand.getOptLogId());
        finalRequest.setBatteryGroup(nextCommand.getBatteryGroup());
        finalRequest.setMode(nextCommand.getMode());
        finalRequest.setConnectResistanceNextAddress(nextCommand.getConnectResistanceNextAddress());
        finalRequest.setConnectResistanceMaxAddress(nextCommand.getConnectResistanceMaxAddress());
        finalRequest.setConnectResistanceFailed(nextCommand.isConnectResistanceFailed());
        BatteryCollectorFrame fullFrame = new BatteryCollectorFrameCodec().buildRequest(9, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame, finalRequest);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                Mockito.eq(1),
                Mockito.eq(0x91),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("0102030405060708"));
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getLastMode());
    }

    @Test
    void shouldUpdateOptLogOnlyWhenFinalConnectResistance91ResponseCompletes() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryConnectResistanceStatisticsRefreshService refreshService =
                Mockito.mock(BatteryConnectResistanceStatisticsRefreshService.class);
        ReflectionTestUtils.setField(
                ReflectionTestUtils.getField(service, "connectResistanceCommandProcessor"),
                "statisticsRefreshService",
                refreshService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 10);
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                10,
                new byte[0],
                false);
        pendingRequest.setOptLogId(10L);
        pendingRequest.setBatteryGroup(2);
        pendingRequest.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        pendingRequest.setConnectResistanceNextAddress(11);
        pendingRequest.setConnectResistanceMaxAddress(10);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(10, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.SUCCESS),
                Mockito.eq(0),
                Mockito.eq(0x91),
                Mockito.anyString(),
                Mockito.isNull(),
                Mockito.eq("0102030405060708"));
        Assertions.assertTrue(state.getQueuedModuleCommands().isEmpty());
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getLastMode());
        Mockito.verify(refreshService).refreshAfterCompletedTest(2);
    }

    @Test
    void shouldUpdateCommandOptLogWithResponsePayloadOnCompletedModuleCommand() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);
        pendingRequest.setOptLogId(10L);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x82,
                new byte[]{0x00, 0x02});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.SUCCESS),
                Mockito.eq(0),
                Mockito.eq(0x82),
                Mockito.anyString(),
                Mockito.isNull(),
                Mockito.eq("0002"));
    }

    @Test
    void shouldQueueNextGroupInternalResistanceCellAfterStatusResponse() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                1, new byte[0], false);
        pendingRequest.setOptLogId(100L);
        pendingRequest.setBatteryGroup(1);
        pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        pendingRequest.setGroupInternalResistanceNextAddress(2);
        pendingRequest.setGroupInternalResistanceMaxAddress(3);
        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(1, 0x82,
                new byte[]{0x00});

        service.handleCompletedPendingResponse(state, frame, pendingRequest);

        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand next = state.getQueuedModuleCommands().peek();
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, next.getProtocolCode());
        Assertions.assertEquals(2, next.getAddress());
        Assertions.assertEquals(3, next.getGroupInternalResistanceNextAddress());
        Assertions.assertEquals(3, next.getGroupInternalResistanceMaxAddress());
        Assertions.assertFalse(next.isGroupInternalResistanceFailed());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeStatusService.get(1).getMode());
    }

    @Test
    void shouldContinueGroupInternalResistanceAfterFailedCellAndFailFinalSummary() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest failedFirst = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                1, new byte[0], false);
        failedFirst.setOptLogId(110L);
        failedFirst.setBatteryGroup(1);
        failedFirst.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        failedFirst.setGroupInternalResistanceNextAddress(2);
        failedFirst.setGroupInternalResistanceMaxAddress(2);
        BatteryCollectorFrame failedFrame = new BatteryCollectorFrameCodec().buildRequest(1, 0x82,
                new byte[]{0x01});

        service.handleCompletedPendingResponse(state, failedFrame, failedFirst);

        BatteryModuleControlCommand next = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(next);
        Assertions.assertEquals(2, next.getAddress());
        Assertions.assertTrue(next.isGroupInternalResistanceFailed());
        BatteryPendingRequest finalRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                2, new byte[0], false);
        finalRequest.setOptLogId(120L);
        finalRequest.setBatteryGroup(1);
        finalRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        finalRequest.setGroupInternalResistanceNextAddress(3);
        finalRequest.setGroupInternalResistanceMaxAddress(2);
        finalRequest.setGroupInternalResistanceFailed(true);
        BatteryCollectorFrame successFrame = new BatteryCollectorFrameCodec().buildRequest(2, 0x82,
                new byte[]{0x00});

        service.handleCompletedPendingResponse(state, successFrame, finalRequest);

        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(1, modeInfo.getResult());
    }
    @Test
    void shouldHandleSingleIRStatusResponsePayloadsWithoutFakeRealtimeValues() {
        // SINGLEIR-002: _6 02/82 状态响应成功/失败/超时只更新命令日志和运行态，不写入伪造实时内阻值
        // 02/82 是状态应答命令，handleCompletedPendingResponse 只走 completeExplicitCommandResponse + markModeStopped，
        // 不调用 realtimeSnapshotService / realtimeConsumer，不伪造内阻数值。

        // --- 成功场景: payload[0]==0x00 表示模块应答成功 ---
        {
            OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
            injectCommandLogMapper(optLogMapper);
            BatteryModeStatusService modeStatusService = newModeStatusService();
            injectModeStatusService(modeStatusService);
            modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
            BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
            BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                    BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                    8, new byte[0], false);
            pendingRequest.setOptLogId(10L);
            pendingRequest.setBatteryGroup(1);
            pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
            BatteryCollectorFrame successFrame = new BatteryCollectorFrameCodec().buildRequest(8, 0x82,
                    new byte[]{0x00, 0x02});

            service.handleCompletedPendingResponse(state, successFrame, pendingRequest);

            // 命令日志更新为 SUCCESS
            Mockito.verify(optLogMapper).updateCommandStatus(
                    Mockito.eq(10L),
                    Mockito.eq(BatteryDeviceStateConstants.CommandStatus.SUCCESS),
                    Mockito.eq(0),
                    Mockito.eq(0x82),
                    Mockito.anyString(),
                    Mockito.isNull(),
                    Mockito.eq("0002"));
            // 模式已停止
            Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeStatusService.get(1).getMode());
        }

        // --- 失败场景: payload[0]==0x01 表示模块应答失败 ---
        {
            OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
            injectCommandLogMapper(optLogMapper);
            BatteryModeStatusService modeStatusService = newModeStatusService();
            injectModeStatusService(modeStatusService);
            modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
            BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
            BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                    BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                    8, new byte[0], false);
            pendingRequest.setOptLogId(20L);
            pendingRequest.setBatteryGroup(1);
            pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
            BatteryCollectorFrame failFrame = new BatteryCollectorFrameCodec().buildRequest(8, 0x82,
                    new byte[]{0x01, 0x03});

            service.handleCompletedPendingResponse(state, failFrame, pendingRequest);

            // 命令日志更新为 FAILED
            Mockito.verify(optLogMapper).updateCommandStatus(
                    Mockito.eq(20L),
                    Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                    Mockito.eq(1),
                    Mockito.eq(0x82),
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.eq("0103"));
            // 模式已停止（失败也停止）
            Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeStatusService.get(1).getMode());
        }

        // --- 超时场景: 命令响应超时 ---
        {
            OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
            injectCommandLogMapper(optLogMapper);
            BatteryModeStatusService modeStatusService = newModeStatusService();
            injectModeStatusService(modeStatusService);
            modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
            BatteryCollectorCommandQueueService commandQueueService =
                    newCommandQueueService(modeStatusService, commandLogService);
            BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
            BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                    BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                    8, new byte[0], false);
            pendingRequest.setOptLogId(30L);
            pendingRequest.setBatteryGroup(1);
            pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);

            commandQueueService.completeTimedOutExplicitCommand(state, pendingRequest);

            // 命令日志更新为 TIMEOUT
            Mockito.verify(optLogMapper).updateCommandStatus(
                    Mockito.eq(30L),
                    Mockito.eq(BatteryDeviceStateConstants.CommandStatus.TIMEOUT),
                    Mockito.eq(1),
                    Mockito.isNull(),
                    Mockito.anyString(),
                    Mockito.eq("命令响应超时"),
                    Mockito.isNull());
        }
    }

    @Test
    void shouldRecordTimedOutModuleCommandAsFailed() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), commandLogService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);
        pendingRequest.setOptLogId(10L);

        commandQueueService.completeTimedOutExplicitCommand(state, pendingRequest);

        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x82, state.getLastCompletedModuleResponseCode());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
        Assertions.assertTrue(state.getLastCompletedModuleCommandTime() > 0);
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.TIMEOUT),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.eq("命令响应超时"),
                Mockito.isNull());
    }

    @Test
    void shouldCompletePendingExplicitCommandBeforeClose() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        ReflectionTestUtils.setField(service, "frameIoService", Mockito.mock(BatteryCollectorFrameIoService.class));
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);
        pendingRequest.setBatteryGroup(1);
        pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        pendingRequest.setOptLogId(10L);
        state.setPendingCommand(pendingRequest);
        state.setExpectedResponseCode(0x82);
        state.setCurrentRetryCount(1);
        state.setRunState(BatteryCollectorRunState.WAIT_COMMAND_RESPONSE);

        ReflectionTestUtils.invokeMethod(service, "closeQuietly", state);

        Assertions.assertNull(state.getPendingCommand());
        Assertions.assertEquals(0, state.getExpectedResponseCode());
        Assertions.assertEquals(0, state.getCurrentRetryCount());
        Assertions.assertEquals(BatteryCollectorRunState.READ, state.getRunState());
        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", state.getLastCompletedModuleCommandName());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                Mockito.eq(1),
                Mockito.eq(0x82),
                Mockito.anyString(),
                Mockito.eq("采集通道关闭，命令未完成"),
                Mockito.isNull());
    }

    @Test
    void shouldNotUpdateCommandLogWhenAutoPollPendingOnClose() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        ReflectionTestUtils.setField(service, "frameIoService", Mockito.mock(BatteryCollectorFrameIoService.class));
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        BatteryPendingRequest autoPollPending = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                true);
        autoPollPending.setOptLogId(20L);
        state.setPendingCommand(autoPollPending);
        state.setExpectedResponseCode(0x82);
        state.setCurrentRetryCount(1);
        state.setRunState(BatteryCollectorRunState.WAIT_COMMAND_RESPONSE);

        ReflectionTestUtils.invokeMethod(service, "closeQuietly", state);

        Assertions.assertNull(state.getPendingCommand());
        Assertions.assertEquals(0, state.getExpectedResponseCode());
        Assertions.assertEquals(0, state.getCurrentRetryCount());
        Assertions.assertEquals(BatteryCollectorRunState.READ, state.getRunState());
        Mockito.verify(optLogMapper, Mockito.never()).updateCommandStatus(
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldCancelQueuedCommandsOnClose() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        ReflectionTestUtils.setField(service, "frameIoService", Mockito.mock(BatteryCollectorFrameIoService.class));
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);

        BatteryModuleControlCommand cmd1 = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(1)
                .requestCode(0x0F)
                .payload(new byte[0])
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE)
                .optLogId(30L)
                .build();
        BatteryModuleControlCommand cmd2 = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(2)
                .requestCode(0x0F)
                .payload(new byte[0])
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE)
                .optLogId(31L)
                .build();
        state.getQueuedModuleCommands().add(cmd1);
        state.getQueuedModuleCommands().add(cmd2);

        ReflectionTestUtils.invokeMethod(service, "closeQuietly", state);

        Assertions.assertTrue(state.getQueuedModuleCommands().isEmpty());
        Mockito.verify(optLogMapper, Mockito.times(2)).updateCommandStatus(
                Mockito.anyLong(),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.CANCELLED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.eq("采集通道关闭，命令未下发"),
                Mockito.isNull());
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(30L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.CANCELLED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.eq("采集通道关闭，命令未下发"),
                Mockito.isNull());
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(31L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.CANCELLED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.eq("采集通道关闭，命令未下发"),
                Mockito.isNull());
    }

    @Test
    void shouldResetModeStatusOnCloseWithPendingAndQueuedCommands() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        ReflectionTestUtils.setField(service, "frameIoService", Mockito.mock(BatteryCollectorFrameIoService.class));
        BatteryModeStatusService modeStatusService = (BatteryModeStatusService)
                ReflectionTestUtils.getField(
                        ReflectionTestUtils.getField(service, "commandQueueService"),
                        "batteryModeStatusService");
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);

        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);
        pendingRequest.setBatteryGroup(1);
        pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        pendingRequest.setOptLogId(40L);
        state.setPendingCommand(pendingRequest);

        BatteryModuleControlCommand queuedCmd = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(9)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .optLogId(41L)
                .build();
        state.getQueuedModuleCommands().add(queuedCmd);

        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 40L);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeStatusService.get(1).getMode());

        ReflectionTestUtils.invokeMethod(service, "closeQuietly", state);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeStatusService.get(1).getMode());
        Assertions.assertEquals(0, modeStatusService.get(1).getStatus());
        Assertions.assertTrue(state.getQueuedModuleCommands().isEmpty());
        Assertions.assertNull(state.getPendingCommand());
    }

    @Test
    void shouldSkipPollingImmediatelyAfterAnySend() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setLastSendTime(System.currentTimeMillis());
        state.setLastPollTime(0L);

        ReflectionTestUtils.invokeMethod(service, "pollIfNecessary", state);

        Assertions.assertEquals(0L, state.getPollRoundCount());
        Assertions.assertEquals(0L, state.getLastPollTime());
    }

    @Test
    void shouldNotUpdateCommandLogAfter0FSendAndQueueFirst1191Read() {
        // CONNRES-002: 0F 无响应发送成功后不会直接完成命令日志，首个 11/91 被排队
        RecordingBatteryCollectorService recordingService = new RecordingBatteryCollectorService();
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        BatteryCollectorCommandLogService testCommandLogService = new BatteryCollectorCommandLogService();
        ReflectionTestUtils.setField(testCommandLogService, "optLogMapper", optLogMapper);
        ReflectionTestUtils.setField(recordingService, "commandLogService", testCommandLogService);
        BatteryCollectorCommandQueueService commandQueueService =
                newCommandQueueService(newModeStatusService(), testCommandLogService);
        ReflectionTestUtils.setField(recordingService, "commandQueueService", commandQueueService);
        injectConnectResistanceProcessor(recordingService, commandQueueService, null, null);
        ReflectionTestUtils.setField(recordingService, "properties", new BatteryCollectorProperties());
        ReflectionTestUtils.setField(recordingService, "protocolLogService", new BatteryCollectorProtocolLogService());
        ReflectionTestUtils.setField(recordingService, "frameCodec", new BatteryCollectorFrameCodec());
        ReflectionTestUtils.setField(recordingService, "frameIoService",
                Mockito.mock(BatteryCollectorFrameIoService.class));

        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setSerialPort(SerialPort.getCommPort("battery-test"));

        BatteryModuleControlCommand cmd0F = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .address(8)
                .requestCode(0x0F)
                .payload(new byte[0])
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE)
                .optLogId(10L)
                .configId(1L)
                .connectResistanceNextAddress(1)
                .connectResistanceMaxAddress(8)
                .build();

        BatteryCollectorFrame frame = new BatteryCollectorFrameCodec().buildRequest(8, 0x0F, new byte[0]);

        // Mode should NOT be stopped (mode=10 CONNECT_RESISTANCE skips mode stop)
        // First mark running, then verify 0F doesn't stop it
        BatteryModeStatusService modeStatusService = (BatteryModeStatusService)
                ReflectionTestUtils.getField(commandQueueService, "batteryModeStatusService");
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 8, 10L);
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeStatusService.get(1).getMode());

        ReflectionTestUtils.invokeMethod(recordingService, "writeFrameWithoutPending", state, frame, cmd0F);

        // Mode should still be CONNECT_RESISTANCE after 0F (not stopped)
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeStatusService.get(1).getMode());

        // 0F should NOT update command log (connect resistance defers log update to final 91)
        Mockito.verify(optLogMapper, Mockito.never()).updateCommandStatus(
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyInt(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // First 11/91 command should be queued
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand queuedCmd = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(queuedCmd);
        Assertions.assertEquals(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                queuedCmd.getProtocolCode());
        Assertions.assertEquals(1, queuedCmd.getAddress());
        Assertions.assertEquals(0x11, queuedCmd.getRequestCode());
        Assertions.assertEquals(0x91, queuedCmd.getResponseCode());
        Assertions.assertEquals(10L, queuedCmd.getOptLogId());
        Assertions.assertEquals(1, queuedCmd.getBatteryGroup());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, queuedCmd.getMode());
        Assertions.assertEquals(Integer.valueOf(2), queuedCmd.getConnectResistanceNextAddress());
        Assertions.assertEquals(Integer.valueOf(8), queuedCmd.getConnectResistanceMaxAddress());
    }

    @Test
    void shouldContinueConnectResistanceAfterMidAddressFailureAndFinallyFail() {
        // CONNRES-003: 中间地址 11/91 响应 payload 不足导致失败，失败标记传播，最终命令日志为 FAILED 且模式关闭
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryConnectResistanceStatisticsRefreshService refreshService =
                Mockito.mock(BatteryConnectResistanceStatisticsRefreshService.class);
        ReflectionTestUtils.setField(
                ReflectionTestUtils.getField(service, "connectResistanceCommandProcessor"),
                "statisticsRefreshService",
                refreshService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 3);
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);

        // --- 地址 1: 成功响应 (8字节有效载荷) ---
        BatteryPendingRequest request1 = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                1, new byte[0], false);
        request1.setOptLogId(10L);
        request1.setBatteryGroup(2);
        request1.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        request1.setConnectResistanceNextAddress(2);
        request1.setConnectResistanceMaxAddress(3);
        BatteryCollectorFrame fullFrame1 = new BatteryCollectorFrameCodec().buildRequest(1, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame1, request1);

        // 地址 1 成功，不应更新命令日志（中间步骤不更新）
        Mockito.verifyNoInteractions(optLogMapper);
        // 地址 2 应被排队
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand cmd2 = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(cmd2);
        Assertions.assertEquals(2, cmd2.getAddress());
        Assertions.assertFalse(cmd2.isConnectResistanceFailed());

        // --- 地址 2: 失败响应 (payload 仅 3 字节, 不足 8 字节) ---
        BatteryPendingRequest request2 = BatteryPendingRequest.fromProtocolCode(
                cmd2.getProtocolCode(), cmd2.getAddress(), cmd2.getPayload(), false);
        request2.setOptLogId(cmd2.getOptLogId());
        request2.setBatteryGroup(cmd2.getBatteryGroup());
        request2.setMode(cmd2.getMode());
        request2.setConnectResistanceNextAddress(cmd2.getConnectResistanceNextAddress());
        request2.setConnectResistanceMaxAddress(cmd2.getConnectResistanceMaxAddress());
        request2.setConnectResistanceFailed(cmd2.isConnectResistanceFailed());
        BatteryCollectorFrame shortFrame2 = new BatteryCollectorFrameCodec().buildRequest(2, 0x91,
                new byte[]{0x01, 0x02, 0x03});

        service.handleCompletedPendingResponse(state, shortFrame2, request2);

        // 中间步骤仍不应直接更新命令日志
        Mockito.verifyNoInteractions(optLogMapper);
        // 失败后仍应继续排队地址 3
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        BatteryModuleControlCommand cmd3 = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(cmd3);
        Assertions.assertEquals(3, cmd3.getAddress());
        // 失败标记应被传播到下一条排队命令
        Assertions.assertTrue(cmd3.isConnectResistanceFailed());

        // --- 地址 3: 成功载荷, 但因中间失败最终状态为 FAILED ---
        BatteryPendingRequest request3 = BatteryPendingRequest.fromProtocolCode(
                cmd3.getProtocolCode(), cmd3.getAddress(), cmd3.getPayload(), false);
        request3.setOptLogId(cmd3.getOptLogId());
        request3.setBatteryGroup(cmd3.getBatteryGroup());
        request3.setMode(cmd3.getMode());
        request3.setConnectResistanceNextAddress(cmd3.getConnectResistanceNextAddress());
        request3.setConnectResistanceMaxAddress(cmd3.getConnectResistanceMaxAddress());
        request3.setConnectResistanceFailed(cmd3.isConnectResistanceFailed());
        BatteryCollectorFrame fullFrame3 = new BatteryCollectorFrameCodec().buildRequest(3, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame3, request3);

        // 最终命令日志应为 FAILED（中间失败传播）
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                Mockito.eq(1),
                Mockito.eq(0x91),
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.eq("0102030405060708"));
        // 模式应已停止
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getLastMode());
    }

    @Test
    void shouldOnlyUpdateCommandLogAndStopModeOnFinalConnectResistanceSuccess() {
        // CONNRES-004: 全部成功的 3 地址流程，中间步骤不触发日志更新和模式关闭，仅最终 11/91 统一执行
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        injectCommandLogMapper(optLogMapper);
        BatteryModeStatusService modeStatusService = newModeStatusService();
        injectModeStatusService(modeStatusService);
        BatteryConnectResistanceStatisticsRefreshService refreshService =
                Mockito.mock(BatteryConnectResistanceStatisticsRefreshService.class);
        ReflectionTestUtils.setField(
                ReflectionTestUtils.getField(service, "connectResistanceCommandProcessor"),
                "statisticsRefreshService",
                refreshService);
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 3);
        BatteryCollectorChannelConfig channelConfig = newChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);

        // --- 地址 1: 成功响应 ---
        BatteryPendingRequest request1 = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE,
                1, new byte[0], false);
        request1.setOptLogId(10L);
        request1.setBatteryGroup(2);
        request1.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        request1.setConnectResistanceNextAddress(2);
        request1.setConnectResistanceMaxAddress(3);
        BatteryCollectorFrame fullFrame1 = new BatteryCollectorFrameCodec().buildRequest(1, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame1, request1);

        // 中间步骤不更新命令日志
        Mockito.verifyNoInteractions(optLogMapper);
        Mockito.verifyNoInteractions(refreshService);
        // 模式仍为 CONNECT_RESISTANCE（未被关闭）
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE,
                modeStatusService.get(2).getMode());
        // 地址 2 被排队
        BatteryModuleControlCommand cmd2 = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(cmd2);
        Assertions.assertEquals(2, cmd2.getAddress());

        // --- 地址 2: 成功响应（仍为中间步骤） ---
        BatteryPendingRequest request2 = BatteryPendingRequest.fromProtocolCode(
                cmd2.getProtocolCode(), cmd2.getAddress(), cmd2.getPayload(), false);
        request2.setOptLogId(cmd2.getOptLogId());
        request2.setBatteryGroup(cmd2.getBatteryGroup());
        request2.setMode(cmd2.getMode());
        request2.setConnectResistanceNextAddress(cmd2.getConnectResistanceNextAddress());
        request2.setConnectResistanceMaxAddress(cmd2.getConnectResistanceMaxAddress());
        request2.setConnectResistanceFailed(cmd2.isConnectResistanceFailed());
        BatteryCollectorFrame fullFrame2 = new BatteryCollectorFrameCodec().buildRequest(2, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame2, request2);

        // 中间步骤仍不更新命令日志
        Mockito.verifyNoInteractions(optLogMapper);
        Mockito.verifyNoInteractions(refreshService);
        // 模式仍未被关闭
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE,
                modeStatusService.get(2).getMode());
        // 地址 3 被排队
        BatteryModuleControlCommand cmd3 = state.getQueuedModuleCommands().poll();
        Assertions.assertNotNull(cmd3);
        Assertions.assertEquals(3, cmd3.getAddress());

        // --- 地址 3: 最终成功响应 ---
        BatteryPendingRequest request3 = BatteryPendingRequest.fromProtocolCode(
                cmd3.getProtocolCode(), cmd3.getAddress(), cmd3.getPayload(), false);
        request3.setOptLogId(cmd3.getOptLogId());
        request3.setBatteryGroup(cmd3.getBatteryGroup());
        request3.setMode(cmd3.getMode());
        request3.setConnectResistanceNextAddress(cmd3.getConnectResistanceNextAddress());
        request3.setConnectResistanceMaxAddress(cmd3.getConnectResistanceMaxAddress());
        request3.setConnectResistanceFailed(cmd3.isConnectResistanceFailed());
        BatteryCollectorFrame fullFrame3 = new BatteryCollectorFrameCodec().buildRequest(3, 0x91,
                new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08});

        service.handleCompletedPendingResponse(state, fullFrame3, request3);

        // 最终步骤统一更新命令日志为 SUCCESS
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.SUCCESS),
                Mockito.eq(0),
                Mockito.eq(0x91),
                Mockito.anyString(),
                Mockito.isNull(),
                Mockito.eq("0102030405060708"));
        // 模式已停止
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getLastMode());
        Mockito.verify(refreshService).refreshAfterCompletedTest(2);
    }

    private BatteryCollectorChannelConfig newChannelConfig() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setBatteryGroup(1);
        channelConfig.setConfigId(1L);
        return channelConfig;
    }
}
