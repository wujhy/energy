package com.shanhe.project.manage.stat.service.impl;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.stat.domain.DevBatteryMonomer;
import com.shanhe.project.manage.stat.mapper.DevBatteryMonomerMapper;
import com.shanhe.project.sync.service.ClientReportService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DevBatteryMonomerServiceImplTest {

    @Test
    void shouldResolveBatteryCellsFromRealtimeSnapshot() {
        DevBatteryMonomerServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(service);
        BatteryModuleCellRealtime cell = cell(1, 120);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .cells(Collections.singletonList(cell))
                .build());

        List<BatteryModuleCellRealtime> result = service.resolveBatteryCells(1);

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(cell, result.get(0));
    }

    @Test
    void shouldReturnEmptyCellsWhenRealtimeSnapshotUnavailable() {
        DevBatteryMonomerServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(service);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);

        List<BatteryModuleCellRealtime> result = service.resolveBatteryCells(1);

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void shouldCalculateMaxResistanceFromRealtimeSnapshot() {
        DevBatteryMonomerServiceImpl service = newService();
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(service);
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        DevBatteryMonomerMapper monomerMapper = (DevBatteryMonomerMapper) ReflectionTestUtils.getField(service, "devBatteryMonomerMapper");
        BatteryPack pack = new BatteryPack();
        pack.setPackId(10L);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .cells(Arrays.asList(cell(1, 120), cell(2, 100)))
                .build());
        Mockito.when(monomerMapper.selectList(10L)).thenReturn(Arrays.asList(
                new DevBatteryMonomer(10L, 1, 100),
                new DevBatteryMonomer(10L, 2, 100)));

        Double maxResistance = service.getMaxResistance(1);

        Assertions.assertEquals(0.2d, maxResistance, 0.0001d);
    }

    private DevBatteryMonomerServiceImpl newService() {
        DevBatteryMonomerServiceImpl service = new DevBatteryMonomerServiceImpl();
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", Mockito.mock(BatteryModuleRealtimeSnapshotService.class));
        ReflectionTestUtils.setField(service, "batteryPackService", Mockito.mock(IBatteryPackService.class));
        ReflectionTestUtils.setField(service, "devBatteryMonomerMapper", Mockito.mock(DevBatteryMonomerMapper.class));
        ReflectionTestUtils.setField(service, "clientReportService", Mockito.mock(ClientReportService.class));
        return service;
    }

    private BatteryModuleRealtimeSnapshotService snapshotService(DevBatteryMonomerServiceImpl service) {
        return (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
    }

    private BatteryModuleCellRealtime cell(Integer batNum, Integer resistance) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setResistance(resistance);
        return cell;
    }
}
