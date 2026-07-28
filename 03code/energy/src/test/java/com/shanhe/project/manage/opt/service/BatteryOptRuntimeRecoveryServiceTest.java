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
        // 唯一索引保证单组只有一个活跃业务日志；缓存返回 _3 运行日志
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1))
                .thenReturn(runningLog(BatteryTestEnum._3.getDictValue()));
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._3.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._7.getDictValue());
    }

    @Test
    void shouldSkipCloseWhenNoRunningCacheLog() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(null);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService, Mockito.never()).getRunningOptLog(Mockito.anyInt(), Mockito.anyInt());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void shouldSkipCloseWithinStartupGrace() {
        Fixture fixture = new Fixture();
        OptLog fresh = runningLog(BatteryTestEnum._3.getDictValue());
        fresh.setCreateTime(new Date());
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(fresh);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(Mockito.anyInt(), Mockito.anyInt());
    }

    @Test
    void shouldKeepResistanceLogUntilLifecycleCompletes() {
        Fixture fixture = new Fixture();
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1))
                .thenReturn(runningLog(BatteryTestEnum._1.getDictValue()));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(5, 0, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._1.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._3.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._7.getDictValue());
    }

    @Test
    void shouldCloseBackupLogByRealtimeStatusOnlyAfterConfirmWindow() {
        Fixture fixture = new Fixture();
        fixture.properties.setBackupEndConfirmMs(0L);
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backupLog());
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.optLogService).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldResetBackupEndMarkerWhenStatusReturnsToBackup() {
        Fixture fixture = new Fixture();
        fixture.properties.setBackupEndConfirmMs(3_600_000L);
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backupLog());
        Mockito.when(fixture.optLogService.getRunningOptLog(Mockito.eq(1), Mockito.anyInt()))
                .thenAnswer(invocation -> runningLog(invocation.getArgument(1)));
        BatteryRealtimePostProcessContext ended = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(6, null, "batch-1"))
                .build();
        BatteryRealtimePostProcessContext backup = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-2")
                .group(group(5, 0, "batch-2"))
                .build();

        // 单帧离开 BACKUP -> 回到 BACKUP -> 再离开：防抖标记被重置，始终不关闭 _5
        fixture.service.process(ended);
        fixture.service.process(backup);
        fixture.service.process(ended);

        Mockito.verify(fixture.optLogService, Mockito.never()).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldAutoStopBackupWhenEndVoltageReached() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        OptLog backup = backupLog();
        backup.setContent("{\"endVoltage\":45.0}");
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backup);
        Mockito.when(fixture.lifecycleService.completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any())).thenReturn(true);
        BatteryModuleGroupRealtime group = group(5, 0, "batch-1");
        group.setPackVoltage(44.5D);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group)
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.lifecycleService).completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any());
    }

    @Test
    void shouldAutoStopBackupWhenDischargeTimeElapsed() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        OptLog backup = backupLog();
        backup.setContent("{\"dischargeTime\":60}");
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backup);
        Mockito.when(fixture.lifecycleService.completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any())).thenReturn(true);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group(5, 0, "batch-1"))
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.lifecycleService).completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any());
    }

    @Test
    void shouldNotAutoStopBackupWhenConditionsNotMet() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        OptLog backup = backupLog();
        // 快照未配置放电时长，组电压高于截止电压：不停止
        backup.setContent("{\"endVoltage\":45.0}");
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backup);
        BatteryModuleGroupRealtime group = group(5, 0, "batch-1");
        group.setPackVoltage(48.0D);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group)
                .build();

        fixture.service.process(context);

        Mockito.verify(fixture.lifecycleService, Mockito.never()).completeAfterRestore(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldNotAutoStopBackupWithoutRunParams() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        // 手动启动且未快照停止参数：不做自动停止，由手动停止/防抖/watchdog 收口
        OptLog backup = backupLog();
        Mockito.when(fixture.optLogService.selectRunningCacheLog(1)).thenReturn(backup);
        Mockito.when(fixture.optLogService.selectRunningList(null))
                .thenReturn(Collections.singletonList(backup));
        BatteryModuleGroupRealtime group = group(5, 0, "batch-1");
        group.setPackVoltage(30.0D);
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo("batch-1")
                .group(group)
                .build();

        fixture.service.process(context);
        int stopped = fixture.service.autoStopExpiredBackupRuns();

        org.junit.jupiter.api.Assertions.assertEquals(0, stopped);
        Mockito.verify(fixture.lifecycleService, Mockito.never()).completeAfterRestore(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldAutoStopExpiredBackupRunWithoutRealtimeData() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        OptLog backup = backupLog();
        backup.setContent("{\"endVoltage\":45.0,\"dischargeTime\":60}");
        Mockito.when(fixture.optLogService.selectRunningList(null))
                .thenReturn(Collections.singletonList(backup));
        Mockito.when(fixture.lifecycleService.completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any())).thenReturn(true);

        // 无采集数据：定时兜底按备电时长到期停止，截止电压不参与判断
        int stopped = fixture.service.autoStopExpiredBackupRuns();

        org.junit.jupiter.api.Assertions.assertEquals(1, stopped);
        Mockito.verify(fixture.lifecycleService).completeAfterRestore(Mockito.eq(backup), Mockito.isNull(),
                Mockito.isNull(), Mockito.any());
    }

    @Test
    void shouldNotAutoStopExpiredBackupRunBeforeDischargeTime() {
        Fixture fixture = new Fixture();
        ReflectionTestUtils.setField(fixture.service, "lifecycleService", fixture.lifecycleService);
        OptLog backup = backupLog();
        // 运行 13 小时，快照放电时长 14 小时：未到期不停止
        backup.setContent("{\"dischargeTime\":" + (14 * 60) + "}");
        Mockito.when(fixture.optLogService.selectRunningList(null))
                .thenReturn(Collections.singletonList(backup));

        int stopped = fixture.service.autoStopExpiredBackupRuns();

        org.junit.jupiter.api.Assertions.assertEquals(0, stopped);
        Mockito.verify(fixture.lifecycleService, Mockito.never()).completeAfterRestore(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
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
        private final BatteryTestLifecycleService lifecycleService = Mockito.mock(BatteryTestLifecycleService.class);

        private Fixture() {
            ReflectionTestUtils.setField(service, "optLogService", optLogService);
            ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
            ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
            ReflectionTestUtils.setField(service, "realtimeSnapshotService", snapshotService);
        }
    }
}
