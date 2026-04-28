package com.shanhe.project.collector.battery.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryDeviceProtocolCodeTest {

    @Test
    void shouldFindCommonDeviceProtocolCodes() {
        Assertions.assertEquals(BatteryDeviceProtocolCode.MODULE_INFO, BatteryDeviceProtocolCode.find(0x81));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, BatteryDeviceProtocolCode.find(0x82));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS, BatteryDeviceProtocolCode.find(0x88));
        Assertions.assertEquals(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA, BatteryDeviceProtocolCode.find(0x0A));
        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST, BatteryDeviceProtocolCode.find(0x0F));
        Assertions.assertEquals(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE, BatteryDeviceProtocolCode.find(0x91));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, BatteryDeviceProtocolCode.find(0x92));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_CALIBRATION_PARAMETER, BatteryDeviceProtocolCode.find(0xF6));
    }

    @Test
    void shouldClassifyKnownCodes() {
        Assertions.assertFalse(BatteryDeviceProtocolCode.isKnown(0xE9));
        Assertions.assertFalse(BatteryDeviceProtocolCode.isKnown(0xEB));
        Assertions.assertTrue(BatteryDeviceProtocolCode.isKnown(0x01));
    }

    @Test
    void shouldKeepDefaultPollingProtocolOnlyOnModuleInfo() {
        Assertions.assertEquals(0x01, BatteryDeviceProtocolCode.MODULE_INFO.getRequestCode());
        Assertions.assertEquals(0x81, BatteryDeviceProtocolCode.MODULE_INFO.getResponseCode());
        Assertions.assertFalse(BatteryDeviceProtocolCode.MODULE_INFO.isStatusResponse());
        Assertions.assertTrue(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.isStatusResponse());
    }
}
