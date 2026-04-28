package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleFrameDataParserServiceTest {

    private final BatteryCollectorFrameCodec codec = new BatteryCollectorFrameCodec();
    private final BatteryModuleFrameDataParserService service = new BatteryModuleFrameDataParserService();

    @Test
    void shouldParseSingleModuleInfo() {
        BatteryCollectorFrame frame = codec.buildRequest(0x05, 0x81,
                new byte[]{0x00, 0x09, (byte) 0xC4, 0x00, 0x64, 0x00, (byte) 0xFB, 0x01, 0x04, (byte) 0xD2});

        BatteryModuleFrameData data = service.parse(frame);

        Assertions.assertNotNull(data);
        Assertions.assertEquals(BatteryModuleDataType.SINGLE_MODULE_INFO, data.getType());
        Assertions.assertEquals(5, data.getModuleAddress());
        Assertions.assertTrue(data.isSuccess());
        Assertions.assertEquals(2.5d, data.getCellVoltage(), 0.0001d);
        Assertions.assertEquals(100, data.getInternalResistance());
        Assertions.assertEquals(25.1d, data.getCellTemperature(), 0.0001d);
        Assertions.assertEquals(1, data.getLeakageStatus());
        Assertions.assertEquals(123.4d, data.getSwollenVoltage(), 0.0001d);
    }

    @Test
    void shouldParseArrayModuleInfo() {
        BatteryCollectorFrame frame = codec.buildRequest(0xF6, 0x81,
                new byte[]{0x00, 0x00, 0x7B, 0x00, 0x7B, 0x30, 0x39, (byte) 0xFF, (byte) 0x9C, 0x00, (byte) 0xFA});

        BatteryModuleFrameData data = service.parse(frame);

        Assertions.assertNotNull(data);
        Assertions.assertEquals(BatteryModuleDataType.ARRAY_MODULE_INFO, data.getType());
        Assertions.assertEquals(246, data.getModuleAddress());
        Assertions.assertEquals(12.3d, data.getChargeDischargeCurrent(), 0.0001d);
        Assertions.assertEquals(0.123d, data.getFloatCurrent(), 0.0001d);
        Assertions.assertEquals(123.45d, data.getExternalVoltage(), 0.0001d);
        Assertions.assertEquals(-10.0d, data.getEnvironmentTemperature1(), 0.0001d);
        Assertions.assertEquals(25.0d, data.getEnvironmentTemperature2(), 0.0001d);
    }

    @Test
    void shouldParseConnectResistanceVoltage() {
        BatteryCollectorFrame frame = codec.buildRequest(0x03, 0x91,
                new byte[]{0x00, 0x00, 0x61, (byte) 0xA8, 0x00, 0x00, (byte) 0xC3, 0x50});

        BatteryModuleFrameData data = service.parse(frame);

        Assertions.assertNotNull(data);
        Assertions.assertEquals(BatteryModuleDataType.CONNECT_RESISTANCE_VOLTAGE, data.getType());
        Assertions.assertEquals(2.5d, data.getConnectBatteryVoltage(), 0.0001d);
        Assertions.assertEquals(5.0d, data.getConnectTestVoltage(), 0.0001d);
    }

    @Test
    void shouldParseStatusResponse() {
        BatteryCollectorFrame frame = codec.buildRequest(0x03, 0x83, new byte[]{0x00});

        BatteryModuleFrameData data = service.parse(frame);

        Assertions.assertNotNull(data);
        Assertions.assertEquals(BatteryModuleDataType.STATUS_RESPONSE, data.getType());
        Assertions.assertEquals("SINGLE_BATTERY_BALANCE", data.getProtocolCode());
        Assertions.assertTrue(data.isSuccess());
        Assertions.assertEquals(0, data.getStatusCode());
    }

    @Test
    void shouldParseAutoSetAddressResponse() {
        BatteryCollectorFrame frame = codec.buildRequest(0x00, 0xA8, new byte[]{0x00, 0x05, 0x02});

        BatteryModuleFrameData data = service.parse(frame);

        Assertions.assertNotNull(data);
        Assertions.assertEquals(BatteryModuleDataType.AUTO_SET_ADDRESS_RESPONSE, data.getType());
        Assertions.assertEquals("AUTO_SET_MODULE_ADDRESS", data.getProtocolCode());
        Assertions.assertEquals(5, data.getAssignedModuleAddress());
        Assertions.assertEquals(2, data.getAutoSetAddressStep());
    }
}
