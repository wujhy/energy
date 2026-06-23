package com.shanhe.project.sync.handler;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
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
        BatterySyncHandler handler = new HandlerBuilder().build();
        ControlBattery controlBattery = (ControlBattery) ReflectionTestUtils.getField(handler, "controlBattery");
        Mockito.when(controlBattery.toSendCmdToOat(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.YES.getDictValue(), BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        Mockito.verify(controlBattery).toSendCmdToOat(Mockito.any(DevBatteryOpt.class));
    }

    @Test
    void shouldFallbackToOldImmediateControlWhenCollectorCommandIsNotMapped() {
        BatterySyncHandler handler = new HandlerBuilder().build();
        ControlBattery controlBattery = (ControlBattery) ReflectionTestUtils.getField(handler, "controlBattery");
        Mockito.when(controlBattery.toSendBatteryCmdToOat(Mockito.any(DevBatteryOpt.class)))
                .thenReturn(AjaxResult.success());

        ResponseVo response = handler.syncBatteryOpt(request(YesNoEnum.NO.getDictValue(), BatteryTestEnum._6.getDictValue(), null));

        Assertions.assertEquals(0, response.getCode());
        ArgumentCaptor<DevBatteryOpt> captor = ArgumentCaptor.forClass(DevBatteryOpt.class);
        Mockito.verify(controlBattery).toSendBatteryCmdToOat(captor.capture());
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
            return handler;
        }
    }
}
