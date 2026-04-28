package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals("battery-rs485-1", result.getChannelName());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.GET_BATTERY_GROUP_INFO, result.getCommandDefinition());
        Assertions.assertTrue(result.getMessage().contains("cannot be sent directly"));
    }
}
