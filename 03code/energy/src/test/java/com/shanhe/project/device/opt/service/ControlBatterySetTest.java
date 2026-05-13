package com.shanhe.project.device.opt.service;

import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.device.config.service.IConfigService;
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
        IConfigService configService = Mockito.mock(IConfigService.class);

        Map<String, Object> extend = new HashMap<>();
        extend.put("buzzerStatus", 1);
        Mockito.when(configService.getExtend()).thenReturn(extend);

        ControlBatterySet service = new ControlBatterySet();
        service.configService = configService;

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);
        request.setAutoBalanced(1);
        request.setManualBalanced(0);

        AjaxResult result = service.balanced(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Assertions.assertEquals(1L, request.getConfigId());
        Mockito.verify(configService).updateExtend(captor.capture());
        Assertions.assertEquals(1, captor.getValue().get("autoBalanced"));
        Assertions.assertEquals(0, captor.getValue().get("manualBalanced"));
        Assertions.assertEquals(1, captor.getValue().get("buzzerStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBuzzerStatusLocally() {
        IConfigService configService = Mockito.mock(IConfigService.class);
        Mockito.when(configService.getExtend()).thenReturn(null);

        ControlBatterySet service = new ControlBatterySet();
        service.configService = configService;

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(10L);
        request.setBuzzerStatus(1);

        AjaxResult result = service.buzzerStatus(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Assertions.assertEquals(1L, request.getConfigId());
        Mockito.verify(configService).updateExtend(captor.capture());
        Assertions.assertEquals(1, captor.getValue().get("buzzerStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveBuzzerStatusWithDefaultConfigWhenConfigIdBlank() {
        IConfigService configService = Mockito.mock(IConfigService.class);
        Mockito.when(configService.getExtend()).thenReturn(null);

        ControlBatterySet service = new ControlBatterySet();
        service.configService = configService;

        BatterySetVO request = new BatterySetVO();
        request.setBuzzerStatus(0);

        AjaxResult result = service.buzzerStatus(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals(1L, request.getConfigId());
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(configService).updateExtend(captor.capture());
        Assertions.assertEquals(0, captor.getValue().get("buzzerStatus"));
    }

    @Test
    void shouldResolveCollectorChannelByGroupWhenConfigIdDefaults() {
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
        request.setPackNum(1);
        request.setModelNum(8);
        request.setNewModelNum(9);

        AjaxResult result = service.manualModelNum(request);

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals(1L, request.getConfigId());
        Mockito.verify(commandService).resolveChannelName(null, 1);
        Mockito.verify(commandService).manualSetSubmoduleAddress("battery-group-1", 1, 8, 9, null);
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
