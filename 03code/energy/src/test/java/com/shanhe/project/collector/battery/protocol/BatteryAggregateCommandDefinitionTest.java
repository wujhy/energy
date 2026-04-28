package com.shanhe.project.collector.battery.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryAggregateCommandDefinitionTest {

    @Test
    void shouldContainPcAndNetworkCommonCommands() {
        Assertions.assertEquals(0x05, BatteryAggregateCommandDefinition.SET_SYSTEM_STATE.getRequestCode());
        Assertions.assertEquals(0x85, BatteryAggregateCommandDefinition.SET_SYSTEM_STATE.getResponseCode());
        Assertions.assertEquals(0x61, BatteryAggregateCommandDefinition.READ_DEVICE_IP_ADDRESS.getRequestCode());
        Assertions.assertEquals(0xB1, BatteryAggregateCommandDefinition.READ_DEVICE_IP_ADDRESS.getResponseCode());
        Assertions.assertEquals(0x3F, BatteryAggregateCommandDefinition.READ_MAC_ADDRESS.getRequestCode());
        Assertions.assertEquals(0xEF, BatteryAggregateCommandDefinition.READ_MAC_ADDRESS.getResponseCode());
        Assertions.assertEquals(0x5E, BatteryAggregateCommandDefinition.SET_DEVICE_IP_ADDRESS.getRequestCode());
        Assertions.assertEquals(0xDE, BatteryAggregateCommandDefinition.SET_DEVICE_IP_ADDRESS.getResponseCode());
    }
}
