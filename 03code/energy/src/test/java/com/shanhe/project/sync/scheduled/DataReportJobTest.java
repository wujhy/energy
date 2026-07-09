package com.shanhe.project.sync.scheduled;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.sync.domain.ConfigHistoryItemVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class DataReportJobTest {

    @Test
    void shouldResolveCachedRealtimeSnapshot() {
        DataReportJob job = new DataReportJob();
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleRealtimeSnapshot snapshot = snapshot();
        ReflectionTestUtils.setField(job, "realtimeSnapshotService", snapshotService);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot);

        BatteryModuleRealtimeSnapshot result = job.resolveRealtimeSnapshot(1);

        Assertions.assertSame(snapshot, result);
    }

    @Test
    void shouldRejectIncompleteRealtimeSnapshot() {
        DataReportJob job = new DataReportJob();
        Assertions.assertFalse(job.isUsableRealtimeSnapshot(null));
        Assertions.assertFalse(job.isUsableRealtimeSnapshot(BatteryModuleRealtimeSnapshot.builder().build()));
        Assertions.assertFalse(job.isUsableRealtimeSnapshot(BatteryModuleRealtimeSnapshot.builder()
                .group(group())
                .cells(Collections.emptyList())
                .build()));
    }

    @Test
    void shouldAcceptRealtimeSnapshotWithGroupAndCells() {
        DataReportJob job = new DataReportJob();
        Assertions.assertTrue(job.isUsableRealtimeSnapshot(snapshot()));
    }

    @Test
    void shouldBuildGroupItemsFromRealtimeGroup() {
        DataReportJob job = new DataReportJob();
        List<ConfigHistoryItemVo> items = job.buildGroupItems(group());

        Map<String, String> itemMap = items.stream()
                .collect(Collectors.toMap(ConfigHistoryItemVo::getItemCode, ConfigHistoryItemVo::getItemValue));
        Assertions.assertEquals("52.1", itemMap.get("packVoltage"));
        Assertions.assertEquals("-11.2", itemMap.get("packCurrent"));
        Assertions.assertEquals("6", itemMap.get("batteryPackStatus"));
        Assertions.assertEquals("1", itemMap.get("deviceWorkIOStatus"));
    }

    private BatteryModuleRealtimeSnapshot snapshot() {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(1);
        cell.setVoltage(2.12);
        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group())
                .cells(Collections.singletonList(cell))
                .build();
    }

    private BatteryModuleGroupRealtime group() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPackVoltage(52.1);
        group.setChargeDischargeCurrent(-11.2);
        group.setFloatCurrent(0.3);
        group.setExternalVoltage(53.0);
        group.setBatteryPackStatus(6);
        group.setDeviceWorkIoStatus(1);
        return group;
    }
}
