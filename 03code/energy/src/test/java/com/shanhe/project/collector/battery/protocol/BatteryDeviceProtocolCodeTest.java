package com.shanhe.project.collector.battery.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryDeviceProtocolCodeTest {

    @Test
    void shouldMatchM460ModuleSideCommandMatrix() {
        assertCode(BatteryDeviceProtocolCode.MODULE_INFO, 0x01, 0x81, false);
        assertCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, 0x02, 0x82, true);
        assertCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_BALANCE, 0x03, 0x83, true);
        assertCode(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS, 0x08, 0x88, true);
        assertCode(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA, 0x0A, null, false);
        assertCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST, 0x0F, null, false);
        assertCode(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE, 0x11, 0x91, false);
        assertCode(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, 0x12, 0x92, true);
        assertCode(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS, 0x18, 0xA8, false);
        assertCode(BatteryDeviceProtocolCode.SET_CALIBRATION_PARAMETER, 0x76, 0xF6, true);
    }

    @Test
    void shouldFindCommonDeviceProtocolCodes() {
        Assertions.assertEquals(BatteryDeviceProtocolCode.MODULE_INFO, BatteryDeviceProtocolCode.find(0x01));
        Assertions.assertEquals(BatteryDeviceProtocolCode.MODULE_INFO, BatteryDeviceProtocolCode.find(0x81));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, BatteryDeviceProtocolCode.find(0x02));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, BatteryDeviceProtocolCode.find(0x82));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_BALANCE, BatteryDeviceProtocolCode.find(0x03));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_BALANCE, BatteryDeviceProtocolCode.find(0x83));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS, BatteryDeviceProtocolCode.find(0x08));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS, BatteryDeviceProtocolCode.find(0x88));
        Assertions.assertEquals(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA, BatteryDeviceProtocolCode.find(0x0A));
        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST, BatteryDeviceProtocolCode.find(0x0F));
        Assertions.assertEquals(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE, BatteryDeviceProtocolCode.find(0x11));
        Assertions.assertEquals(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE, BatteryDeviceProtocolCode.find(0x91));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, BatteryDeviceProtocolCode.find(0x12));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, BatteryDeviceProtocolCode.find(0x92));
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS, BatteryDeviceProtocolCode.find(0x18));
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS, BatteryDeviceProtocolCode.find(0xA8));
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_CALIBRATION_PARAMETER, BatteryDeviceProtocolCode.find(0x76));
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

    @Test
    void shouldNotTreatUpperAggregateInternalResistanceCoefficientCodesAsModuleCodes() {
        Assertions.assertNull(BatteryDeviceProtocolCode.find(0x19));
        Assertions.assertNull(BatteryDeviceProtocolCode.find(0x99));
        Assertions.assertFalse(BatteryDeviceProtocolCode.isKnown(0x19));
        Assertions.assertFalse(BatteryDeviceProtocolCode.isKnown(0x99));
    }

    private void assertCode(BatteryDeviceProtocolCode code,
                            int requestCode,
                            Integer responseCode,
                            boolean statusResponse) {
        Assertions.assertEquals(requestCode, code.getRequestCode());
        Assertions.assertEquals(responseCode, code.getResponseCode());
        Assertions.assertEquals(responseCode != null, code.hasResponseCode());
        Assertions.assertTrue(code.isRequest(requestCode));
        Assertions.assertTrue(code.matches(requestCode));
        Assertions.assertEquals(statusResponse, code.isStatusResponse());
        if (responseCode == null) {
            Assertions.assertFalse(code.isResponse(requestCode));
            return;
        }
        Assertions.assertTrue(code.isResponse(responseCode));
        Assertions.assertTrue(code.matches(responseCode));
    }
}
