package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.opt.domain.OptLog;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class BatteryOptRuntimeRecoveryServiceTest {

    @Test
    void shouldKeepBackupRunningLogWhenRealtimeStatusIsBackup() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.reportLogService.lastCache(1))
                .thenReturn(reportLog(BatteryPackStatusEnum.BACKUP.getCode()));
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService, Mockito.never())
                .update(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService, Mockito.never()).updateCache();
    }

    @Test
    void shouldCloseBackupRunningLogWhenRealtimeStatusIsNotBackup() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.reportLogService.lastCache(1))
                .thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService)
                .update(Mockito.eq(100L), Mockito.eq(1), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService).updateCache();
        Mockito.verifyNoInteractions(fixture.modeStatusService);
    }

    @Test
    void shouldKeepBackupRunningLogWhenRealtimeStatusIsUnknown() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.reportLogService.lastCache(1)).thenReturn(new BatteryReportLog());
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService, Mockito.never())
                .update(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService, Mockito.never()).updateCache();
    }

    private static OptLog backupLog() {
        OptLog log = new OptLog();
        log.setId(100L);
        log.setPackNum(1);
        log.setType(BatteryTestEnum._5.getDictValue());
        log.setCreateTime(new Date(System.currentTimeMillis() - 13L * 60L * 60L * 1000L));
        return log;
    }

    private static BatteryReportLog reportLog(String status) {
        BatteryReportLog reportLog = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("batteryPackStatus", status);
        reportLog.setPackParam(packParam);
        return reportLog;
    }

    private static class Fixture {
        private final BatteryOptRuntimeRecoveryService service = new BatteryOptRuntimeRecoveryService();
        private final OptLogService optLogService = Mockito.mock(OptLogService.class);
        private final BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        private final BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        private final BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);

        private Fixture() {
            BatteryCollectorProperties properties = new BatteryCollectorProperties();
            properties.setJsonTcpRealtimeSourceEnabled(false);
            ReflectionTestUtils.setField(service, "optLogService", optLogService);
            ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
            ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
            ReflectionTestUtils.setField(service, "batteryModuleReportLogAdapterService", adapterService);
            ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);
        }
    }
}
