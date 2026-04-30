package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
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
        group.setCreateTime(new Date(999_000L));
        group.setPollBatchNo("batch-1");
        group.setGroupModuleFresh(Boolean.TRUE);

        BatteryModuleGroupRealtime calculation = service.buildCalculation(
                1,
                Arrays.asList(
                        cell(1, 2.10d, 100, 25.0d, 999_000L),
                        cell(2, 2.20d, 120, 26.5d, 998_000L),
                        cell(3, 2.05d, 90, 24.0d, 997_000L)
                ),
                group,
                now,
                "batch-1",
                new Date(990_000L),
                180_000L);

        Assertions.assertEquals(3, calculation.getCellCount());
        Assertions.assertEquals(3, calculation.getOnlineCellCount());
        Assertions.assertEquals(0, calculation.getStaleCellCount());
        Assertions.assertTrue(calculation.getDataFresh());
        Assertions.assertEquals(2, calculation.getMaxVoltageBatNum());
        Assertions.assertEquals(2.20d, calculation.getMaxCellVoltage(), 0.0001d);
        Assertions.assertEquals(3, calculation.getMinVoltageBatNum());
        Assertions.assertEquals(2.05d, calculation.getMinCellVoltage(), 0.0001d);
        Assertions.assertEquals(2.116666d, calculation.getAvgCellVoltage(), 0.0001d);
        Assertions.assertEquals(0.15d, calculation.getVoltageRange(), 0.0001d);
        Assertions.assertEquals(30, calculation.getResistanceRange());
        Assertions.assertEquals(123.4d, calculation.getExternalVoltage(), 0.0001d);
        Assertions.assertTrue(calculation.getGroupModuleFresh());
    }

    @Test
    void shouldMarkStaleCellsOffline() {
        Date now = new Date(1_000_000L);

        BatteryModuleGroupRealtime calculation = service.buildCalculation(
                1,
                Arrays.asList(
                        cell(1, 2.10d, 100, 25.0d, 999_000L),
                        cell(2, 2.20d, 120, 26.5d, 700_000L)
                ),
                null,
                now,
                "batch-1",
                new Date(990_000L),
                180_000L);

        Assertions.assertEquals(2, calculation.getCellCount());
        Assertions.assertEquals(1, calculation.getOnlineCellCount());
        Assertions.assertEquals(1, calculation.getStaleCellCount());
        Assertions.assertFalse(calculation.getDataFresh());
    }

    @Test
    void shouldNotCopyStaleGroupModuleValuesWhenBatchDoesNotMatch() {
        Date now = new Date(1_000_000L);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(123.4d);
        group.setPackCurrent(-10.0d);
        group.setCreateTime(new Date(900_000L));
        group.setPollBatchNo("old-batch");
        group.setGroupModuleFresh(Boolean.TRUE);

        BatteryModuleGroupRealtime calculation = service.buildCalculation(
                1,
                Arrays.asList(cell(1, 2.10d, 100, 25.0d, 999_000L)),
                group,
                now,
                "batch-1",
                new Date(990_000L),
                180_000L);

        Assertions.assertFalse(calculation.getGroupModuleFresh());
        Assertions.assertNull(calculation.getExternalVoltage());
        Assertions.assertNull(calculation.getPackCurrent());
        Assertions.assertEquals(new Date(900_000L), calculation.getLatestGroupUpdateTime());
    }

    private BatteryModuleCellRealtime cell(int address,
                                           double voltage,
                                           int resistance,
                                           double temperature,
                                           long updateTimeMs) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(address);
        cell.setVoltage(voltage);
        cell.setResistance(resistance);
        cell.setTemperature(temperature);
        cell.setCreateTime(new Date(updateTimeMs));
        return cell;
    }
}
