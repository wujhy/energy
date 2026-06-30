package com.shanhe.project.sync.handler;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
import com.shanhe.project.manage.opt.service.BatteryOptExecuteType;
import com.shanhe.project.manage.opt.service.ControlBattery;
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
    void shouldSavePlanSyncWithoutDispatchingCommand() {
        BatterySyncHandler handler = new HandlerBuilder().build();
        ControlBattery controlBattery = (ControlBattery) ReflectionTestUtils.getField(handler, "controlBattery");
        IDevBatteryOptService optService = (IDevBatteryOptService) ReflectionTestUtils.getField(handler, "devBatteryOptService");

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.YES.getDictValue(), BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        Mockito.verify(optService).insertDevBatteryOpt(Mockito.any(DevBatteryOpt.class));
        Mockito.verifyNoInteractions(controlBattery);
    }

    @Test
    void shouldSaveAndExecuteImmediateSyncThroughUnifiedPath() {
        BatterySyncHandler handler = new HandlerBuilder().build();
        ControlBattery controlBattery = (ControlBattery) ReflectionTestUtils.getField(handler, "controlBattery");
        IDevBatteryOptService optService = (IDevBatteryOptService) ReflectionTestUtils.getField(handler, "devBatteryOptService");
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.any(DevBatteryOpt.class), Mockito.eq(BatteryOptExecuteType.SYNC)))
                .thenReturn(AjaxResult.success());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.NO.getDictValue(), BatteryTestEnum._6.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(optService).insertDevBatteryOpt(Mockito.any(DevBatteryOpt.class));
        Mockito.verify(controlBattery).executeBatteryOpt(captor.capture(), Mockito.eq(BatteryOptExecuteType.SYNC));
        Assertions.assertEquals(BatteryTestEnum._6.getDictValue(), captor.getValue().getTestType());
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

    private static class HandlerBuilder {
        BatterySyncHandler build() {
            BatterySyncHandler handler = new BatterySyncHandler();

            ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
            ReflectionTestUtils.setField(handler, "controlBattery", controlBattery);
            ReflectionTestUtils.setField(handler, "devBatteryOptService", Mockito.mock(IDevBatteryOptService.class));
            return handler;
        }
    }
}
