package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryOptCollectorCommandAdapterTest {

    @Test
    void shouldExecuteConnectResistanceByPreparedContext() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(successResult());

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._2, null, 24));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService).connectResistanceTest("battery-group-1", 1, 24, null);
    }

    @Test
    void shouldExecuteSingleInternalResistanceByPreparedContext() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.singleInternalResistanceTest("battery-group-1", 1, 8, null))
                .thenReturn(successResult());

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._6, 8, 24));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService).singleInternalResistanceTest("battery-group-1", 1, 8, null);
    }

    @Test
    void shouldReturnErrorWhenGroupInternalResistanceNotMapped() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.groupInternalResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .mappedToModuleCommand(false)
                        .message("整组内阻测试尚未映射600模块命令")
                        .build());

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._1, null, 24));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("整组内阻测试尚未映射600模块命令", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnErrorWhenPreparedContextChannelMissing() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        // 覆盖默认 mock，使 resolveChannelName 返回 null 模拟通道未找到
        Mockito.when(commandService.resolveChannelName(1)).thenReturn(null);
        Mockito.when(commandService.connectResistanceTest(null, 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("未找到电池组采集通道")
                        .build());

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._2, null, 24));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("未找到电池组采集通道", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(commandService).connectResistanceTest(null, 1, 24, null);
    }

    @Test
    void shouldReturnErrorWhenModuleCommandDisabled() {
        BatteryOptCollectorCommandAdapter adapter = adapter(false);

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._2, null, 24));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("独立采集模块命令未启用，已迁移测试类型禁止回退旧 M460 指令", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnErrorWhenCollectorCommandQueueFails() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("队列加入失败")
                        .build());

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._2, null, 24));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("队列加入失败", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnErrorWhenCollectorCommandThrowsException() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenThrow(new RuntimeException("队列不可用"));

        AjaxResult result = adapter.tryExecutePrepared(context(BatteryTestEnum._2, null, 24));

        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("独立采集模块命令执行失败", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldStopCollectorManagedTest() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService = commandService(adapter);
        Mockito.when(commandService.stopRunningTest(1,
                        BatteryModeStatusService.MODE_CONNECT_RESISTANCE,
                        BatteryTestEnum._2.getDictValue()))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(true)
                        .message("测试停止成功")
                        .build());

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._2, null));

        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
    }

    @Test
    void shouldReturnNullWhenStopTypeUnsupported() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._3, null));

        Assertions.assertNull(result);
    }

    private BatteryOptCollectorCommandAdapter adapter(boolean moduleCommandEnabled) {
        BatteryOptCollectorCommandAdapter adapter = new BatteryOptCollectorCommandAdapter();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpModuleCommandEnabled(moduleCommandEnabled);
        BatteryCollectorCommandService mockService = Mockito.mock(BatteryCollectorCommandService.class);
        Mockito.when(mockService.resolveChannelName(1)).thenReturn("battery-group-1");
        ReflectionTestUtils.setField(adapter, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(adapter, "batteryCollectorCommandService", mockService);
        return adapter;
    }

    private BatteryCollectorCommandService commandService(BatteryOptCollectorCommandAdapter adapter) {
        return (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
    }

    private BatteryCollectorCommandResult successResult() {
        return BatteryCollectorCommandResult.builder()
                .success(true)
                .mappedToModuleCommand(true)
                .message("已加入队列")
                .build();
    }

    private BatteryCommandContext context(BatteryTestEnum testEnum,
                                          Integer modelNum,
                                          int batteryCount) {
        DevBatteryOpt opt = opt(testEnum, modelNum);
        return new BatteryCommandContext(
                opt,
                testEnum,
                BatteryOptExecuteType.MANUAL,
                null,
                null,
                batteryCount,
                null);
    }

    private DevBatteryOpt opt(BatteryTestEnum testEnum, Integer modelNum) {
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setPackNum(1);
        opt.setTestType(testEnum == null ? null : testEnum.getDictValue());
        opt.setModelNum(modelNum);
        return opt;
    }
}
