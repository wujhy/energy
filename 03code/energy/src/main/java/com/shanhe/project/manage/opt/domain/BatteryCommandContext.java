package com.shanhe.project.manage.opt.domain;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.service.BatteryOptExecuteType;

/**
 * 蓄电池测试命令入口上下文。
 */
public class BatteryCommandContext {
    public final DevBatteryOpt opt;
    public final BatteryTestEnum testEnum;
    public final BatteryOptExecuteType executeType;
    public final Config config;
    public final BatteryPack batteryPack;
    public final int batteryCount;
    public final String optLogSource;
    public BatteryReportLog reportLog;

    public BatteryCommandContext(DevBatteryOpt opt,
                                 BatteryTestEnum testEnum,
                                 BatteryOptExecuteType executeType,
                                 Config config,
                                 BatteryPack batteryPack,
                                 int batteryCount,
                                 String optLogSource) {
        this.opt = opt;
        this.testEnum = testEnum;
        this.executeType = executeType;
        this.config = config;
        this.batteryPack = batteryPack;
        this.batteryCount = batteryCount;
        this.optLogSource = optLogSource;
    }
}
