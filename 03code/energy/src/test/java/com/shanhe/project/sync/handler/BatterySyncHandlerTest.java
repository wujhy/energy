package com.shanhe.project.sync.handler;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.sync.consts.MethodEnum;
import com.shanhe.project.sync.domain.BatteryOptVo;
import com.shanhe.project.sync.domain.RequestVo;
import com.shanhe.project.sync.domain.ResponseVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatterySyncHandlerTest {

    @Test
    void shouldKeepPlanSyncOnOldControlPathWhenCollectorCommandEnabled() {
        BatterySyncHandler handler = newHandler(true);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        ReflectionTestUtils.setField(handler, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(handler, "batteryCollectorCommandService", commandService);
        Mockito.when(controlBattery.toSendCmdToOat(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.YES.getDictValue(), BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        Mockito.verify(controlBattery).toSendCmdToOat(Mockito.any(DevBatteryOpt.class));
        Mockito.verifyNoInteractions(commandService);
    }

    @Test
    void shouldUseCollectorCommandOnlyForImmediateExecutableCommand() {
        BatterySyncHandler handler = newHandler(true);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        ReflectionTestUtils.setField(handler, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(handler, "batteryCollectorCommandService", commandService);
        Mockito.when(commandService.resolveChannelName(10L, 1)).thenReturn("battery-rs485-1");
        Mockito.when(commandService.connectResistanceTest("battery-rs485-1", 1, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(true)
                        .mappedToModuleCommand(true)
                        .channelName("battery-rs485-1")
                        .commandDefinition(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST)
                        .build());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.NO.getDictValue(), BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        Mockito.verify(commandService).connectResistanceTest("battery-rs485-1", 1, null);
        Mockito.verifyNoInteractions(controlBattery);
    }

    @Test
    void shouldFallbackToOldImmediateControlWhenCollectorCommandIsNotMapped() {
        BatterySyncHandler handler = newHandler(true);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        ReflectionTestUtils.setField(handler, "controlBattery", controlBattery);
        ReflectionTestUtils.setField(handler, "batteryCollectorCommandService", commandService);
        Mockito.when(commandService.resolveChannelName(10L, 1)).thenReturn("battery-rs485-1");
        Mockito.when(controlBattery.toSendBatteryCmdToOat(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.NO.getDictValue(), BatteryTestEnum._6.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(controlBattery).toSendBatteryCmdToOat(captor.capture());
        Assertions.assertEquals(BatteryTestEnum._6.getDictValue(), captor.getValue().getTestType());
    }

    private BatterySyncHandler newHandler(boolean collectorCommandEnabled) {
        BatterySyncHandler handler = new BatterySyncHandler();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpModuleCommandEnabled(collectorCommandEnabled);
        ReflectionTestUtils.setField(handler, "batteryCollectorProperties", properties);
        return handler;
    }

    private RequestVo request(Integer isNow, Integer testType, Integer modelNum) {
        BatteryOptVo optVo = new BatteryOptVo();
        optVo.setDevId(10L);
        optVo.setPackNum(1);
        optVo.setIsNow(isNow);
        optVo.setTestType(testType);
        optVo.setModelNum(modelNum);
        return new RequestVo()
                .setImei("imei")
                .setBusinessId("biz")
                .setMethod(MethodEnum._43.getDictValue())
                .setContent(optVo);
    }
}
