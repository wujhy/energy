package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BatteryCollectorServiceTest {

    private final BatteryCollectorService service = new BatteryCollectorService();

    private BatteryModeStatusService newModeStatusService() {
        BatteryModeStatusService modeStatusService = new BatteryModeStatusService();
        ReflectionTestUtils.setField(modeStatusService, "cacheAccessor", new TestCacheAccessor());
        return modeStatusService;
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

    @Test
    void shouldUseSafeDefaultsForInvalidTimingConfig() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setLoopDelayMs(0L);
        properties.setRequestGapMs(null);
        properties.setModuleAddressMissThreshold(0);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", newModeStatusService());

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
        Assertions.assertEquals(9600, service.resolveBaudRate(channelConfig));
        Assertions.assertEquals(8, service.resolveDataBits(channelConfig));
        Assertions.assertEquals(1, service.resolveStopBits(channelConfig));
        Assertions.assertEquals(0, service.resolveParity(channelConfig));
        Assertions.assertEquals(1000, service.resolvePortTimeoutMs(channelConfig));
        Assertions.assertEquals(1, service.resolveModuleAddressStart(channelConfig));
        Assertions.assertEquals(246, service.resolveModuleAddressEnd(channelConfig));
        Assertions.assertEquals(3000L, service.resolvePollIntervalMs(channelConfig));
        Assertions.assertEquals(2048, service.resolveReadBufferSize(channelConfig));
        Assertions.assertEquals(64, service.resolveReceiveBufferLimit(channelConfig));
        Assertions.assertEquals(1500L, service.resolveResponseTimeoutMs(channelConfig));
        Assertions.assertEquals(2, service.resolveMaxRetryCount(channelConfig));
        Assertions.assertEquals(3, service.resolveModuleAddressMissThreshold());
    }

    @Test
    void shouldAllowZeroRetryCount() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setMaxRetryCount(0);

        Assertions.assertEquals(0, service.resolveMaxRetryCount(channelConfig));
    }

    @Test
    void shouldResolveExpectedCellCountFromChannelConfig() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setExpectedCellCount(600);

        Assertions.assertEquals(245, service.resolveExpectedCellCount(channelConfig));
    }

    @Test
    void shouldBuildChannelSnapshot() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setPortName("ttyS9");
        channelConfig.setBatteryGroup(1);
        channelConfig.setDeviceAddress(1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.setRunState(BatteryCollectorRunState.WAIT_RESPONSE);
        state.setLastSendTime(100L);
        state.setLastReceiveTime(200L);
        state.setTimeoutCount(3);
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
    @SuppressWarnings("unchecked")
    void shouldResetModuleAddressCacheForSelectedChannel() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
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
    }

    @Test
    void shouldRequireFullDiscoveryWhenOnlyGroupModuleAddressIsCached() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(246);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(246);

        Assertions.assertFalse(service.hasActiveCellModuleAddress(state));
    }

    @Test
    void shouldDetectActiveCellModuleAddress() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(246);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);
        state.getActiveModuleAddresses().add(246);

        Assertions.assertTrue(service.hasActiveCellModuleAddress(state));
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
    void shouldRemoveCachedModuleAddressAfterConsecutiveMisses() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        properties.setModuleAddressMissThreshold(2);
        ReflectionTestUtils.setField(service, "properties", properties);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);

        ReflectionTestUtils.invokeMethod(service, "updateModuleAddressCache", state, 8, false);
        Assertions.assertTrue(state.getActiveModuleAddresses().contains(8));

        ReflectionTestUtils.invokeMethod(service, "updateModuleAddressCache", state, 8, false);

        Assertions.assertFalse(state.getActiveModuleAddresses().contains(8));
        Assertions.assertFalse(state.getModuleAddressMissCounts().containsKey(8));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAlwaysPollGroupModuleAddressEvenWhenCellRangeEndsAt245() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setModuleAddressCacheEnabled(true);
        ReflectionTestUtils.setField(service, "properties", properties);

        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setModuleAddressStart(1);
        channelConfig.setModuleAddressEnd(245);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        state.getActiveModuleAddresses().add(8);

        List<Integer> fullDiscoveryAddresses = ReflectionTestUtils.invokeMethod(service,
                "resolvePollingAddresses", state, true);
        List<Integer> cachedAddresses = ReflectionTestUtils.invokeMethod(service,
                "resolvePollingAddresses", state, false);

        Assertions.assertNotNull(fullDiscoveryAddresses);
        Assertions.assertTrue(fullDiscoveryAddresses.contains(246));
        Assertions.assertEquals(246, fullDiscoveryAddresses.get(fullDiscoveryAddresses.size() - 1));
        Assertions.assertNotNull(cachedAddresses);
        Assertions.assertEquals(Arrays.asList(8, 246), cachedAddresses);
    }

    @Test
    void shouldSkipRemainingCellDiscoveryAfterExpectedCellResponsesButKeepGroupModule() {
        Assertions.assertFalse(service.shouldSkipRemainingCellDiscovery(true, 8, 23, 24));
        Assertions.assertTrue(service.shouldSkipRemainingCellDiscovery(true, 24, 24, 24));
        Assertions.assertFalse(service.shouldSkipRemainingCellDiscovery(true, 246, 24, 24));
        Assertions.assertFalse(service.shouldSkipRemainingCellDiscovery(false, 24, 24, 24));
        Assertions.assertFalse(service.shouldSkipRemainingCellDiscovery(true, 24, 24, 0));
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
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

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

        ReflectionTestUtils.invokeMethod(service, "processQueuedModuleCommand", state);

        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
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
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
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
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
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
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
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
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
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

        ReflectionTestUtils.invokeMethod(service, "markModeStopped", stopGroup, true);
        BatteryModeInfo modeInfo = modeStatusService.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM, modeInfo.getLastMode());
        Assertions.assertEquals(2, modeInfo.getLastAddress());
    }

    @Test
    void shouldKeepConnectResistanceModeRunningAfterStartFrameWritten() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
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

        Boolean shouldStop = ReflectionTestUtils.invokeMethod(service, "shouldStopModeAfterNoResponseCommand", command);
        Assertions.assertFalse(shouldStop);
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
    void shouldRecordTimedOutModuleCommandAsFailed() {
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(new BatteryCollectorChannelConfig());
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8,
                new byte[0],
                false);

        service.handleTimedOutPendingRequest(state, pendingRequest);

        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", state.getLastCompletedModuleCommandName());
        Assertions.assertEquals(0x82, state.getLastCompletedModuleResponseCode());
        Assertions.assertFalse(state.isLastCompletedModuleCommandSuccess());
        Assertions.assertTrue(state.getLastCompletedModuleCommandTime() > 0);
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
}
