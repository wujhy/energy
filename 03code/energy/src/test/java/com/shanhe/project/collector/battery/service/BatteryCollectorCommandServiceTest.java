package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BatteryCollectorCommandServiceTest {

    private final BatteryCollectorCommandService service = new BatteryCollectorCommandService();

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
    void shouldBlockAggregateCommandOnModuleChannel() {
        BatteryCollectorCommandResult result = service.execute(
                BatteryAggregateCommandDefinition.GET_BATTERY_GROUP_INFO,
                "battery-rs485-1",
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.isTimeout());
        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertEquals("battery-rs485-1", result.getChannelName());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.GET_BATTERY_GROUP_INFO, result.getCommandDefinition());
        Assertions.assertTrue(result.getMessage().contains("cannot be sent directly"));
    }

    @Test
    void shouldMapSingleResistanceTestToModuleCommand() {
        BatteryCollectorCommandResult result = service.singleInternalResistanceTest(
                "battery-rs485-1",
                1,
                8,
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertNotNull(result.getModuleControlCommand());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(8, result.getModuleControlCommand().getAddress());
    }

    @Test
    void shouldMapConnectResistanceTestToBroadcastModuleCommand() {
        BatteryCollectorCommandResult result = service.connectResistanceTest("battery-rs485-1", 1, 1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
    }

    @Test
    void shouldMapClearBatteryGroupDebugDataToBroadcastModuleCommand() {
        BatteryCollectorCommandResult result = service.clearBatteryGroupDebugData("battery-rs485-1", 1, 1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.CLEAR_INDIVIDUAL_DEBUGGING_DATA,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertEquals(1, result.getModuleControlCommand().getBatteryGroup());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0x0F}, result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldMapAutomaticModuleAddressFromBatteryPackConfig() {
        BatteryCollectorCommandResult result = service.autoSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                24,
                2,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(246, result.getModuleControlCommand().getAddress());
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM,
                result.getModuleControlCommand().getMode());
        Assertions.assertEquals(24, result.getModuleControlCommand().getAutoAddressBatteryCount());
        Assertions.assertEquals(2, result.getModuleControlCommand().getAutoAddressBatterySpecification());
        Assertions.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 1},
                result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldRejectAutomaticModuleAddressWhenSpecificationUnsupported() {
        BatteryCollectorCommandResult result = service.autoSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                24,
                7,
                1000L);

        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void shouldMapManualModuleAddressToModuleCommand() {
        BatteryCollectorCommandResult result = service.manualSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                8,
                9,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.SET_SUBMODULE_ID, result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(8, result.getModuleControlCommand().getAddress());
        Assertions.assertArrayEquals(new byte[]{9}, result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldMapInternalResistanceCoefficientWithM460FloatPayload() {
        BatteryCollectorCommandResult result = service.setInternalResistanceCoefficient(
                "battery-rs485-1",
                1,
                0,
                1000,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0x00, 0x00, (byte) 0x80, 0x3F},
                result.getModuleControlCommand().getPayload());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueueMappedModuleCommandWhenCollectorChannelExists() {
        BatteryCollectorService collectorService = new BatteryCollectorService();
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
        ReflectionTestUtils.setField(collectorService, "batteryModeStatusService", newModeStatusService());
        ReflectionTestUtils.setField(service, "collectorService", collectorService);

        BatteryCollectorCommandResult result = service.singleInternalResistanceTest(
                "battery-rs485-1",
                1,
                8,
                1000L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                state.getQueuedModuleCommands().peek().getProtocolCode());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getRequestCode(),
                result.getRequestCode());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getResponseCode(),
                result.getResponseCode());
        Assertions.assertTrue(result.getMessage().contains("queued"));
    }

    @Test
    void shouldKeepAmbiguousSubmoduleIdUnsupported() {
        BatteryCollectorCommandResult result = service.execute(
                BatteryAggregateCommandDefinition.SET_DEVICE_ID,
                "battery-rs485-1",
                1000L,
                8);

        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("cannot be sent directly"));
    }

    @Test
    void shouldKeepAggregateCommandsWithInsufficientContextUnsupported() {
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.SET_SYSTEM_STATE,
                "battery-rs485-1",
                1000L,
                1, 1).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.CLEAR_HOST_DEBUGGING_DATA,
                "battery-rs485-1",
                1000L).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT,
                "battery-rs485-1",
                1000L,
                1, 1, 0x03, 0xE8).isMappedToModuleCommand());
    }

    @Test
    void shouldMapExplicitAggregateCommandOnlyWhenPayloadIsComplete() {
        BatteryCollectorCommandResult incomplete = service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1, 2, 3);
        BatteryCollectorCommandResult complete = service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1, 2, 3, 4, 5, 6, 7);

        Assertions.assertFalse(incomplete.isMappedToModuleCommand());
        Assertions.assertTrue(complete.isMappedToModuleCommand());
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                complete.getModuleControlCommand().getProtocolCode());
        Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7},
                complete.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldResolveChannelNameByBatteryGroup() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig first = new BatteryCollectorChannelConfig();
        first.setName("battery-rs485-1");
        first.setConfigId(10L);
        first.setBatteryGroup(1);
        BatteryCollectorChannelConfig second = new BatteryCollectorChannelConfig();
        second.setName("battery-rs485-2");
        second.setConfigId(20L);
        second.setBatteryGroup(2);
        properties.getChannels().add(first);
        properties.getChannels().add(second);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(2);

        Assertions.assertEquals("battery-rs485-2", channelName);
    }

    @Test
    void shouldKeepLegacyResolveChannelNameSignatureCompatible() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig channel = new BatteryCollectorChannelConfig();
        channel.setName("battery-rs485-1");
        channel.setConfigId(10L);
        channel.setBatteryGroup(1);
        properties.getChannels().add(channel);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(99L, 1);

        Assertions.assertEquals("battery-rs485-1", channelName);
    }

    @Test
    void shouldResolveBlankConfigIdChannelWhenConfigIdIsSpecified() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig channel = new BatteryCollectorChannelConfig();
        channel.setName("battery-rs485-1");
        channel.setBatteryGroup(1);
        properties.getChannels().add(channel);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(10L, 1);

        Assertions.assertEquals("battery-rs485-1", channelName);
    }

    @Test
    void shouldResolveByBatteryGroupOnlyWhenConfigIdIsBlank() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig channel = new BatteryCollectorChannelConfig();
        channel.setName("battery-rs485-1");
        channel.setBatteryGroup(1);
        properties.getChannels().add(channel);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(null, 1);

        Assertions.assertEquals("battery-rs485-1", channelName);
    }

    @Test
    void shouldNotResolveByBatteryGroupWhenMultipleChannelsMatch() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig first = new BatteryCollectorChannelConfig();
        first.setName("battery-rs485-1");
        first.setBatteryGroup(1);
        BatteryCollectorChannelConfig second = new BatteryCollectorChannelConfig();
        second.setName("battery-rs485-2");
        second.setBatteryGroup(1);
        properties.getChannels().add(first);
        properties.getChannels().add(second);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(null, 1);

        Assertions.assertNull(channelName);
    }
}
