package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleCellCompatibilityFillServiceTest {

    private final BatteryModuleCellCompatibilityFillService service = new BatteryModuleCellCompatibilityFillService();

    @Test
    void shouldFillConnectResistanceFromCacheByChannelGroup() {
        service.putConnectResistance(1, 8, 12.3d);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(2);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(12.3d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldFallbackToCellPackWhenChannelMissing() {
        service.putConnectResistance(2, 8, 45.6d);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(2);
        cell.setBatNum(8);

        service.fillFromCache(null, cell);

        Assertions.assertEquals(45.6d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldLeaveConnectResistanceEmptyWhenCacheMissing() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertNull(cell.getResistanceRageSlip());
    }

    @Test
    void shouldScopeConnectResistanceByBatteryGroupAndCellNumber() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(1, 9, 23.4d);
        service.putConnectResistance(2, 8, 45.6d);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(45.6d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldIgnoreNullConnectResistanceInputs() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(null, 8, 99.9d);
        service.putConnectResistance(1, null, 99.9d);
        service.putConnectResistance(1, 8, null);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(12.3d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldNotOverwriteExistingConnectResistanceWhenCacheMissing() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);
        cell.setResistanceRageSlip(77.7d);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(77.7d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldClearOnlySpecifiedPackConnectResistanceCache() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(2, 8, 45.6d);

        service.clearConnectResistanceCache(1);

        BatteryModuleCellRealtime clearedCell = new BatteryModuleCellRealtime();
        clearedCell.setPackNum(1);
        clearedCell.setBatNum(8);
        service.fillFromCache(null, clearedCell);
        Assertions.assertNull(clearedCell.getResistanceRageSlip());

        BatteryModuleCellRealtime retainedCell = new BatteryModuleCellRealtime();
        retainedCell.setPackNum(2);
        retainedCell.setBatNum(8);
        service.fillFromCache(null, retainedCell);
        Assertions.assertEquals(45.6d, retainedCell.getResistanceRageSlip(), 0.0001d);
    }
}
