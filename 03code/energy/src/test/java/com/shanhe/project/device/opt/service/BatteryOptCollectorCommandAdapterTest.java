package com.shanhe.project.device.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IBatteryPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryOptCollectorCommandAdapterTest {

    @Test
    void shouldExecuteConnectResistanceByCollectorCommand() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(successResult());

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService).connectResistanceTest("battery-group-1", 1, 24, null);
    }

    @Test
    void shouldExecuteSingleInternalResistanceByCollectorCommand() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.singleInternalResistanceTest("battery-group-1", 1, 8, null))
                .thenReturn(successResult());

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), 8));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService).singleInternalResistanceTest("battery-group-1", 1, 8, null);
    }

    @Test
    void shouldRejectSingleInternalResistanceWhenModelNumOutOfRange() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), 25));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService, Mockito.never())
                .singleInternalResistanceTest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void shouldReturnErrorWhenCollectorChannelMissing() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn(null);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("未找到电池组采集通道", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(commandService, Mockito.never())
                .connectResistanceTest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void shouldReturnErrorWhenCollectorCommandQueueFails() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("queue failed")
                        .build());

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("queue failed", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnErrorWhenCollectorCommandThrows() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenThrow(new IllegalStateException("queue unavailable"));

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("独立采集模块命令执行失败", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldFallbackWhenCollectorCommandSwitchDisabled() {
        BatteryOptCollectorCommandAdapter adapter = adapter(false);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNull(result);
        Mockito.verifyNoInteractions(commandService);
    }

    private BatteryOptCollectorCommandAdapter adapter(boolean moduleCommandEnabled) {
        BatteryOptCollectorCommandAdapter adapter = new BatteryOptCollectorCommandAdapter();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpModuleCommandEnabled(moduleCommandEnabled);
        ReflectionTestUtils.setField(adapter, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(adapter, "batteryCollectorCommandService",
                Mockito.mock(BatteryCollectorCommandService.class));
        ReflectionTestUtils.setField(adapter, "batteryPackService", Mockito.mock(IBatteryPackService.class));
        return adapter;
    }

    private BatteryCollectorCommandResult successResult() {
        return BatteryCollectorCommandResult.builder()
                .success(true)
                .message("queued")
                .build();
    }

    private DevBatteryOpt opt(Integer testType, Integer modelNum) {
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(testType);
        opt.setModelNum(modelNum);
        return opt;
    }
}
