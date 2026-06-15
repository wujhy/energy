package com.shanhe.project.modbus.rtu;

import com.shanhe.project.collector.battery.service.BatteryModuleModbusReadMappingService;
import com.shanhe.project.modbus.config.ModbusRtuProperties;
import com.shanhe.project.modbus.service.ModbusWriteMappingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class ModbusRtuServerTest {

    @Test
    void shouldIgnoreFrameWithInvalidCrc() {
        ModbusRtuServer server = server(1, Collections.emptyMap());
        BatteryModuleModbusReadMappingService readMappingService =
                (BatteryModuleModbusReadMappingService) ReflectionTestUtils.getField(server, "readMappingService");
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x01});
        frame[frame.length - 1] ^= 0x01;

        ReflectionTestUtils.invokeMethod(server, "processFrame", frame, frame.length);

        Mockito.verifyNoInteractions(readMappingService);
    }

    @Test
    void shouldIgnoreFrameForOtherStation() {
        ModbusRtuServer server = server(1, Collections.emptyMap());
        BatteryModuleModbusReadMappingService readMappingService =
                (BatteryModuleModbusReadMappingService) ReflectionTestUtils.getField(server, "readMappingService");
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x02, 0x03, 0x00, 0x00, 0x00, 0x01});

        ReflectionTestUtils.invokeMethod(server, "processFrame", frame, frame.length);

        Mockito.verifyNoInteractions(readMappingService);
    }

    @Test
    void shouldRejectInvalidReadQuantityBeforeMapping() {
        ModbusRtuServer server = server(1, Collections.emptyMap());
        BatteryModuleModbusReadMappingService readMappingService =
                (BatteryModuleModbusReadMappingService) ReflectionTestUtils.getField(server, "readMappingService");
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x00});

        ReflectionTestUtils.invokeMethod(server, "processFrame", frame, frame.length);

        Mockito.verifyNoInteractions(readMappingService);
    }

    @Test
    void shouldUseConfiguredStationPackMappingBeforeLowNibbleFallback() {
        ModbusRtuServer mapped = server(17, Collections.singletonMap(17, 6));
        ModbusRtuServer fallback = server(17, Collections.emptyMap());

        Integer mappedPack = ReflectionTestUtils.invokeMethod(mapped, "resolvePackNumFromStation", 17);
        Integer fallbackPack = ReflectionTestUtils.invokeMethod(fallback, "resolvePackNumFromStation", 17);

        Assertions.assertEquals(6, mappedPack);
        Assertions.assertEquals(1, fallbackPack);
    }

    private ModbusRtuServer server(int stationAddress, java.util.Map<Integer, Integer> stationPackMap) {
        ModbusRtuServer server = new ModbusRtuServer();
        ModbusRtuProperties properties = new ModbusRtuProperties();
        properties.setStationAddress(stationAddress);
        properties.setStationPackMap(stationPackMap);
        ReflectionTestUtils.setField(server, "modbusRtuProperties", properties);
        ReflectionTestUtils.setField(server, "readMappingService", Mockito.mock(BatteryModuleModbusReadMappingService.class));
        ReflectionTestUtils.setField(server, "writeMappingService", Mockito.mock(ModbusWriteMappingService.class));
        return server;
    }
}
