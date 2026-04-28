package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryPendingRequestTest {

    @Test
    void shouldKeepPollingCommandAutoPollFlag() {
        BatteryPendingRequest autoPoll = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.MODULE_INFO,
                8,
                new byte[0],
                true);
        BatteryPendingRequest explicit = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                9,
                new byte[0],
                false);

        Assertions.assertTrue(autoPoll.isAutoPoll());
        Assertions.assertFalse(explicit.isAutoPoll());
        Assertions.assertEquals(8, autoPoll.getRequestAddress());
        Assertions.assertEquals(9, explicit.getRequestAddress());
        Assertions.assertEquals(0x01, autoPoll.getRequestCode());
        Assertions.assertEquals(0x81, autoPoll.getResponseCode());
    }
}
