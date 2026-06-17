package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

class BatteryCollectorDeviceStateServiceTest {

    private final BatteryCollectorDeviceStateService service = new BatteryCollectorDeviceStateService();

    @Test
    void shouldDeduplicateSerialPortStateAndPersistRecoveryBoundary() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig("battery-group-1", 1));

        service.persistSerialPortState(state, true);
        service.persistSerialPortState(state, true);
        service.persistSerialPortState(state, false);
        service.persistSerialPortState(state, false);
        service.persistSerialPortState(state, true);

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
        injectStateService(stateService);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig("battery-group-1", 1));

        service.persistChannelError(state, new IllegalStateException("open failed"));
        service.persistChannelError(state, new IllegalStateException("open failed"));
        service.persistSerialPortState(state, true);
        service.persistSerialPortState(state, true);

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
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        injectStateService(stateService);
        BatteryCollectorChannelConfig config = channelConfig("battery-group-1", 1);
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(config);
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.MODULE_INFO,
                8,
                new byte[0],
                true);

        service.persistModuleTimeout(state, pendingRequest);
        service.persistModuleTimeout(state, pendingRequest);
        service.persistModuleActive(config.getName(), config, 8, true);
        service.clearModuleTimeout(config.getName(), config, 8);
        service.clearModuleTimeout(config.getName(), config, 8);

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
    void shouldClearDedupCacheForSelectedBatteryGroupOnly() {
        Map<String, String> lastStateValues = service.getLastStateValues();
        lastStateValues.put("1:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-1:CHANNEL_OPEN", "open");
        lastStateValues.put("battery-group-1:8:MODULE_ACTIVE", "active");
        lastStateValues.put("2:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-2:CHANNEL_OPEN", "open");

        int removed = service.clearDedupCacheByBatteryGroup(Arrays.asList(
                new BatteryCollectorChannelState(channelConfig("battery-group-1", 1)),
                new BatteryCollectorChannelState(channelConfig("battery-group-2", 2))), 1);

        Assertions.assertEquals(3, removed);
        Assertions.assertFalse(lastStateValues.containsKey("1:GROUP_246_FRESHNESS"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:CHANNEL_OPEN"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:8:MODULE_ACTIVE"));
        Assertions.assertTrue(lastStateValues.containsKey("2:GROUP_246_FRESHNESS"));
        Assertions.assertTrue(lastStateValues.containsKey("battery-group-2:CHANNEL_OPEN"));
    }

    @Test
    void shouldOnlyTreatModuleAndPackStateCacheKeysAsHighCardinality() {
        Assertions.assertFalse(service.isHighCardinalityStateCacheKey("battery-group-1:CHANNEL_OPEN"));
        Assertions.assertFalse(service.isHighCardinalityStateCacheKey("battery-group-1:CHANNEL_ERROR"));
        Assertions.assertTrue(service.isHighCardinalityStateCacheKey("battery-group-1:8:MODULE_TIMEOUT"));
        Assertions.assertTrue(service.isHighCardinalityStateCacheKey("battery-group-1:8:MODULE_ACTIVE"));
        Assertions.assertTrue(service.isHighCardinalityStateCacheKey("1:GROUP_246_FRESHNESS"));
    }

    private void injectStateService(BatteryDeviceStateService stateService) {
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", stateService);
    }

    /** 构造状态持久化测试使用的通道配置。 */
    private BatteryCollectorChannelConfig channelConfig(String name, Integer batteryGroup) {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName(name);
        config.setBatteryGroup(batteryGroup);
        return config;
    }
}
