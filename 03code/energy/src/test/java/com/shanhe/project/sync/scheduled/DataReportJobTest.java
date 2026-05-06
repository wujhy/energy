package com.shanhe.project.sync.scheduled;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

class DataReportJobTest {

    @Test
    void shouldUseOldCacheWhenRealtimeSourceDisabled() {
        DataReportJob job = newJob(false);
        BatteryReportLogService oldService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLog oldLog = log("old");
        ReflectionTestUtils.setField(job, "batteryReportLogService", oldService);
        ReflectionTestUtils.setField(job, "batteryModuleReportLogAdapterService", adapterService);
        Mockito.when(oldService.lastCache(10L, 1)).thenReturn(oldLog);

        BatteryReportLog result = job.resolveBatteryReportLog(10L, 1);

        Assertions.assertSame(oldLog, result);
        Mockito.verifyNoInteractions(adapterService);
    }

    @Test
    void shouldUseRealtimeLogFirstWhenRealtimeSourceEnabled() {
        DataReportJob job = newJob(true);
        BatteryReportLogService oldService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLog realtimeLog = log("realtime");
        ReflectionTestUtils.setField(job, "batteryReportLogService", oldService);
        ReflectionTestUtils.setField(job, "batteryModuleReportLogAdapterService", adapterService);
        Mockito.when(adapterService.buildReportLog(10L, 1)).thenReturn(realtimeLog);

        BatteryReportLog result = job.resolveBatteryReportLog(10L, 1);

        Assertions.assertSame(realtimeLog, result);
        Mockito.verifyNoInteractions(oldService);
    }

    @Test
    void shouldFallbackToOldCacheWhenRealtimeLogUnavailable() {
        DataReportJob job = newJob(true);
        BatteryReportLogService oldService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLog oldLog = log("old");
        ReflectionTestUtils.setField(job, "batteryReportLogService", oldService);
        ReflectionTestUtils.setField(job, "batteryModuleReportLogAdapterService", adapterService);
        Mockito.when(adapterService.buildReportLog(10L, 1)).thenReturn(new BatteryReportLog());
        Mockito.when(oldService.lastCache(10L, 1)).thenReturn(oldLog);

        BatteryReportLog result = job.resolveBatteryReportLog(10L, 1);

        Assertions.assertSame(oldLog, result);
    }

    private DataReportJob newJob(boolean realtimeSourceEnabled) {
        DataReportJob job = new DataReportJob();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeSourceEnabled);
        ReflectionTestUtils.setField(job, "batteryCollectorProperties", properties);
        return job;
    }

    private BatteryReportLog log(String value) {
        BatteryReportLog log = new BatteryReportLog();
        Map<String, Object> packParam = new LinkedHashMap<>();
        packParam.put("source", value);
        log.setPackParam(packParam);
        return log;
    }
}
