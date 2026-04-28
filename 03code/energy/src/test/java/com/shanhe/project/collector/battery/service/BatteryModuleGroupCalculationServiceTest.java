package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupCalculation;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;

class BatteryModuleGroupCalculationServiceTest {

    private final BatteryModuleGroupCalculationService service = new BatteryModuleGroupCalculationService();

    @Test
    void shouldCalculateGroupMetrics() {
        Date now = new Date(1_000_000L);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(123.4d);
        group.setChargeDischargeCurrent(10.1d);
        group.setFloatCurrent(0.12d);
        group.setEnvironmentTemperature1(25.0d);
        group.setEnvironmentTemperature2(26.0d);
        group.setUpdateTime(new Date(999_000L));

        BatteryModuleGroupCalculation calculation = service.buildCalculation(
                "battery-group-1",
                1,
                Arrays.asList(
                        cell(1, 2.10d, 100, 25.0d, true, 999_000L),
                        cell(2, 2.20d, 120, 26.5d, true, 998_000L),
                        cell(3, 2.05d, 90, 24.0d, true, 997_000L)
                ),
                group,
                now,
                180_000L);

        Assertions.assertEquals(3, calculation.getCellCount());
        Assertions.assertEquals(3, calculation.getOnlineCellCount());
        Assertions.assertEquals(0, calculation.getStaleCellCount());
        Assertions.assertTrue(calculation.getDataFresh());
        Assertions.assertEquals(2, calculation.getMaxVoltageModuleAddress());
        Assertions.assertEquals(2.20d, calculation.getMaxCellVoltage(), 0.0001d);
        Assertions.assertEquals(3, calculation.getMinVoltageModuleAddress());
        Assertions.assertEquals(2.05d, calculation.getMinCellVoltage(), 0.0001d);
        Assertions.assertEquals(2.116666d, calculation.getAvgCellVoltage(), 0.0001d);
        Assertions.assertEquals(0.15d, calculation.getVoltageRange(), 0.0001d);
        Assertions.assertEquals(30, calculation.getResistanceRange());
        Assertions.assertEquals(123.4d, calculation.getExternalVoltage(), 0.0001d);
    }

    @Test
    void shouldMarkStaleCellsOffline() {
        Date now = new Date(1_000_000L);

        BatteryModuleGroupCalculation calculation = service.buildCalculation(
                "battery-group-1",
                1,
                Arrays.asList(
                        cell(1, 2.10d, 100, 25.0d, true, 999_000L),
                        cell(2, 2.20d, 120, 26.5d, true, 700_000L)
                ),
                null,
                now,
                180_000L);

        Assertions.assertEquals(2, calculation.getCellCount());
        Assertions.assertEquals(1, calculation.getOnlineCellCount());
        Assertions.assertEquals(1, calculation.getStaleCellCount());
        Assertions.assertFalse(calculation.getDataFresh());
    }

    private BatteryModuleCellRealtime cell(int address,
                                           double voltage,
                                           int resistance,
                                           double temperature,
                                           boolean success,
                                           long updateTimeMs) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setModuleAddress(address);
        cell.setCellVoltage(voltage);
        cell.setInternalResistance(resistance);
        cell.setCellTemperature(temperature);
        cell.setSuccess(success);
        cell.setUpdateTime(new Date(updateTimeMs));
        return cell;
    }
}
