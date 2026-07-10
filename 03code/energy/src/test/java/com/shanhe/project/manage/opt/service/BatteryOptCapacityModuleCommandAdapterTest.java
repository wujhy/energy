package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryOptCapacityModuleCommandAdapterTest {

    @Test
    void shouldKeepCapacityCommandAsPlaceholder() {
        BatteryOptCapacityModuleCommandAdapter adapter = adapter();

        Assertions.assertNull(adapter.tryExecute(context(BatteryTestEnum._3)));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._3.getDictValue())));
    }

    @Test
    void shouldStartBackupThroughExternalModule() {
        BatteryOptCapacityModuleCommandAdapter adapter = adapter();
        BackupExternalModuleControlService backupService = field(adapter, "backupExternalModuleControlService");
        OptLogService optLogService = field(adapter, "optLogService");
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(backupService.startBackup(1)).thenReturn(success);

        AjaxResult result = adapter.tryExecute(context(BatteryTestEnum._5));

        Assertions.assertSame(success, result);
        Mockito.verify(optLogService).insert(1, BatteryTestEnum._5.getDictValue(), null, "web");
    }

    @Test
    void shouldStopBackupThroughExternalModule() {
        BatteryOptCapacityModuleCommandAdapter adapter = adapter();
        BackupExternalModuleControlService backupService = field(adapter, "backupExternalModuleControlService");
        OptLogService optLogService = field(adapter, "optLogService");
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(backupService.stopBackup(1)).thenReturn(success);

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._5.getDictValue()));

        Assertions.assertSame(success, result);
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldIgnoreCollectorManagedAndUnsupportedTypes() {
        BatteryOptCapacityModuleCommandAdapter adapter = adapter();

        Assertions.assertNull(adapter.tryExecute(context(BatteryTestEnum._1)));
        Assertions.assertNull(adapter.tryExecute(context(BatteryTestEnum._2)));
        Assertions.assertNull(adapter.tryExecute(context(BatteryTestEnum._6)));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._1.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._2.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._6.getDictValue())));
    }

    @Test
    void shouldReturnNullForEmptyInput() {
        BatteryOptCapacityModuleCommandAdapter adapter = adapter();

        Assertions.assertNull(adapter.tryExecute(new DevBatteryOpt()));
        Assertions.assertNull(adapter.tryExecute((BatteryCommandContext) null));
        Assertions.assertNull(adapter.tryStop(new DevBatteryOpt()));
    }

    private BatteryOptCapacityModuleCommandAdapter adapter() {
        BatteryOptCapacityModuleCommandAdapter adapter = new BatteryOptCapacityModuleCommandAdapter();
        ReflectionTestUtils.setField(adapter, "backupExternalModuleControlService",
                Mockito.mock(BackupExternalModuleControlService.class));
        ReflectionTestUtils.setField(adapter, "optLogService", Mockito.mock(OptLogService.class));
        return adapter;
    }

    private BatteryCommandContext context(BatteryTestEnum testEnum) {
        return new BatteryCommandContext(
                opt(testEnum.getDictValue()),
                testEnum,
                BatteryOptExecuteType.MANUAL,
                null,
                null,
                12,
                "web");
    }

    private DevBatteryOpt opt(Integer testType) {
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(testType);
        return opt;
    }

    @SuppressWarnings("unchecked")
    private <T> T field(BatteryOptCapacityModuleCommandAdapter adapter, String name) {
        return (T) ReflectionTestUtils.getField(adapter, name);
    }
}