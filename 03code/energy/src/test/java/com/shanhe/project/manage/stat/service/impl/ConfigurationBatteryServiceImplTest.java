package com.shanhe.project.manage.stat.service.impl;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class ConfigurationBatteryServiceImplTest {

    @Test
    void shouldBuildReportLogFromRealtimeSnapshot() {
        ConfigurationBatteryServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot());

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getPackNum());
        Assertions.assertEquals(52.6, result.getPackParam().get("packVoltage"));
        Assertions.assertEquals(2, result.getPackParam().get("batteryPackStatus"));
        Assertions.assertEquals(1, result.getBatteryList().size());
        Assertions.assertEquals(10, result.getBatteryList().get(0).getBatNum());
        Assertions.assertEquals(2.21, result.getBatteryList().get(0).getVoltage());
    }

    @Test
    void shouldReturnNullWhenRealtimeSnapshotUnavailable() {
        ConfigurationBatteryServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertNull(result);
    }

    private ConfigurationBatteryServiceImpl newService() {
        ConfigurationBatteryServiceImpl service = new ConfigurationBatteryServiceImpl();
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", Mockito.mock(BatteryModuleRealtimeSnapshotService.class));
        return service;
    }

    private BatteryModuleRealtimeSnapshot snapshot() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPackVoltage(52.6);
        group.setBatteryPackStatus(2);
        group.setBackupDuration(120);

        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(10);
        cell.setVoltage(2.21);
        cell.setResistance(100);
        cell.setTemperature(28.5);

        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group)
                .cells(Collections.singletonList(cell))
                .build();
    }
}