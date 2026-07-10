package com.shanhe.project.manage.screen.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLogIndex;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ScreenServiceImplTest {

    @Test
    void shouldBuildBatteryListFromRealtimeSnapshot() {
        ScreenServiceImpl service = newService();
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(service, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        Mockito.when(reportLogService.batteryList()).thenReturn(Collections.singletonList(index(1, "old")));
        Mockito.when(batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue()))
                .thenReturn(Collections.singletonList(pack(1, 10L)));
        Mockito.when(alarmLogService.isAlarmByCache(1)).thenReturn(0);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(1, 53.2, 2));

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.get(0).getPackNum());
        Assertions.assertEquals(10L, result.get(0).getConfigId());
        Assertions.assertEquals(0, result.get(0).getAlarm());
        Assertions.assertEquals(53.2, result.get(0).getPackParam().get("packVoltage"));
        Assertions.assertEquals(2, result.get(0).getPackParam().get("batteryPackStatus"));
    }

    @Test
    void shouldFallbackPerPackWhenRealtimeSnapshotUnavailable() {
        ScreenServiceImpl service = newService();
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        BatteryModuleRealtimeSnapshotService snapshotService =
                (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(service, "realtimeSnapshotService");
        BatteryReportLogIndex fallback = index(1, "old");
        Mockito.when(reportLogService.batteryList()).thenReturn(Collections.singletonList(fallback));
        Mockito.when(batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue()))
                .thenReturn(Collections.singletonList(pack(1, 10L)));
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(fallback, result.get(0));
    }

    @Test
    void shouldFallbackToLegacyListWhenPackConfigEmpty() {
        ScreenServiceImpl service = newService();
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        List<BatteryReportLogIndex> legacyList = Collections.singletonList(index(1, "old"));
        Mockito.when(reportLogService.batteryList()).thenReturn(legacyList);
        Mockito.when(batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue()))
                .thenReturn(Collections.emptyList());

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertSame(legacyList, result);
    }

    private ScreenServiceImpl newService() {
        ScreenServiceImpl service = new ScreenServiceImpl();
        ReflectionTestUtils.setField(service, "batteryReportLogService", Mockito.mock(BatteryReportLogService.class));
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", Mockito.mock(BatteryModuleRealtimeSnapshotService.class));
        ReflectionTestUtils.setField(service, "batteryPackService", Mockito.mock(IBatteryPackService.class));
        ReflectionTestUtils.setField(service, "alarmLogService", Mockito.mock(IAlarmLogService.class));
        return service;
    }

    private BatteryModuleRealtimeSnapshot snapshot(Integer packNum, Double packVoltage, Integer status) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(packNum);
        group.setPackVoltage(packVoltage);
        group.setBatteryPackStatus(status);
        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(packNum)
                .group(group)
                .build();
    }

    private BatteryPack pack(Integer packNum, Long configId) {
        BatteryPack pack = new BatteryPack();
        pack.setPackNum(packNum);
        pack.setConfigId(configId);
        return pack;
    }

    private BatteryReportLogIndex index(Integer packNum, String source) {
        BatteryReportLogIndex index = new BatteryReportLogIndex();
        index.setPackNum(packNum);
        index.setConfigId(Constants.DEFAULT_CONFIG_ID);
        index.setPackParam(packParam(source));
        return index;
    }

    private Map<String, Object> packParam(String source) {
        Map<String, Object> packParam = new LinkedHashMap<>();
        packParam.put("source", source);
        return packParam;
    }
}