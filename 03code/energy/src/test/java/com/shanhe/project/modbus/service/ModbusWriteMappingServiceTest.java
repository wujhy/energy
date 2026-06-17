package com.shanhe.project.modbus.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class ModbusWriteMappingServiceTest {

    @Test
    void shouldMapBalanceRegisterToInternalCommandService() {
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("COM1");
        Mockito.when(commandService.singleBatteryBalance("COM1", 1, 2, 1, null))
                .thenReturn(BatteryCollectorCommandResult.builder().success(true).build());
        Mockito.when(commandService.singleBatteryBalance("COM1", 1, 1, 255, null))
                .thenReturn(BatteryCollectorCommandResult.builder().success(true).build());
        ModbusWriteMappingService service = service(commandService);

        Assertions.assertTrue(service.writeSingleRegister(1, 404915, 0x0102));
        Assertions.assertTrue(service.writeSingleRegister(1, 404915, 0xFF01));

        Mockito.verify(commandService).singleBatteryBalance("COM1", 1, 2, 1, null);
        Mockito.verify(commandService).singleBatteryBalance("COM1", 1, 1, 255, null);
    }

    @Test
    void shouldReturnFalseWhenMappedCommandIsRejectedByInternalService() {
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("COM1");
        Mockito.when(commandService.singleBatteryBalance("COM1", 1, 1, 1, null))
                .thenReturn(BatteryCollectorCommandResult.builder().success(false).build());
        ModbusWriteMappingService service = service(commandService);

        Assertions.assertFalse(service.writeSingleRegister(1, 404915, 0x0101));
    }

    @Test
    void shouldRejectUnsupportedWriteRegister() {
        ModbusWriteMappingService service = service(Mockito.mock(BatteryCollectorCommandService.class));

        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 404901, 1));
        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 404902, 1));
        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 410004, 1));
    }

    @Test
    void shouldRejectManualAddressSingleRegisterWrite() {
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        ModbusWriteMappingService service = service(commandService);

        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 404921, 8));
        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 404922, 8));
        Mockito.verifyNoInteractions(commandService);
    }

    @Test
    void shouldRejectInvalidRegisterValuePackNumAndModelNum() {
        ModbusWriteMappingService service = service(Mockito.mock(BatteryCollectorCommandService.class));

        Assertions.assertThrows(ModbusIllegalDataValueException.class,
                () -> service.writeSingleRegister(null, 404915, 0x0101));
        Assertions.assertThrows(ModbusIllegalDataValueException.class,
                () -> service.writeSingleRegister(1, 404915, -1));
        Assertions.assertThrows(ModbusIllegalDataValueException.class,
                () -> service.writeSingleRegister(1, 404915, 0x1_0000));
        Assertions.assertThrows(ModbusIllegalDataValueException.class,
                () -> service.writeSingleRegister(1, 404915, 0x0100));
        Assertions.assertThrows(ModbusIllegalDataValueException.class,
                () -> service.writeSingleRegister(1, 404915, 0x01F6));
    }

    @Test
    void shouldRejectBalanceWhenPackHasNoChannel() {
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        Mockito.when(commandService.resolveChannelName(1)).thenReturn(null);
        ModbusWriteMappingService service = service(commandService);

        Assertions.assertThrows(ModbusIllegalDataAddressException.class,
                () -> service.writeSingleRegister(1, 404915, 0x0101));
    }

    private ModbusWriteMappingService service(BatteryCollectorCommandService commandService) {
        ModbusWriteMappingService service = new ModbusWriteMappingService();
        ReflectionTestUtils.setField(service, "commandService", commandService);
        return service;
    }
}
