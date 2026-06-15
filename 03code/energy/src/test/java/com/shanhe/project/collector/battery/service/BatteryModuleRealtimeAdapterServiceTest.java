package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

class BatteryModuleRealtimeAdapterServiceTest {

    @Test
    void shouldReturnNullWhenDisabled() {
        BatteryModuleRealtimeAdapterService service = newService(false);

        Assertions.assertNull(service.getCellRealtime(1));
        Assertions.assertNull(service.getGroupRealtime(1));
        Assertions.assertFalse(service.isEnabled());
    }

    @Test
    void shouldReturnDataWhenEnabledAndDataExists() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(1);
        cell.setVoltage(2.123);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell));
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleRealtimeAdapterService service = newService(true, mapper);

        Assertions.assertNotNull(service.getCellRealtime(1));
        Assertions.assertEquals(1, service.getCellRealtime(1).size());
        Assertions.assertNotNull(service.getGroupRealtime(1));
        Assertions.assertEquals(1, service.getGroupRealtime(1).getPackNum());
    }

    @Test
    void shouldPreferSnapshotWhenEnabledAndSnapshotExists() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(3);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        Mockito.when(snapshotService.getSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .cells(Arrays.asList(cell))
                .group(group)
                .build());

        BatteryModuleRealtimeAdapterService service = newService(true, mapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        Assertions.assertEquals(3, service.getCellRealtime(1).get(0).getBatNum());
        Assertions.assertSame(group, service.getGroupRealtime(1));
        Mockito.verify(mapper, Mockito.never()).selectCells(1);
        Mockito.verify(mapper, Mockito.never()).selectGroup(1);
    }

    @Test
    void shouldReturnNullWhenDataMissing() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(null);
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleRealtimeAdapterService service = newService(true, mapper);

        Assertions.assertNull(service.getCellRealtime(1));
        Assertions.assertNull(service.getGroupRealtime(1));
    }

    @Test
    void shouldReturnNullWhenCellsEmpty() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Collections.emptyList());

        BatteryModuleRealtimeAdapterService service = newService(true, mapper);

        Assertions.assertNull(service.getCellRealtime(1));
    }

    @Test
    void shouldReturnNullForNullPackNum() {
        BatteryModuleRealtimeAdapterService service = newService(true);

        Assertions.assertNull(service.getCellRealtime(null));
        Assertions.assertNull(service.getGroupRealtime(null));
    }

    @Test
    void shouldReturnNullOnException() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenThrow(new RuntimeException("DB error"));

        BatteryModuleRealtimeAdapterService service = newService(true, mapper);

        Assertions.assertNull(service.getCellRealtime(1));
    }

    private BatteryModuleRealtimeAdapterService newService(boolean enabled) {
        return newService(enabled, Mockito.mock(BatteryModuleRealtimeMapper.class));
    }

    private BatteryModuleRealtimeAdapterService newService(boolean enabled, BatteryModuleRealtimeMapper mapper) {
        BatteryModuleRealtimeAdapterService service = new BatteryModuleRealtimeAdapterService();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(enabled);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "realtimeMapper", mapper);
        return service;
    }
}
