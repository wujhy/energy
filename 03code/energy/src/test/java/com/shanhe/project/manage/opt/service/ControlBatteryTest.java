package com.shanhe.project.manage.opt.service;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.ResistanceTestStatusEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigService;
import com.shanhe.project.manage.opt.cmd.CmdBatteryControlService;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.service.OptLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class ControlBatteryTest {

    @Test
    void shouldUseRealtimeReportForCommandPrecheckWhenSwitchEnabled() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog(BatteryPackStatusEnum.CAPACITY_TEST.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(adapterService).buildReportLog(1);
        Mockito.verify(reportLogService, Mockito.never()).lastCache(1);
    }

    @Test
    void shouldFallbackToLegacyReportWhenRealtimeReportHasNoPackParam() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(new BatteryReportLog());
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(adapterService).buildReportLog(1);
        Mockito.verify(reportLogService).lastCache(1);
    }

    @Test
    void shouldKeepLegacyReportWhenRealtimeSwitchDisabled() {
        ControlBattery service = service(false);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verifyNoInteractions(adapterService);
        Mockito.verify(reportLogService).lastCache(1);
    }

    @Test
    void shouldRejectConnectResistanceWhenReportMissing() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(null);
        Mockito.when(reportLogService.lastCache(1)).thenReturn(null);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("暂无上报数据", result.get(AjaxResult.MSG_TAG));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldCheckConnectResistanceCurrentBeforeCollectorCommandAdapter() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryReportLog reportLog = reportLog(BatteryPackStatusEnum.IDLE.getCode());
        reportLog.getPackParam().put("packCurrent", 1D);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog);

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue())));

        Assertions.assertTrue(exception.getMessage().contains("组电流超过"));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldRejectCommandWhenAnyRunningLogExists() {
        ControlBattery service = service(true);
        OptLogService optLogService = (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.singletonList(new OptLog()));

        AjaxResult result = service.executeBatteryOpt(request(BatteryTestEnum._5.getDictValue()), BatteryOptExecuteType.SYNC);

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("蓄电池正在执行测试工作，请稍后再试！", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(optLogService).selectRunningList(1);
        Mockito.verifyNoInteractions(adapterService);
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldRejectStopForCollectorManagedTestTypes() {
        ControlBattery service = service(true);
        CmdBatteryControlService cmdService = (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(commandAdapter.tryStop(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.error("当前电池组没有正在执行的测试", 0));

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(cmdService, Mockito.never()).genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble());
    }

    @Test
    void shouldRejectStopFallbackForCollectorManagedTypeWhenAdapterNotHandled() {
        assertRejectStopFallbackForCollectorManagedType(BatteryTestEnum._2);
        assertRejectStopFallbackForCollectorManagedType(BatteryTestEnum._6);
    }

    private void assertRejectStopFallbackForCollectorManagedType(BatteryTestEnum testEnum) {
        ControlBattery service = service(true);
        CmdBatteryControlService cmdService = (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(commandAdapter.tryStop(Mockito.any(DevBatteryOpt.class))).thenReturn(null);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(testEnum.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("当前停止类型未完成新链路执行，禁止回退旧M460指令", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(commandAdapter).tryStop(Mockito.any(DevBatteryOpt.class));
        Mockito.verify(cmdService, Mockito.never()).genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble());
    }

    @Test
    void shouldDelegateStopToAdapterForSingleInternalResistance() {
        ControlBattery service = service(true);
        CmdBatteryControlService cmdService = (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(commandAdapter.tryStop(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success("stopped"));

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._6.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandAdapter).tryStop(Mockito.any(DevBatteryOpt.class));
        Mockito.verify(cmdService, Mockito.never()).genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble());
    }

    // ---- tryExecuteAdaptedCommand tests ----

    @Test
    void shouldRejectFloatChargeInAdaptedCommandPath() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._4.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("浮充"));
    }

    @Test
    void shouldReturnCollectorAdapterResultWhenTryExecuteSucceeds() {
        ControlBattery service = service(true);
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(commandAdapter.tryExecutePrepared(Mockito.any(BatteryCommandContext.class)))
                .thenReturn(AjaxResult.success("adapted"));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void shouldReturnCapacityAdapterResultWhenTryExecuteSucceeds() {
        ControlBattery service = service(true);
        BatteryOptCapacityModuleCommandAdapter capacityAdapter =
                (BatteryOptCapacityModuleCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCapacityModuleCommandAdapter");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(capacityAdapter.tryExecute(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success("capacity-adapted"));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._3.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
    }

    // ---- validateBeforeCommand tests ----

    @Test
    void shouldRejectWhenActiveAlarmExists() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        IAlarmLogService alarmLogService =
                (IAlarmLogService) ReflectionTestUtils.getField(service, "alarmLogService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setStatus(1);
        alarmLog.setDataInfo("通信异常告警");
        Mockito.when(alarmLogService.getByCache(Mockito.eq(1), Mockito.isNull(), Mockito.anyString()))
                .thenReturn(alarmLog);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("通信异常告警", result.get(AjaxResult.MSG_TAG));
    }

    // ---- validateTestCondition tests ----

    @Test
    void shouldPassValidationForNonType2WhenBatteryIdle() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        CmdBatteryControlService cmdService =
                (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("当前测试类型未完成新链路执行，禁止回退旧M460指令", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(cmdService, Mockito.never()).genCmd05(Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void shouldRejectNonType2WhenBatteryNotIdle() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.CAPACITY_TEST.getCode()));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("电池组处于非空闲状态，不允许测试！", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldPassType2ValidationWhenCurrentSufficient() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryReportLog log = reportLog(BatteryPackStatusEnum.IDLE.getCode());
        log.getPackParam().put("packCurrent", 10D);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(log);
        Mockito.when(commandAdapter.tryExecutePrepared(Mockito.any(BatteryCommandContext.class)))
                .thenReturn(AjaxResult.success("ok"));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void shouldPassType2ValidationWhenCurrentNull() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryReportLog log = reportLog(BatteryPackStatusEnum.IDLE.getCode());
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(log);
        Mockito.when(commandAdapter.tryExecutePrepared(Mockito.any(BatteryCommandContext.class)))
                .thenReturn(AjaxResult.success("ok"));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
    }

    // ---- legacy fallback boundary tests ----

    @Test
    void shouldReturnErrorForMigratedCollectorTypeWhenAdapterNotHandled() {
        ControlBattery service = service(false);
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        CmdBatteryControlService cmdService =
                (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        BatteryReportLog log = reportLog(BatteryPackStatusEnum.IDLE.getCode());
        log.getPackParam().put("packCurrent", 10D);
        Mockito.when(reportLogService.lastCache(1)).thenReturn(log);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("当前测试类型未完成新链路执行，禁止回退旧M460指令", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(cmdService, Mockito.never()).genCmd0F(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldGenerateCapacityTestCommandViaLegacyFallback() {
        ControlBattery service = service(false);
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        CmdBatteryControlService cmdService =
                (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(cmdService.genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.any(), Mockito.any()))
                .thenReturn("");

        DevBatteryOpt opt = request(BatteryTestEnum._3.getDictValue());
        opt.setDischargeTime(2);
        opt.setEndVoltage(48.0);
        AjaxResult result = service.toSendBatteryCmdToOat(opt);

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(cmdService).genCmd30(Mockito.any(), Mockito.eq(1), Mockito.eq("2"), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldReturnErrorForUnsupportedTestTypeInGenerateCommand() {
        ControlBattery service = service(false);
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryOptCapacityModuleCommandAdapter capacityAdapter =
                (BatteryOptCapacityModuleCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCapacityModuleCommandAdapter");
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(commandAdapter.tryExecutePrepared(Mockito.any(BatteryCommandContext.class))).thenReturn(null);
        Mockito.when(capacityAdapter.tryExecute(Mockito.any(BatteryCommandContext.class))).thenReturn(null);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._4.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("浮充"));
    }

    // ---- Stop command edge cases ----

    @Test
    void shouldStopMappedGroupInternalResistanceBeforeLegacyType1LocalClose() {
        ControlBattery service = service(true);
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        OptLogService optLogService =
                (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        Mockito.when(commandAdapter.tryStop(Mockito.any()))
                .thenReturn(AjaxResult.success("stopped"));

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandAdapter).tryStop(Mockito.any());
        Mockito.verifyNoInteractions(adapterService);
        Mockito.verify(optLogService, Mockito.never()).doStopTest(Mockito.anyInt(), Mockito.any());
    }
    @Test
    void shouldReturnSuccessForStopType1WhenTestingAndReportFresh() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        OptLogService optLogService =
                (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        BatteryReportLog log = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("resistanceTestStatus", ResistanceTestStatusEnum.TESTING.getCode());
        log.setPackParam(packParam);
        log.setCreateTime(new Date());
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(log);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(optLogService, Mockito.never()).doStopTest(Mockito.anyInt(), Mockito.any());
    }

    @Test
    void shouldCloseStateForStopType1WhenNotInTestingStatus() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        OptLogService optLogService =
                (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        BatteryReportLog log = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("resistanceTestStatus", ResistanceTestStatusEnum.NOT_TESTING.getCode());
        log.setPackParam(packParam);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(log);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._1.getDictValue());
    }

    @Test
    void shouldCloseStateForStopType1WhenReportStale() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        OptLogService optLogService =
                (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        BatteryReportLog log = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("resistanceTestStatus", ResistanceTestStatusEnum.TESTING.getCode());
        log.setPackParam(packParam);
        log.setCreateTime(new Date(System.currentTimeMillis() - 5 * 60 * 1000));
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(log);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._1.getDictValue());
    }

    @Test
    void shouldStopType5ViaLegacyGenCmd30AndDoStopTest() {
        ControlBattery service = service(true);
        CmdBatteryControlService cmdService =
                (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        OptLogService optLogService =
                (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        Mockito.when(cmdService.genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble()))
                .thenReturn("mock-stop-cmd");

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._5.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._5.getDictValue());
    }

    @Test
    void shouldReturnErrorForStopType3WhenBlankCommand() {
        ControlBattery service = service(true);
        CmdBatteryControlService cmdService =
                (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        Mockito.when(cmdService.genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble()))
                .thenReturn("");

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._3.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("指令生成失败"));
    }

    @Test
    void shouldVerifyMarkStoppedCalledForLegacyInternalResistanceStop() {
        ControlBattery service = service(true);
        BatteryModeStatusService modeStatusService =
                (BatteryModeStatusService) ReflectionTestUtils.getField(service, "batteryModeStatusService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(null);

        service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Mockito.verify(modeStatusService).markStopped(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 1, true);
    }

    // ---- getConfig error tests ----

    @Test
    void shouldThrowWhenConfigNull() {
        ControlBattery service = service(true);
        IConfigService configService =
                (IConfigService) ReflectionTestUtils.getField(service, "configService");
        Mockito.when(configService.selectDefaultConfig()).thenReturn(null);

        Assertions.assertThrows(ServiceException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue())));
    }

    @Test
    void shouldThrowWhenConfigNotBatteryType() {
        ControlBattery service = service(true);
        IConfigService configService =
                (IConfigService) ReflectionTestUtils.getField(service, "configService");
        Config nonBattery = new Config();
        nonBattery.setConfigId(1L);
        nonBattery.setType(2);
        Mockito.when(configService.selectDefaultConfig()).thenReturn(nonBattery);

        Assertions.assertThrows(ServiceException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue())));
    }

    @Test
    void shouldThrowWhenBatteryPackNull() {
        ControlBattery service = service(true);
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(null);

        Assertions.assertThrows(ServiceException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue())));
    }

    @Test
    void shouldThrowWhenType5AndBatteryNotAllowPower() {
        ControlBattery service = service(true);
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        BatteryPack pack = new BatteryPack();
        pack.setPackNum(1);
        pack.setIsAllowPower(1);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);

        Assertions.assertThrows(ServiceException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._5.getDictValue())));
    }

    // ---- utility method tests ----

    @Test
    void shouldRejectUnsupportedCommandType99() {
        ControlBattery service = service(true);

        AjaxResult result = service.executeBatteryOpt(request(BatteryTestEnum._99.getDictValue()), BatteryOptExecuteType.MANUAL);

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void shouldFallbackWhenBuildReportLogThrowsException() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLogService reportLogService =
                (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        Mockito.when(adapterService.buildReportLog(1)).thenThrow(new RuntimeException("adapter error"));
        Mockito.when(reportLogService.lastCache(1)).thenReturn(null);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals("暂无上报数据", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(reportLogService).lastCache(1);
    }

    @Test
    void shouldPassWhenNoAlarmInValidateBeforeCommand() {
        ControlBattery service = service(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        IAlarmLogService alarmLogService =
                (IAlarmLogService) ReflectionTestUtils.getField(service, "alarmLogService");
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(BatteryPackStatusEnum.IDLE.getCode()));
        Mockito.when(alarmLogService.getByCache(Mockito.eq(1), Mockito.isNull(), Mockito.anyString()))
                .thenReturn(null);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals("当前测试类型未完成新链路执行，禁止回退旧M460指令", result.get(AjaxResult.MSG_TAG));
    }

    private ControlBattery service(boolean realtimeEnabled) {
        ControlBattery service = new ControlBattery();

        IConfigService configService = Mockito.mock(IConfigService.class);
        Mockito.when(configService.selectDefaultConfig()).thenReturn(config());
        ReflectionTestUtils.setField(service, "configService", configService);

        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(batteryPack());
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);

        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeEnabled);
        ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);

        ReflectionTestUtils.setField(service, "batteryModuleReportLogAdapterService",
                Mockito.mock(BatteryModuleReportLogAdapterService.class));
        ReflectionTestUtils.setField(service, "batteryReportLogService",
                Mockito.mock(BatteryReportLogService.class));
        ReflectionTestUtils.setField(service, "alarmLogService",
                Mockito.mock(IAlarmLogService.class));

        ReflectionTestUtils.setField(service, "optLogService",
                Mockito.mock(OptLogService.class));
        ReflectionTestUtils.setField(service, "batteryOptCollectorCommandAdapter",
                Mockito.mock(BatteryOptCollectorCommandAdapter.class));
        ReflectionTestUtils.setField(service, "batteryOptCapacityModuleCommandAdapter",
                Mockito.mock(BatteryOptCapacityModuleCommandAdapter.class));
        ReflectionTestUtils.setField(service, "batteryModeStatusService",
                Mockito.mock(BatteryModeStatusService.class));
        CmdBatteryControlService cmdService = Mockito.mock(CmdBatteryControlService.class);
        ReflectionTestUtils.setField(service, "cmdBatteryControlService", cmdService);
        return service;
    }

    private Config config() {
        Config config = new Config();
        config.setConfigId(1L);
        config.setType(DeviceTypeEnum._1.getDictValue());
        return config;
    }

    private BatteryPack batteryPack() {
        BatteryPack batteryPack = new BatteryPack();
        batteryPack.setPackNum(1);
        return batteryPack;
    }

    private DevBatteryOpt request(Integer testType) {
        DevBatteryOpt request = new DevBatteryOpt();
        request.setPackNum(1);
        request.setTestType(testType);
        return request;
    }

    private BatteryReportLog reportLog(String batteryPackStatus) {
        BatteryReportLog reportLog = new BatteryReportLog();
        Map<String, Object> packParam = new HashMap<>();
        packParam.put("batteryPackStatus", batteryPackStatus);
        reportLog.setPackParam(packParam);
        return reportLog;
    }

    // ---- TASK-AI-VERIFY-STOP-003: stop routing tests ----

    @Test
    void shouldRouteStopForLegacyInternalResistanceToLocalPath() {
        ControlBattery service = service(true);
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        OptLogService optLogService = (OptLogService) ReflectionTestUtils.getField(service, "optLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        Mockito.when(commandAdapter.tryStop(Mockito.any())).thenReturn(null);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(null);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandAdapter).tryStop(Mockito.any());
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._1.getDictValue());
    }

    @Test
    void shouldRouteStopForBackupToLegacyGenCmd30Path() {
        ControlBattery service = service(true);
        BatteryOptCollectorCommandAdapter commandAdapter =
                (BatteryOptCollectorCommandAdapter) ReflectionTestUtils.getField(service, "batteryOptCollectorCommandAdapter");
        CmdBatteryControlService cmdService = (CmdBatteryControlService) ReflectionTestUtils.getField(service, "cmdBatteryControlService");
        Mockito.when(cmdService.genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble()))
                .thenReturn("mock-cmd");

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._3.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandAdapter, Mockito.never()).tryStop(Mockito.any());
        Mockito.verify(cmdService).genCmd30(Mockito.any(), Mockito.anyInt(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyDouble());
    }
}
