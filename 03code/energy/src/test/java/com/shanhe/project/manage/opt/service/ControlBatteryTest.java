package com.shanhe.project.manage.opt.service;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigService;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import com.shanhe.project.manage.opt.domain.OptLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class ControlBatteryTest {

    @Test
    void shouldRejectWhenRealtimeSnapshotMissing() {
        ControlBattery service = service();
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("暂无上报数据", result.get(AjaxResult.MSG_TAG));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldCheckConnectResistanceCurrentBeforeCollectorCommandAdapter() {
        ControlBattery service = service();
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode(), 1D));

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._2.getDictValue())));

        Assertions.assertTrue(exception.getMessage().contains("组电流超过 5A"));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldRejectNonType2WhenBatteryNotIdle() {
        ControlBattery service = service();
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.CAPACITY_TEST.getCode(), 10D));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldRejectWhenActiveAlarmExists() {
        ControlBattery service = service();
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        IAlarmLogService alarmLogService = field(service, "alarmLogService");
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode(), 10D));
        AlarmLog alarmLog = new AlarmLog();
        alarmLog.setStatus(YesNoEnum.NO.getDictValue());
        alarmLog.setDataInfo("通讯异常");
        Mockito.when(alarmLogService.getByCache(1, null, ItemCode.TXZT.getCode())).thenReturn(alarmLog);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("通讯异常", result.get(AjaxResult.MSG_TAG));
        Mockito.verifyNoInteractions(commandAdapter);
    }

    @Test
    void shouldReturnCollectorAdapterResultWhenTryExecuteSucceeds() {
        ControlBattery service = service();
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode(), 10D));
        Mockito.when(commandAdapter.tryExecutePrepared(Mockito.any(BatteryCommandContext.class))).thenReturn(success);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertSame(success, result);
    }

    @Test
    void shouldReturnCapacityAdapterResultWhenTryExecuteSucceeds() {
        ControlBattery service = service();
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        BatteryOptCapacityModuleCommandAdapter commandAdapter = field(service, "batteryOptCapacityModuleCommandAdapter");
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode(), 10D));
        Mockito.when(commandAdapter.tryExecute(Mockito.any(BatteryCommandContext.class))).thenReturn(success);

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._3.getDictValue()));

        Assertions.assertSame(success, result);
    }

    @Test
    void shouldRejectCommandWhenAnyRunningLogExists() {
        ControlBattery service = service();
        OptLogService optLogService = field(service, "optLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = field(service, "realtimeSnapshotService");
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.singletonList(new OptLog()));
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(BatteryPackStatusEnum.IDLE.getCode(), 10D));

        AjaxResult result = service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void shouldThrowWhenConfigNull() {
        ControlBattery service = service();
        IConfigService configService = field(service, "configService");
        Mockito.when(configService.selectDefaultConfig()).thenReturn(null);

        Assertions.assertThrows(ServiceException.class,
                () -> service.toSendBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue())));
    }

    @Test
    void shouldStopCollectorManagedTypeThroughCollectorAdapter() {
        ControlBattery service = service();
        BatteryOptCollectorCommandAdapter commandAdapter = field(service, "batteryOptCollectorCommandAdapter");
        AjaxResult success = AjaxResult.success("ok");
        Mockito.when(commandAdapter.tryStop(Mockito.any())).thenReturn(success);

        AjaxResult result = service.toSendStopBatteryCmdToOat(request(BatteryTestEnum._1.getDictValue()));

        Assertions.assertSame(success, result);
    }

    private ControlBattery service() {
        ControlBattery service = new ControlBattery();

        IConfigService configService = Mockito.mock(IConfigService.class);
        Mockito.when(configService.selectDefaultConfig()).thenReturn(config());
        ReflectionTestUtils.setField(service, "configService", configService);

        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(batteryPack());
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);

        ReflectionTestUtils.setField(service, "realtimeSnapshotService", Mockito.mock(BatteryModuleRealtimeSnapshotService.class));
        ReflectionTestUtils.setField(service, "alarmLogService", Mockito.mock(IAlarmLogService.class));
        ReflectionTestUtils.setField(service, "optLogService", Mockito.mock(OptLogService.class));
        ReflectionTestUtils.setField(service, "batteryOptCollectorCommandAdapter", Mockito.mock(BatteryOptCollectorCommandAdapter.class));
        ReflectionTestUtils.setField(service, "batteryOptCapacityModuleCommandAdapter", Mockito.mock(BatteryOptCapacityModuleCommandAdapter.class));
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
        batteryPack.setBatSinSize(12);
        batteryPack.setIsAllowPower(YesNoEnum.YES.getDictValue());
        return batteryPack;
    }

    private DevBatteryOpt request(Integer testType) {
        DevBatteryOpt request = new DevBatteryOpt();
        request.setPackNum(1);
        request.setTestType(testType);
        return request;
    }

    private BatteryModuleRealtimeSnapshot snapshot(String status, Double current) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setBatteryPackStatus(Integer.valueOf(status));
        group.setPackCurrent(current);
        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group)
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> T field(ControlBattery service, String name) {
        return (T) ReflectionTestUtils.getField(service, name);
    }
}