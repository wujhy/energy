package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryOptCapacityModuleCommandAdapterTest {

    @Test
    void shouldFallbackExecuteForCapacityAndBackupBeforeModuleChannelReady() {
        BatteryOptCapacityModuleCommandAdapter adapter = new BatteryOptCapacityModuleCommandAdapter();

        Assertions.assertNull(adapter.tryExecute(opt(BatteryTestEnum._3.getDictValue())));
        Assertions.assertNull(adapter.tryExecute(opt(BatteryTestEnum._5.getDictValue())));
    }

    @Test
    void shouldFallbackStopForCapacityAndBackupBeforeModuleChannelReady() {
        BatteryOptCapacityModuleCommandAdapter adapter = new BatteryOptCapacityModuleCommandAdapter();

        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._3.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._5.getDictValue())));
    }

    @Test
    void shouldIgnoreCollectorManagedAndUnsupportedTypes() {
        BatteryOptCapacityModuleCommandAdapter adapter = new BatteryOptCapacityModuleCommandAdapter();

        Assertions.assertNull(adapter.tryExecute(opt(BatteryTestEnum._1.getDictValue())));
        Assertions.assertNull(adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue())));
        Assertions.assertNull(adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._1.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._2.getDictValue())));
        Assertions.assertNull(adapter.tryStop(opt(BatteryTestEnum._6.getDictValue())));
    }

    @Test
    void shouldReturnNullForEmptyInput() {
        BatteryOptCapacityModuleCommandAdapter adapter = new BatteryOptCapacityModuleCommandAdapter();

        Assertions.assertNull(adapter.tryExecute(null));
        Assertions.assertNull(adapter.tryExecute(new DevBatteryOpt()));
        Assertions.assertNull(adapter.tryStop(null));
        Assertions.assertNull(adapter.tryStop(new DevBatteryOpt()));
    }

    private DevBatteryOpt opt(Integer testType) {
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(testType);
        return opt;
    }
}