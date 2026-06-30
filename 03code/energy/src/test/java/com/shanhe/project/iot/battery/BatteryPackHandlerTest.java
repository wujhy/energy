package com.shanhe.project.iot.battery;

import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;

class BatteryPackHandlerTest {

    @Test
    void shouldPreferRealtimeReportLogAsOldInfo() {
        BatteryPackHandler handler = new BatteryPackHandler();
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog realtimeLog = reportLogWithPackParam();
        BatteryReportLog historyLog = new BatteryReportLog();
        historyLog.setCreateTime(new Date());
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(realtimeLog);
        Mockito.when(reportLogService.lastCache(1)).thenReturn(historyLog);
        ReflectionTestUtils.setField(handler, "batteryModuleReportLogAdapterService", adapterService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", reportLogService);

        BatteryReportLog result = ReflectionTestUtils.invokeMethod(
                handler, "loadRecentOldReportLog", new Config(), 1);

        Assertions.assertSame(realtimeLog, result);
        Mockito.verify(reportLogService, Mockito.never()).lastCache(1);
    }

    @Test
    void shouldFallbackToRecentHistoryWhenRealtimeReportLogEmpty() {
        BatteryPackHandler handler = new BatteryPackHandler();
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog emptyRealtimeLog = new BatteryReportLog();
        emptyRealtimeLog.setPackParam(new HashMap<>());
        BatteryReportLog historyLog = new BatteryReportLog();
        historyLog.setCreateTime(new Date());
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(emptyRealtimeLog);
        Mockito.when(reportLogService.lastCache(1)).thenReturn(historyLog);
        ReflectionTestUtils.setField(handler, "batteryModuleReportLogAdapterService", adapterService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", reportLogService);

        BatteryReportLog result = ReflectionTestUtils.invokeMethod(
                handler, "loadRecentOldReportLog", new Config(), 1);

        Assertions.assertSame(historyLog, result);
    }

    private BatteryReportLog reportLogWithPackParam() {
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setPackParam(new HashMap<>());
        reportLog.getPackParam().put("packVoltage", "54.0");
        return reportLog;
    }
}
