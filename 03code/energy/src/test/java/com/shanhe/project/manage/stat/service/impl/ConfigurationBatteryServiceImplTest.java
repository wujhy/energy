package com.shanhe.project.manage.stat.service.impl;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class ConfigurationBatteryServiceImplTest {

    @Test
    void shouldUseRealtimeReportLogWhenEnabled() {
        ConfigurationBatteryServiceImpl service = newService(true);
        BatteryReportLogService oldService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLog realtimeLog = reportLog("realtime");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(realtimeLog);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertSame(realtimeLog, result);
        Mockito.verifyNoInteractions(oldService);
    }

    @Test
    void shouldFallbackToOldCacheWhenRealtimeReportLogUnavailable() {
        ConfigurationBatteryServiceImpl service = newService(true);
        BatteryReportLogService oldService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLog oldLog = reportLog("old");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(new BatteryReportLog());
        Mockito.when(oldService.lastCache(1)).thenReturn(oldLog);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertSame(oldLog, result);
    }

    @Test
    void shouldUseOldCacheWhenRealtimeSourceDisabled() {
        ConfigurationBatteryServiceImpl service = newService(false);
        BatteryReportLogService oldService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLog oldLog = reportLog("old");
        Mockito.when(oldService.lastCache(1)).thenReturn(oldLog);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertSame(oldLog, result);
        Mockito.verifyNoInteractions(adapterService);
    }

    private ConfigurationBatteryServiceImpl newService(boolean realtimeSourceEnabled) {
        ConfigurationBatteryServiceImpl service = new ConfigurationBatteryServiceImpl();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeSourceEnabled);
        ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(service, "batteryModuleReportLogAdapterService",
                Mockito.mock(BatteryModuleReportLogAdapterService.class));
        ReflectionTestUtils.setField(service, "batteryReportLogService", Mockito.mock(BatteryReportLogService.class));
        return service;
    }

    private BatteryReportLog reportLog(String source) {
        BatteryReportLog reportLog = new BatteryReportLog();
        Map<String, Object> packParam = new LinkedHashMap<>();
        packParam.put("source", source);
        reportLog.setPackParam(packParam);
        reportLog.setBatteryList(Collections.singletonList(new BatteryMonitor()));
        return reportLog;
    }
}
