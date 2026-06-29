package com.shanhe.project.scheduled;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.opt.domain.OptLog;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
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
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Assertions.assertTrue(captor.getValue().getOptCommand().contains("SKIPPED"));
    }

    @Test
    void shouldSkipWhenPlanIsNotDue() {
        BatteryOptScheduleJob job = new BatteryOptScheduleJob();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(BatteryTestEnum._2.getDictValue());
        opt.setIsEnabled(YesNoEnum.YES.getDictValue());
        opt.setTestTime(new Date(System.currentTimeMillis() + 3600_000L));

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        Mockito.verifyNoInteractions(controlBattery);
    }

    @Test
    void shouldSkipWhenPlanHasNoTestTime() {
        BatteryOptScheduleJob job = new BatteryOptScheduleJob();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(BatteryTestEnum._2.getDictValue());
        opt.setIsEnabled(YesNoEnum.YES.getDictValue());
        opt.setTestTime(null);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        Mockito.verifyNoInteractions(controlBattery);
    }

    @Test
    void shouldSkipWhenRunningOptLogExists() {
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

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.selectRunningList(1))
                .thenReturn(Collections.singletonList(new OptLog()));
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        Mockito.verifyNoInteractions(controlBattery);
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Assertions.assertTrue(captor.getValue().getOptCommand().contains("SKIPPED"));
    }

    @Test
    void shouldExecuteAndDisableOneTimePlan() {
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
        opt.setIntervalDays(null);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.any(DevBatteryOpt.class), Mockito.any()))
                .thenReturn(AjaxResult.success());
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Assertions.assertEquals(YesNoEnum.NO.getDictValue(), captor.getValue().getIsEnabled());
    }

    @Test
    void shouldExecuteAndUpdateNextScheduleTime() {
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
        opt.setIntervalDays(7);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.any(DevBatteryOpt.class), Mockito.any()))
                .thenReturn(AjaxResult.success());
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        long beforeRun = System.currentTimeMillis();
        job.executeDueBatteryOpt();

        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Date newTestTime = captor.getValue().getTestTime();
        Assertions.assertNotNull(newTestTime);
        Assertions.assertTrue(newTestTime.after(new Date(beforeRun)));
    }

    @Test
    void shouldNotUpdateWhenExecutionFails() {
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
        opt.setIntervalDays(7);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.any(DevBatteryOpt.class), Mockito.any()))
                .thenReturn(AjaxResult.error("failed"));
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Assertions.assertTrue(captor.getValue().getOptCommand().contains("FAILED"));
        Assertions.assertEquals(7, captor.getValue().getIntervalDays());
    }

    @Test
    void shouldSwallowExecutionException() {
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

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.any(DevBatteryOpt.class), Mockito.any()))
                .thenThrow(new RuntimeException("unexpected"));
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        Assertions.assertDoesNotThrow(job::executeDueBatteryOpt);
    }

    @Test
    void shouldSkipWhenCollectorModeRunningForSingleIR() {
        BatteryOptScheduleJob job = new BatteryOptScheduleJob();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(BatteryTestEnum._6.getDictValue());
        opt.setIsEnabled(YesNoEnum.YES.getDictValue());
        opt.setTestTime(new Date(System.currentTimeMillis() - 1000L));
        BatteryModeInfo modeInfo = new BatteryModeInfo();
        modeInfo.setPackNum(1);
        modeInfo.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        modeInfo.setStatus(1);

        Mockito.when(optService.selectDevBatteryOptList(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(Collections.singletonList(opt));
        Mockito.when(optLogService.getRunningOptLog(1, BatteryTestEnum._6.getDictValue()))
                .thenReturn(null);
        Mockito.when(modeStatusService.get(1)).thenReturn(modeInfo);
        ReflectionTestUtils.setField(job, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(job, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(job, "optLogService", optLogService);
        ReflectionTestUtils.setField(job, "batteryModeStatusService", modeStatusService);

        job.executeDueBatteryOpt();

        Mockito.verifyNoInteractions(controlBattery);
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).updateDevBatteryOpt(captor.capture());
        Assertions.assertTrue(captor.getValue().getOptCommand().contains("SKIPPED"));
    }
}
