package com.shanhe.project.scheduled;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;

class BatteryOptScheduleJobTest {

    @Test
    void shouldSkipConnectResistanceWhenCollectorModeIsRunning() {
        BatteryOptScheduleJob job = new BatteryOptScheduleJob();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(BatteryTestEnum._2.getDictValue());
        opt.setIsEnabled(YesNoEnum.YES.getDictValue());
        opt.setTestTime(new Date(System.currentTimeMillis() - 1000L));
        BatteryModeInfo modeInfo = new BatteryModeInfo();
        modeInfo.setPackNum(1);
        modeInfo.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        modeInfo.setStatus(1);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        Mockito.when(modeStatusService.get(1)).thenReturn(modeInfo);
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        Mockito.verifyNoInteractions(controlBattery);
        Mockito.verify(optService, Mockito.never()).updateDevBatteryOpt(Mockito.any());
    }
}
