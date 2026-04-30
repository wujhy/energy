package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleControlCommandServiceTest {

    private final BatteryModuleControlCommandService service = new BatteryModuleControlCommandService();

    @Test
    void shouldBuildModuleControlCommands() {
        BatteryModuleControlCommand balance = service.singleBatteryBalance(8, 1);

        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_BALANCE, balance.getProtocolCode());
        Assertions.assertEquals(8, balance.getAddress());
        Assertions.assertEquals(0x03, balance.getRequestCode());
        Assertions.assertEquals(0x83, balance.getResponseCode());
        Assertions.assertArrayEquals(new byte[]{1}, balance.getPayload());
    }

    @Test
    void shouldBuildNoResponseBroadcastCommand() {
        BatteryModuleControlCommand command = service.connectStripResistanceTest();

        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST, command.getProtocolCode());
        Assertions.assertEquals(0, command.getAddress());
        Assertions.assertNull(command.getResponseCode());
        Assertions.assertEquals(0, command.getPayload().length);
    }

    @Test
    void shouldValidatePayloadLength() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.autoSetModuleAddress(0, 1, 2, 3));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.setCalibrationParameter(1, 1, 2, 3, 4, 5));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.setInternalResistanceCoefficient(1, 1, 2, 3));
    }

    @Test
    void shouldBuildBroadcastInternalResistanceCoefficientWithoutResponse() {
        BatteryModuleControlCommand command = service.setInternalResistanceCoefficient(0, 0x3F, 0x80, 0x00, 0x00);

        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, command.getProtocolCode());
        Assertions.assertEquals(0, command.getAddress());
        Assertions.assertEquals(0x12, command.getRequestCode());
        Assertions.assertNull(command.getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0x3F, (byte) 0x80, 0x00, 0x00}, command.getPayload());
    }

    @Test
    void shouldRejectUnicastCommandsWithBroadcastAddress() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.singleBatteryInternalResistanceTest(0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.singleBatteryBalance(0, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.setModuleAddress(0, 2));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.getConnectResistanceVoltage(0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.setCalibrationParameter(0, 1, 2, 3, 4));
    }
}
