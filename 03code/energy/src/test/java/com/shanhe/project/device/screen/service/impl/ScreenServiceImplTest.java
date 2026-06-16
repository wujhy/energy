package com.shanhe.project.device.screen.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.BatteryReportLogIndex;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class ScreenServiceImplTest {

    @Test
    void shouldUseLegacyBatteryListWhenRealtimeSourceDisabled() {
        ScreenServiceImpl service = newService(false);
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        List<BatteryReportLogIndex> legacyList = Collections.singletonList(index(1, "old"));
        Mockito.when(reportLogService.batteryList()).thenReturn(legacyList);

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertSame(legacyList, result);
        Mockito.verifyNoInteractions(adapterService);
    }

    @Test
    void shouldBuildBatteryListFromRealtimeReportLogWhenEnabled() {
        ScreenServiceImpl service = newService(true);
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(service, "alarmLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(reportLogService.batteryList()).thenReturn(Collections.singletonList(index(1, "old")));
        Mockito.when(batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue()))
                .thenReturn(Collections.singletonList(pack(1, 10L)));
        Mockito.when(alarmLogService.isAlarmByCache(1)).thenReturn(0);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(1, "realtime"));

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(1, result.get(0).getPackNum());
        Assertions.assertEquals(10L, result.get(0).getConfigId());
        Assertions.assertEquals(0, result.get(0).getAlarm());
        Assertions.assertEquals("realtime", result.get(0).getPackParam().get("source"));
    }

    @Test
    void shouldFallbackPerPackWhenRealtimeReportLogUnavailable() {
        ScreenServiceImpl service = newService(true);
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogIndex fallback = index(1, "old");
        Mockito.when(reportLogService.batteryList()).thenReturn(Collections.singletonList(fallback));
        Mockito.when(batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue()))
                .thenReturn(Collections.singletonList(pack(1, 10L)));
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(new BatteryReportLog());

        List<BatteryReportLogIndex> result = service.batteryList();

        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(fallback, result.get(0));
    }

    private ScreenServiceImpl newService(boolean realtimeSourceEnabled) {
        ScreenServiceImpl service = new ScreenServiceImpl();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeSourceEnabled);
        ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(service, "batteryReportLogService", Mockito.mock(BatteryReportLogService.class));
        ReflectionTestUtils.setField(service, "batteryModuleReportLogAdapterService",
                Mockito.mock(BatteryModuleReportLogAdapterService.class));
        ReflectionTestUtils.setField(service, "batteryPackService", Mockito.mock(IBatteryPackService.class));
        ReflectionTestUtils.setField(service, "alarmLogService", Mockito.mock(IAlarmLogService.class));
        return service;
    }

    private BatteryPack pack(Integer packNum, Long configId) {
        BatteryPack pack = new BatteryPack();
        pack.setPackNum(packNum);
        pack.setConfigId(configId);
        return pack;
    }

    private BatteryReportLog reportLog(Integer packNum, String source) {
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setPackNum(packNum);
        reportLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        reportLog.setPackParam(packParam(source));
        return reportLog;
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
