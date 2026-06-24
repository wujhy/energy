package com.shanhe.project.device.opt.service;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.cmd.CmdBatteryControlService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

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
    void shouldRejectUnsupportedScheduleCommandTypeBeforePersist() {
        ControlBattery service = service(true);
        IConfigService configService = (IConfigService) ReflectionTestUtils.getField(service, "configService");
        IDevBatteryOptService optService = (IDevBatteryOptService) ReflectionTestUtils.getField(service, "devBatteryOptService");

        AjaxResult result = service.toSendCmdToOat(request(12345));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verifyNoInteractions(optService);
        Mockito.verify(configService, Mockito.never()).selectDefaultConfig();
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
        ReflectionTestUtils.setField(service, "controlBatterySet",
                controlBatterySet());
        ReflectionTestUtils.setField(service, "optLogService",
                Mockito.mock(OptLogService.class));
        ReflectionTestUtils.setField(service, "devBatteryOptService",
                Mockito.mock(IDevBatteryOptService.class));

        ReflectionTestUtils.setField(service, "batteryOptCollectorCommandAdapter",
                Mockito.mock(BatteryOptCollectorCommandAdapter.class));

        CmdBatteryControlService cmdService = Mockito.mock(CmdBatteryControlService.class);
        Mockito.when(cmdService.genCmd05(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn("");
        ReflectionTestUtils.setField(service, "cmdBatteryControlService", cmdService);
        return service;
    }

    private ControlBatterySet controlBatterySet() {
        ControlBatterySet controlBatterySet = Mockito.mock(ControlBatterySet.class);
        BatteryModeInfo idle = new BatteryModeInfo();
        idle.setMode(0);
        idle.setStatus(0);
        Mockito.when(controlBatterySet.getModelResult(1)).thenReturn(idle);
        return controlBatterySet;
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
}
