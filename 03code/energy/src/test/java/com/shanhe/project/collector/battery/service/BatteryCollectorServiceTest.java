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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

class BatteryCollectorServiceTest {

    private final BatteryCollectorService service = new BatteryCollectorService();

    @Test
    void shouldUseSafeDefaultsForInvalidTimingConfig() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setLoopDelayMs(0L);
        properties.setRequestGapMs(null);
        properties.setModuleAddressMissThreshold(0);
        ReflectionTestUtils.setField(service, "properties", properties);

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
    @SuppressWarnings("unchecked")
    void shouldQueueModuleCommandForActiveChannel() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(service, "channelStates");
        channelStates.add(state);

        boolean queued = service.submitModuleCommand("battery-group-1", BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build());

        Assertions.assertTrue(queued);
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
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
