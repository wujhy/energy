package com.shanhe.project.device.config.controller;

import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class BatteryReportLogControllerTest {

    @Test
    void shouldUseRealtimeReportAndFillAlarmsWhenSwitchEnabled() {
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLog realtimeLog = reportLog("realtime", monitor(1), monitor(2));
        AlarmLog cellAlarm = alarm(2);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(realtimeLog);
        Mockito.when(alarmLogService.selectBatteryAlarmLogListCache(1)).thenReturn(Collections.singletonList(cellAlarm));
        BatteryReportLogController controller = controller(true, reportLogService, adapterService, alarmLogService);

        AjaxResult result = controller.detailList(1L, 1);

        BatteryReportLog data = (BatteryReportLog) result.get(AjaxResult.DATA_TAG);
        Assertions.assertSame(realtimeLog, data);
        Assertions.assertEquals(0, data.getAlarm());
        Assertions.assertEquals(Collections.singletonList(cellAlarm), data.getAlarmList());
        Assertions.assertTrue(data.getBatteryList().get(0).getAlarmList().isEmpty());
        Assertions.assertEquals(Collections.singletonList(cellAlarm), data.getBatteryList().get(1).getAlarmList());
        Assertions.assertNull(data.getPackData());
        Assertions.assertNull(data.getMonitorData());
        Mockito.verify(adapterService).buildReportLog(1);
        Mockito.verify(reportLogService, Mockito.never()).selectLastHasAlarm(1);
    }

    @Test
    void shouldFallbackLegacyReportWhenRealtimeHasNoData() {
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLog legacyLog = reportLog("legacy", monitor(1));
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(new BatteryReportLog());
        Mockito.when(reportLogService.selectLastHasAlarm(1)).thenReturn(legacyLog);
        BatteryReportLogController controller = controller(true, reportLogService, adapterService, alarmLogService);

        AjaxResult result = controller.detailList(1L, 1);

        Assertions.assertSame(legacyLog, result.get(AjaxResult.DATA_TAG));
        Mockito.verify(adapterService).buildReportLog(1);
        Mockito.verify(reportLogService).selectLastHasAlarm(1);
        Mockito.verifyNoInteractions(alarmLogService);
    }

    @Test
    void shouldKeepLegacyReportWhenRealtimeSwitchDisabled() {
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLog legacyLog = reportLog("legacy", monitor(1));
        Mockito.when(reportLogService.selectLastHasAlarm(1)).thenReturn(legacyLog);
        BatteryReportLogController controller = controller(false, reportLogService, adapterService, alarmLogService);

        AjaxResult result = controller.detailList(1L, 1);

        Assertions.assertSame(legacyLog, result.get(AjaxResult.DATA_TAG));
        Mockito.verifyNoInteractions(adapterService);
        Mockito.verify(reportLogService).selectLastHasAlarm(1);
        Mockito.verifyNoInteractions(alarmLogService);
    }

    private BatteryReportLogController controller(boolean realtimeEnabled,
                                                  BatteryReportLogService reportLogService,
                                                  BatteryModuleReportLogAdapterService adapterService,
                                                  IAlarmLogService alarmLogService) {
        BatteryReportLogController controller = new BatteryReportLogController();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeEnabled);
        ReflectionTestUtils.setField(controller, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(controller, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(controller, "batteryModuleReportLogAdapterService", adapterService);
        ReflectionTestUtils.setField(controller, "alarmLogService", alarmLogService);
        return controller;
    }

    private BatteryReportLog reportLog(String source, BatteryMonitor... monitors) {
        BatteryReportLog reportLog = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("source", source);
        reportLog.setPackParam(packParam);
        reportLog.setBatteryList(monitors == null ? Collections.emptyList() : Arrays.asList(monitors));
        reportLog.setPackData("{}");
        reportLog.setMonitorData("[]");
        return reportLog;
    }

    private BatteryMonitor monitor(Integer batNum) {
        BatteryMonitor monitor = new BatteryMonitor();
        monitor.setBatNum(batNum);
        return monitor;
    }

    private AlarmLog alarm(Integer modelNum) {
        AlarmLog alarm = new AlarmLog();
        alarm.setModelNum(modelNum);
        return alarm;
    }
}
