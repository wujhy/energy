package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
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
}
