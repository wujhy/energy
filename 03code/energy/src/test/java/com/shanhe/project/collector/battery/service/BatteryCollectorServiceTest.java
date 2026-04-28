package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
}
