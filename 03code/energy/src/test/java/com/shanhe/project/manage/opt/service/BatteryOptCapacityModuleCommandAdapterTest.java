package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import com.shanhe.project.manage.opt.domain.OptLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class BatteryOptCapacityModuleCommandAdapterTest {

    @Test
    void shouldKeepCapacityCommandAsPlaceholder() {
        Fixture fixture = new Fixture();

        Assertions.assertNull(fixture.adapter.tryExecute(context(BatteryTestEnum._3)));
        Assertions.assertNull(fixture.adapter.tryStop(opt(BatteryTestEnum._3.getDictValue())));
    }

    @Test
    void shouldStartBackupThroughLifecycle() {
        Fixture fixture = new Fixture();
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(fixture.backupService.startBackup(1)).thenReturn(success);

        AjaxResult result = fixture.adapter.tryExecute(context(BatteryTestEnum._5));

        Assertions.assertSame(success, result);
        Mockito.verify(fixture.optLogService).updateRuntime(
                100L, BatteryTestLifecycleService.STARTING, null);
        Mockito.verify(fixture.optLogService).updateRuntime(
                100L, BatteryTestLifecycleService.RUNNING, null);
    }

    @Test
    void shouldStopBackupBetweenStoppingAndCancelled() {
        Fixture fixture = new Fixture();
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(fixture.backupService.stopBackup(1)).thenReturn(success);
        InOrder order = Mockito.inOrder(fixture.optLogService, fixture.backupService);

        AjaxResult result = fixture.adapter.tryStop(opt(BatteryTestEnum._5.getDictValue()));

        Assertions.assertSame(success, result);
        order.verify(fixture.optLogService).updateRuntime(
                100L, BatteryTestLifecycleService.STOPPING, null);
        order.verify(fixture.backupService).stopBackup(1);
        order.verify(fixture.optLogService).updateRuntime(
                100L, BatteryTestLifecycleService.CANCELLED, 1);
    }

    @Test
    void shouldKeepStoppingWhenBackupRestoreFails() {
        Fixture fixture = new Fixture();
        AjaxResult failed = AjaxResult.error("failed", 0);
        Mockito.when(fixture.backupService.stopBackup(1)).thenReturn(failed);

        AjaxResult result = fixture.adapter.tryStop(opt(BatteryTestEnum._5.getDictValue()));

        Assertions.assertSame(failed, result);
        Mockito.verify(fixture.optLogService).updateRuntime(
                100L, BatteryTestLifecycleService.STOPPING, null);
        Mockito.verify(fixture.optLogService, Mockito.never()).updateRuntime(
                100L, BatteryTestLifecycleService.CANCELLED, 1);
    }

    @Test
    void shouldIgnoreCollectorManagedAndUnsupportedTypes() {
        Fixture fixture = new Fixture();

        Assertions.assertNull(fixture.adapter.tryExecute(context(BatteryTestEnum._1)));
        Assertions.assertNull(fixture.adapter.tryExecute(context(BatteryTestEnum._2)));
        Assertions.assertNull(fixture.adapter.tryExecute(context(BatteryTestEnum._6)));
        Assertions.assertNull(fixture.adapter.tryStop(opt(BatteryTestEnum._1.getDictValue())));
        Assertions.assertNull(fixture.adapter.tryStop(opt(BatteryTestEnum._2.getDictValue())));
        Assertions.assertNull(fixture.adapter.tryStop(opt(BatteryTestEnum._6.getDictValue())));
    }

    @Test
    void shouldReturnNullForEmptyInput() {
        Fixture fixture = new Fixture();

        Assertions.assertNull(fixture.adapter.tryExecute(new DevBatteryOpt()));
        Assertions.assertNull(fixture.adapter.tryExecute((BatteryCommandContext) null));
        Assertions.assertNull(fixture.adapter.tryStop(new DevBatteryOpt()));
    }

    private static BatteryCommandContext context(BatteryTestEnum testEnum) {
        return new BatteryCommandContext(opt(testEnum.getDictValue()), testEnum,
                BatteryOptExecuteType.MANUAL, null, null, 12, "web");
    }

    private static DevBatteryOpt opt(Integer testType) {
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(testType);
        return opt;
    }

    private static class Fixture {
        private final BatteryOptCapacityModuleCommandAdapter adapter =
                new BatteryOptCapacityModuleCommandAdapter();
        private final BackupExternalModuleControlService backupService =
                Mockito.mock(BackupExternalModuleControlService.class);
        private final OptLogService optLogService = Mockito.mock(OptLogService.class);
        private final BatteryModeStatusService modeStatusService =
                Mockito.mock(BatteryModeStatusService.class);

        private Fixture() {
            OptLog running = new OptLog();
            running.setId(100L);
            running.setPackNum(1);
            running.setType(BatteryTestEnum._5.getDictValue());
            Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.emptyList());
            Mockito.when(optLogService.insert(Mockito.eq(1), Mockito.eq(BatteryTestEnum._5.getDictValue()),
                            Mockito.isNull(), Mockito.eq("web"), Mockito.any()))
                    .thenReturn(100L);
            Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._5.getDictValue()))
                    .thenReturn(running);
            BatteryTestLifecycleService lifecycleService = new BatteryTestLifecycleService();
            ReflectionTestUtils.setField(lifecycleService, "optLogService", optLogService);
            ReflectionTestUtils.setField(lifecycleService, "modeStatusService", modeStatusService);
            ReflectionTestUtils.setField(adapter, "backupExternalModuleControlService", backupService);
            ReflectionTestUtils.setField(adapter, "lifecycleService", lifecycleService);
        }
    }
}
