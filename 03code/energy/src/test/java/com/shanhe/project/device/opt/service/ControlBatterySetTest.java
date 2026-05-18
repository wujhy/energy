package com.shanhe.project.device.opt.service;

import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ControlBatterySetTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBalancedStatusLocally() {
        IHostService hostService = Mockito.mock(IHostService.class);

        Map<String, Object> extend = new HashMap<>();
        extend.put("buzzerStatus", 1);
        Mockito.when(hostService.getExtend()).thenReturn(extend);

        ControlBatterySet service = new ControlBatterySet();
        ReflectionTestUtils.setField(service, "hostService", hostService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);
        request.setAutoBalanced(1);
        request.setManualBalanced(0);

        AjaxResult result = service.balanced(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Assertions.assertEquals(10L, request.getConfigId());
        Mockito.verify(hostService).updateExtend(captor.capture());
        Assertions.assertEquals(1, captor.getValue().get("autoBalanced"));
        Assertions.assertEquals(0, captor.getValue().get("manualBalanced"));
        Assertions.assertEquals(1, captor.getValue().get("buzzerStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBuzzerStatusLocally() {
        IHostService hostService = Mockito.mock(IHostService.class);
        Mockito.when(hostService.getExtend()).thenReturn(null);

        ControlBatterySet service = new ControlBatterySet();
        ReflectionTestUtils.setField(service, "hostService", hostService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);
        request.setBuzzerStatus(1);

        AjaxResult result = service.buzzerStatus(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Assertions.assertEquals(10L, request.getConfigId());
        Mockito.verify(hostService).updateExtend(captor.capture());
        Assertions.assertEquals(1, captor.getValue().get("buzzerStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBuzzerStatusWithoutMutatingConfigId() {
        IHostService hostService = Mockito.mock(IHostService.class);
        Mockito.when(hostService.getExtend()).thenReturn(null);

        ControlBatterySet service = new ControlBatterySet();
        ReflectionTestUtils.setField(service, "hostService", hostService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(null);
        request.setBuzzerStatus(0);

        AjaxResult result = service.buzzerStatus(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertNull(request.getConfigId());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(hostService).updateExtend(captor.capture());
        Assertions.assertEquals(0, captor.getValue().get("buzzerStatus"));
    }

    @Test
    void shouldResolveCollectorChannelByGroupWithoutMutatingConfigId() {
        BatteryCollectorCommandService commandService = Mockito.mock(BatteryCollectorCommandService.class);
        Mockito.when(commandService.resolveChannelName(null, 1)).thenReturn("battery-group-1");
        Mockito.when(commandService.manualSetSubmoduleAddress("battery-group-1", 1, 8, 9, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(true)
                        .channelName("battery-group-1")
                        .build());

        ControlBatterySet service = new ControlBatterySet();
        ReflectionTestUtils.setField(service, "batteryCollectorCommandService", commandService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(null);
        request.setPackNum(1);
        request.setModelNum(8);
        request.setNewModelNum(9);

        AjaxResult result = service.manualModelNum(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertNull(request.getConfigId());
        Mockito.verify(commandService).resolveChannelName(null, 1);
        Mockito.verify(commandService).manualSetSubmoduleAddress("battery-group-1", 1, 8, 9, null);
    }

    @Test
    void shouldClearHostByLocalEnergyRestore() {
        IHostService hostService = Mockito.mock(IHostService.class);

        ControlBatterySet service = new ControlBatterySet();
        ReflectionTestUtils.setField(service, "hostService", hostService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);

        AjaxResult result = service.delHost(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals(10L, request.getConfigId());
        Mockito.verify(hostService).restore();
    }

    @Test
    void shouldReserveUnconfirmedM460ActionsWithoutProtocolPassthrough() {
        ControlBatterySet service = new ControlBatterySet();

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);
        request.setPackNum(1);

        AjaxResult delGbResult = service.delGb(request);
        AjaxResult resetResult = service.reset(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), delGbResult.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), resetResult.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals(10L, request.getConfigId());
    }

    @Test
    void shouldRequireBuzzerStatusButNotConfigIdForBuzzerValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        BatterySetVO request = new BatterySetVO();

        Set<ConstraintViolation<BatterySetVO>> violations = validator.validate(request, BatterySetVO.cmd39.class);

        Assertions.assertEquals(1, violations.size());
        request.setBuzzerStatus(1);
        Assertions.assertTrue(validator.validate(request, BatterySetVO.cmd39.class).isEmpty());
    }
}
