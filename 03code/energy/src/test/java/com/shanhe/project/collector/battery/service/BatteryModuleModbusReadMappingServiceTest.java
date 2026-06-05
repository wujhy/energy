package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * BatteryDeviceStateService 在单测中传 null，状态寄存器测试需要单独 mock。
 */

class BatteryModuleModbusReadMappingServiceTest {

    @Test
    void shouldMapCellReferenceRegisters() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.123d, 101, 25.1d, 3.4d),
                cell(2, 2.456d, 102, -5.0d, null)
        ));

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{2123, 2456}, service.readHoldingRegisters(1, 410004, 2));
        Assertions.assertArrayEquals(new int[]{101, 102}, service.readHoldingRegisters(1, 410252, 2));
        Assertions.assertArrayEquals(new int[]{751, 450}, service.readHoldingRegisters(1, 410500, 2));
        Assertions.assertArrayEquals(new int[]{34, 0}, service.readHoldingRegisters(1, 410748, 2));
    }

    @Test
    void shouldMapGroupReferenceRegistersWithFallbacksAndOffsets() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(123.4d);
        group.setChargeDischargeCurrent(-12.3d);
        group.setFloatCurrent(0.123d);
        group.setEnvironmentTemperature1(25.1d);
        group.setEnvironmentTemperature2(null);
        group.setMaxVoltageBatNum(2);
        group.setMaxCellVoltage(2.345d);
        group.setMinVoltageBatNum(1);
        group.setMinCellVoltage(2.111d);
        group.setAvgCellVoltage(2.222d);
        group.setVoltageRange(0.234d);
        group.setMaxResistanceBatNum(3);
        group.setMaxInternalResistance(120);
        group.setMinResistanceBatNum(1);
        group.setMinInternalResistance(90);
        group.setAvgInternalResistance(105.6d);
        group.setMaxTemperatureBatNum(4);
        group.setMaxCellTemperature(30.2d);
        group.setMinTemperatureBatNum(1);
        group.setMinCellTemperature(20.0d);
        group.setAvgCellTemperature(25.6d);
        group.setBatteryPackSoc(80.5d);
        group.setBatteryPackSoh(99.4d);
        group.setBatteryPackStatus(5);
        group.setResistanceTestStatus(2);
        group.setBackupDuration(120);
        group.setCapacity(100.5d);
        group.setDisChargeDuration(60);
        group.setDisChargeCapacity(18.6d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{1234, 29877, 10123, 751, 0},
                service.readHoldingRegisters(1, 411729, 5));
        Assertions.assertArrayEquals(new int[]{2, 2345, 1, 2111, 2222, 0, 234},
                service.readHoldingRegisters(1, 411734, 7));
        Assertions.assertArrayEquals(new int[]{3, 120, 1, 90, 106, 4, 802, 1, 700, 756, 805, 994},
                service.readHoldingRegisters(1, 411741, 12));
        Assertions.assertArrayEquals(new int[]{0x0502, 120, 1005, 60, 186},
                service.readHoldingRegisters(1, 411762, 5));
    }

    @Test
    void shouldRejectUnsupportedAddressAndInvalidQuantity() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.0d, 100, 25.0d, null)));
        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 411753, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, 126));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(null, 410004, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, 0));
    }

    @Test
    void shouldThrowWhenDataNotReady() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(null);
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 410004, 1));
    }

    @Test
    void shouldReturnZeroForMissingCellsWhenGroupExists() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(null);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(48.0d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        // 单体数据为空时返回 0，组数据正常读取
        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 410004, 1));
        Assertions.assertArrayEquals(new int[]{480}, service.readHoldingRegisters(1, 411729, 1));
    }

    @Test
    void shouldHandleBoundaryCellAddresses() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.0d, 100, 25.0d, null),
                cell(245, 2.5d, 200, 30.0d, 5.0d)
        ));

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper, null, null);

        // 单体 1 和 245 的电压
        Assertions.assertArrayEquals(new int[]{2000}, service.readHoldingRegisters(1, 410004, 1));
        Assertions.assertArrayEquals(new int[]{2500}, service.readHoldingRegisters(1, 410248, 1));
        // 单体 1 和 245 的内阻
        Assertions.assertArrayEquals(new int[]{100}, service.readHoldingRegisters(1, 410252, 1));
        Assertions.assertArrayEquals(new int[]{200}, service.readHoldingRegisters(1, 410496, 1));
        // 单体 1 和 245 的温度 (25+50)*10=750, (30+50)*10=800
        Assertions.assertArrayEquals(new int[]{750}, service.readHoldingRegisters(1, 410500, 1));
        Assertions.assertArrayEquals(new int[]{800}, service.readHoldingRegisters(1, 410744, 1));
    }

    private BatteryModuleCellRealtime cell(int batNum,
                                           double voltage,
                                           int resistance,
                                           double temperature,
                                           Double swollenVoltage) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setVoltage(voltage);
        cell.setResistance(resistance);
        cell.setTemperature(temperature);
        cell.setSwollenVoltage(swollenVoltage);
        return cell;
    }
}
