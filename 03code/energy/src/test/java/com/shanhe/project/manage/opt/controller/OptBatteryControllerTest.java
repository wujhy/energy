package com.shanhe.project.manage.opt.controller;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IDevBatteryOptService;
import com.shanhe.project.manage.opt.service.BatteryOptExecuteType;
import com.shanhe.project.manage.opt.service.ControlBattery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class OptBatteryControllerTest {

    @Test
    void doCmdOptBatteryTestShouldNotPersistPlanParameters() {
        OptBatteryController controller = new OptBatteryController();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        DevBatteryOpt request = request(BatteryTestEnum._2.getDictValue());
        Mockito.when(controlBattery.executeBatteryOpt(Mockito.same(request), Mockito.eq(BatteryOptExecuteType.MANUAL)))
                .thenReturn(AjaxResult.success());
        ReflectionTestUtils.setField(controller, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(controller, "controlBattery", controlBattery);

        AjaxResult result = controller.doCmdOptBatteryTest(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verifyNoInteractions(optService);
        Mockito.verify(controlBattery).executeBatteryOpt(Mockito.same(request), Mockito.eq(BatteryOptExecuteType.MANUAL));
    }

    @Test
    void editShouldPersistPlanParameters() {
        OptBatteryController controller = new OptBatteryController();
        IDevBatteryOptService optService = Mockito.mock(IDevBatteryOptService.class);
        ControlBattery controlBattery = Mockito.mock(ControlBattery.class);
        DevBatteryOpt request = request(BatteryTestEnum._2.getDictValue());
        ReflectionTestUtils.setField(controller, "devBatteryOptService", optService);
        ReflectionTestUtils.setField(controller, "controlBattery", controlBattery);

        AjaxResult result = controller.edit(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(optService).insertDevBatteryOpt(request);
        Mockito.verifyNoInteractions(controlBattery);
    }

    private DevBatteryOpt request(Integer testType) {
        DevBatteryOpt request = new DevBatteryOpt();
        request.setPackNum(1);
        request.setTestType(testType);
        return request;
    }
}