package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
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
        Mockito.when(fixture.snapshotService.getCachedSnapshot(1))
                .thenReturn(snapshot(BatteryPackStatusEnum.BACKUP.getCode()));
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
        Mockito.when(fixture.snapshotService.getCachedSnapshot(1))
                .thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService)
                .update(Mockito.eq(100L), Mockito.eq(1), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService, Mockito.never()).updateCache();
        Mockito.verifyNoInteractions(fixture.modeStatusService);
    }

    @Test
    void shouldKeepBackupRunningLogBeforeConfirmWindow() {
        Fixture fixture = new Fixture();
        fixture.properties.setBackupRuntimeRecoveryConfirmMs(14L * 60L * 60L * 1000L);
        Mockito.when(fixture.snapshotService.getCachedSnapshot(1))
                .thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService, Mockito.never())
                .update(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService, Mockito.never()).updateCache();
    }

    @Test
    void shouldKeepBackupRunningLogWhenRealtimeStatusIsUnknown() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder().build());
        Mockito.when(fixture.optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(backupLog()));

        fixture.service.recoverPack(1);

        Mockito.verify(fixture.optLogService, Mockito.never())
                .update(Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class));
        Mockito.verify(fixture.optLogService, Mockito.never()).updateCache();
    }


    @Test
    void shouldCloseExistingLogsWhenRealtimeBatteryStatusEnded() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._3.getDictValue());
        // _5 未超过确认窗口时保留，避免单帧状态波动误停备电
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._7.getDictValue());
    }

    @Test
    void shouldCloseBackupLogByRealtimeStatusOnlyAfterConfirmWindow() {
        Fixture fixture = new Fixture();
        OptLog backup = backupLog();
        backup.setLastProgressAt(timeText(System.currentTimeMillis() - 13L * 60L * 60L * 1000L));
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    Integer type = invocation.getArgument(1);
                    return BatteryTestEnum._5.getDictValue().equals(type) ? backup : runningLog(type);
                });
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldKeepBackupLogByRealtimeStatusWhenProgressFresh() {
        Fixture fixture = new Fixture();
        OptLog backup = backupLog();
        backup.setLastProgressAt(timeText(System.currentTimeMillis() - 60_000L));
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> {
                    Integer type = invocation.getArgument(1);
                    return BatteryTestEnum._5.getDictValue().equals(type) ? backup : runningLog(type);
                });
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldKeepBatteryLogsWhenRealtimeBatteryStatusActive() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(5, 0, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._1.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._3.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._7.getDictValue());
    }

    @Test
    void shouldRejectRealtimePostProcessWhenBatchMismatched() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "other-batch"))
                .build();

        org.junit.jupiter.api.Assertions.assertFalse(fixture.service.shouldProcess(context));
    }

    private static OptLog runningLog(Integer type) {
        OptLog log = new OptLog();
        log.setId(100L + type);
        log.setPackNum(1);
        log.setType(type);
        return log;
    }
    private static OptLog backupLog() {
        OptLog log = new OptLog();
        log.setId(100L);
        log.setPackNum(1);
        log.setType(BatteryTestEnum._5.getDictValue());
        log.setCreateTime(new Date(System.currentTimeMillis() - 13L * 60L * 60L * 1000L));
        return log;
    }

    private static String timeText(long epochMillis) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(epochMillis));
    }


    private static BatteryModuleGroupRealtime group(Integer batteryPackStatus, Integer resistanceTestStatus, String pollBatchNo) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setBatteryPackStatus(batteryPackStatus);
        group.setResistanceTestStatus(resistanceTestStatus);
        group.setPollBatchNo(pollBatchNo);
        return group;
    }
    private static BatteryModuleRealtimeSnapshot snapshot(String status) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setBatteryPackStatus(Integer.valueOf(status));
        return BatteryModuleRealtimeSnapshot.builder()
                .group(group)
                .build();
    }

    private static class Fixture {
        private final BatteryOptRuntimeRecoveryService service = new BatteryOptRuntimeRecoveryService();
        private final OptLogService optLogService = Mockito.mock(OptLogService.class);
        private final BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        private final BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        private final BatteryCollectorProperties properties = new BatteryCollectorProperties();

        private Fixture() {
            ReflectionTestUtils.setField(service, "optLogService", optLogService);
            ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
            ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
            ReflectionTestUtils.setField(service, "realtimeSnapshotService", snapshotService);
        }
    }
}
