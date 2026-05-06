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
}
