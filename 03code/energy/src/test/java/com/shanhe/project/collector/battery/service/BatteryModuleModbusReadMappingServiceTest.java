package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

class BatteryModuleModbusReadMappingServiceTest {

    @Test
    void shouldMapCellReferenceRegisters() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.123d, 101, 25.1d, 3.4d),
                cell(2, 2.456d, 102, -5.0d, null)
        ));

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper);

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
        group.setBackupDuration(120);
        group.setCapacity(100.5d);
        group.setDisChargeDuration(60);
        group.setDisChargeCapacity(18.6d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper);

        Assertions.assertArrayEquals(new int[]{1234, 29877, 10123, 751, 0},
                service.readHoldingRegisters(1, 411729, 5));
        Assertions.assertArrayEquals(new int[]{2, 2345, 1, 2111, 2222, 0, 234},
                service.readHoldingRegisters(1, 411734, 7));
        Assertions.assertArrayEquals(new int[]{3, 120, 1, 90, 106, 4, 802, 1, 700, 756, 805, 994},
                service.readHoldingRegisters(1, 411741, 12));
        Assertions.assertArrayEquals(new int[]{5, 120, 1005, 60, 186},
                service.readHoldingRegisters(1, 411762, 5));
    }

    @Test
    void shouldRejectUnsupportedAddressAndInvalidQuantity() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleModbusReadMappingService service = new BatteryModuleModbusReadMappingService(mapper);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 411753, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, 126));
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
