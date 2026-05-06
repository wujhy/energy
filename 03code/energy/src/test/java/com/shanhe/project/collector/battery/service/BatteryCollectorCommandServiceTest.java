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

import java.util.List;

class BatteryCollectorCommandServiceTest {

    private final BatteryCollectorCommandService service = new BatteryCollectorCommandService();

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
    @SuppressWarnings("unchecked")
    void shouldQueueMappedModuleCommandWhenCollectorChannelExists() {
        BatteryCollectorService collectorService = new BatteryCollectorService();
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
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
        BatteryCollectorCommandResult result = service.setSubmoduleId("battery-rs485-1", 8, 1000L);

        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("cannot be sent directly"));
    }

    @Test
    void shouldKeepAggregateCommandsWithInsufficientContextUnsupported() {
        Assertions.assertFalse(service.setSystemState("battery-rs485-1", 1, 1, 1000L).isMappedToModuleCommand());
        Assertions.assertFalse(service.automaticSetSubmoduleAddress("battery-rs485-1", 1, 1000L).isMappedToModuleCommand());
        Assertions.assertFalse(service.clearHostDebuggingData("battery-rs485-1", 1000L).isMappedToModuleCommand());
        Assertions.assertFalse(service.settingInternalResistanceCoefficient("battery-rs485-1", 1, 1.0d, 1000L)
                .isMappedToModuleCommand());
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
    void shouldResolveChannelNameByConfigIdAndBatteryGroup() {
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

        String channelName = service.resolveChannelName(20L, 2);

        Assertions.assertEquals("battery-rs485-2", channelName);
    }

    @Test
    void shouldNotFallbackToBlankConfigIdChannelWhenConfigIdIsSpecified() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig channel = new BatteryCollectorChannelConfig();
        channel.setName("battery-rs485-1");
        channel.setBatteryGroup(1);
        properties.getChannels().add(channel);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(10L, 1);

        Assertions.assertNull(channelName);
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
