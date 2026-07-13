package com.shanhe.project.manage.stat.service.impl;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

class ConfigurationBatteryServiceImplTest {

    @Test
    void shouldResolveCurrentRealtimeFromSnapshot() {
        ConfigurationBatteryServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot());

        ConfigurationBatteryServiceImpl.CurrentBatteryRealtime result = service.resolveCurrentRealtime(1);

        BatteryModuleGroupRealtime group = (BatteryModuleGroupRealtime) ReflectionTestUtils.getField(result, "group");
        List<?> cells = (List<?>) ReflectionTestUtils.getField(result, "cells");
        Assertions.assertNotNull(group);
        Assertions.assertEquals(1, group.getPackNum());
        Assertions.assertEquals(52.6, group.getPackVoltage());
        Assertions.assertEquals(1, cells.size());
        Assertions.assertEquals(10, ((BatteryModuleCellRealtime) cells.get(0)).getBatNum());
    }

    @Test
    void shouldReturnEmptyCurrentRealtimeWhenSnapshotUnavailable() {
        ConfigurationBatteryServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);

        ConfigurationBatteryServiceImpl.CurrentBatteryRealtime result = service.resolveCurrentRealtime(1);

        Assertions.assertNull(ReflectionTestUtils.getField(result, "group"));
        Assertions.assertTrue(((List<?>) ReflectionTestUtils.getField(result, "cells")).isEmpty());
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