package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.iot.model.BatteryModeInfo;
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
    void shouldStopConnectResistanceByCollectorCommand() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.when(commandService.stopRunningTest(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, BatteryTestEnum._2.getDictValue()))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(true)
                        .message("stopped")
                        .build());

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.SUCCESS.value(), result.get(AjaxResult.CODE_TAG));
        Mockito.verify(commandService).stopRunningTest(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, BatteryTestEnum._2.getDictValue());
    }

    @Test
    void shouldStopSingleInternalResistanceByCollectorCommand() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.when(commandService.stopRunningTest(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, BatteryTestEnum._6.getDictValue()))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("当前运行测试类型与停止类型不一致")
                        .build());

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._6.getDictValue(), 8));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("当前运行测试类型与停止类型不一致", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(commandService).stopRunningTest(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, BatteryTestEnum._6.getDictValue());
    }

    @Test
    void shouldFallbackStopWhenCollectorCommandSwitchDisabled() {
        BatteryOptCollectorCommandAdapter adapter = adapter(false);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNull(result);
        Mockito.verifyNoInteractions(commandService);
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

    // ---- TASK-AI-VERIFY-STOP-001: stop path gap tests ----

    @Test
    void shouldReturnNullWhenTryStopWithNullOpt() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);

        AjaxResult result = adapter.tryStop(null);

        Assertions.assertNull(result);
    }

    @Test
    void shouldRouteGroupInternalResistanceStopToCollectorMode() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.when(commandService.stopRunningTest(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, BatteryTestEnum._1.getDictValue()))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("当前电池组没有正在执行的测试")
                        .build());

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._1.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("当前电池组没有正在执行的测试", result.get(AjaxResult.MSG_TAG));
        Mockito.verify(commandService).stopRunningTest(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, BatteryTestEnum._1.getDictValue());
    }

    @Test
    void shouldReturnErrorWhenStopRunningTestReturnsNull() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.when(commandService.stopRunningTest(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);

        AjaxResult result = adapter.tryStop(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("停止测试失败", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnNullWhenTryExecuteWithNullOpt() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);

        AjaxResult result = adapter.tryExecute(null);

        Assertions.assertNull(result);
    }

    @Test
    void shouldFallbackGroupInternalResistanceWhenModuleStateMachineNotMapped() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.groupInternalResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder()
                        .success(false)
                        .mappedToModuleCommand(false)
                        .message("整组内阻测试尚未实现等价 600 显式控制状态机，继续使用旧 M460 链路")
                        .build());

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._1.getDictValue(), null));

        Assertions.assertNull(result);
        Mockito.verify(commandService).groupInternalResistanceTest("battery-group-1", 1, 24, null);
    }

    // ---- TASK-AI-VERIFY-EXEC-001: execution entry gap tests ----

    @Test
    void shouldReturnNullWhenTryExecuteWithNullTestType() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);

        AjaxResult result = adapter.tryExecute(opt(null, null));

        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnNullWhenTryExecuteWithNullPackNum() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        DevBatteryOpt opt = new DevBatteryOpt();
        opt.setTestType(BatteryTestEnum._2.getDictValue());
        // don't set packNum

        AjaxResult result = adapter.tryExecute(opt);

        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnErrorWhenSingleInternalResistanceModelNumIsNull() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("单节内阻测试单体编号无效", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    void shouldReturnErrorWhenSingleInternalResistanceModelNumIsZero() {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(adapter, "batteryPackService");
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), 0));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertEquals("单节内阻测试单体编号无效", result.get(AjaxResult.MSG_TAG));
    }

    // ---- TASK-AI-VERIFY-EXEC-002: mutex tests ----

    @Test
    void shouldRejectExecuteWhenSameConnectResistanceModeRunning() {
        BatteryOptCollectorCommandAdapter adapter = adapterWithModeRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("同类型测试运行中"));
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.verify(commandService, Mockito.never())
                .connectResistanceTest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void shouldRejectExecuteWhenSameInternalResistanceModeRunning() {
        BatteryOptCollectorCommandAdapter adapter = adapterWithModeRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), 8));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("同类型测试运行中"));
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.verify(commandService, Mockito.never())
                .singleInternalResistanceTest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void shouldRejectExecuteWhenDifferentModeRunning() {
        BatteryOptCollectorCommandAdapter adapter = adapterWithModeRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);

        AjaxResult result = adapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), result.get(AjaxResult.CODE_TAG));
        Assertions.assertTrue(result.get(AjaxResult.MSG_TAG).toString().contains("其他测试运行中"));
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(adapter, "batteryCollectorCommandService");
        Mockito.verify(commandService, Mockito.never())
                .connectResistanceTest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt(), Mockito.any());
    }

    // ---- TASK-AI-VERIFY-CONFIG-001: module command switch regression ----

    /**
     * CONFIG-001: 开关关闭时 tryExecute 和 tryStop 均返回 null，允许旧 980/CommServer 链路兜底。
     */
    @Test
    void shouldReturnNullForExecuteAndStopWhenSwitchDisabled() {
        BatteryOptCollectorCommandAdapter offAdapter = adapter(false);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(offAdapter, "batteryCollectorCommandService");

        AjaxResult executeResult = offAdapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));
        AjaxResult stopResult = offAdapter.tryStop(opt(BatteryTestEnum._2.getDictValue(), null));

        Assertions.assertNull(executeResult, "开关关闭时 tryExecute 应返回 null 允许旧链路");
        Assertions.assertNull(stopResult, "开关关闭时 tryStop 应返回 null 允许旧链路");
        Mockito.verifyNoInteractions(commandService);
    }

    /**
     * CONFIG-001: 开关开启时所有适配失败路径返回业务错误，不返回 null（不触发旧链路回退）。
     */
    @Test
    void shouldReturnBusinessErrorsForAllFailurePathsWhenSwitchEnabled() {
        BatteryOptCollectorCommandAdapter onAdapter = adapter(true);
        BatteryCollectorCommandService commandService =
                (BatteryCollectorCommandService) ReflectionTestUtils.getField(onAdapter, "batteryCollectorCommandService");
        IBatteryPackService batteryPackService =
                (IBatteryPackService) ReflectionTestUtils.getField(onAdapter, "batteryPackService");

        // 失败路径 1: 通道未找到
        Mockito.when(commandService.resolveChannelName(1)).thenReturn(null);
        AjaxResult noChannel = onAdapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));
        Assertions.assertNotNull(noChannel, "通道缺失不应返回 null");
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), noChannel.get(AjaxResult.CODE_TAG));

        // 失败路径 2: 命令队列返回失败
        Mockito.when(commandService.resolveChannelName(1)).thenReturn("battery-group-1");
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenReturn(BatteryCollectorCommandResult.builder().success(false).message("queue full").build());
        AjaxResult queueFail = onAdapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));
        Assertions.assertNotNull(queueFail, "队列失败不应返回 null");
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), queueFail.get(AjaxResult.CODE_TAG));

        // 失败路径 3: 命令服务抛异常
        Mockito.when(commandService.connectResistanceTest("battery-group-1", 1, 24, null))
                .thenThrow(new IllegalStateException("unexpected"));
        AjaxResult exceptionPath = onAdapter.tryExecute(opt(BatteryTestEnum._2.getDictValue(), null));
        Assertions.assertNotNull(exceptionPath, "异常不应返回 null");
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), exceptionPath.get(AjaxResult.CODE_TAG));

        // 失败路径 4: _6 单体编号无效
        AjaxResult invalidModel = onAdapter.tryExecute(opt(BatteryTestEnum._6.getDictValue(), 999));
        Assertions.assertNotNull(invalidModel, "无效编号不应返回 null");
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), invalidModel.get(AjaxResult.CODE_TAG));

        // 失败路径 5: 停止时 stopRunningTest 返回 null
        Mockito.when(commandService.stopRunningTest(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, BatteryTestEnum._2.getDictValue()))
                .thenReturn(null);
        AjaxResult stopFail = onAdapter.tryStop(opt(BatteryTestEnum._2.getDictValue(), null));
        Assertions.assertNotNull(stopFail, "停止失败不应返回 null");
        Assertions.assertEquals(AjaxResult.Type.ERROR.value(), stopFail.get(AjaxResult.CODE_TAG));
    }

    private BatteryOptCollectorCommandAdapter adapterWithModeRunning(Integer packNum, Integer mode) {
        BatteryOptCollectorCommandAdapter adapter = adapter(true);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        BatteryModeInfo modeInfo = new BatteryModeInfo();
        modeInfo.setPackNum(packNum);
        modeInfo.setMode(mode);
        modeInfo.setStatus(1);
        Mockito.when(modeStatusService.get(packNum)).thenReturn(modeInfo);
        ReflectionTestUtils.setField(adapter, "batteryModeStatusService", modeStatusService);
        return adapter;
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
