package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.command.BatteryCollectorCommandQueueService;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.device.opt.mapper.OptLogMapper;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BatteryCollectorCommandServiceTest {

    private final BatteryCollectorCommandService service = new BatteryCollectorCommandService();

    private BatteryModeStatusService newModeStatusService() {
        BatteryModeStatusService modeStatusService = new BatteryModeStatusService();
        ReflectionTestUtils.setField(modeStatusService, "cacheAccessor", new TestCacheAccessor());
        return modeStatusService;
    }

    private BatteryCollectorService newCollectorService(BatteryModeStatusService modeStatusService) {
        BatteryCollectorService collectorService = new BatteryCollectorService();
        ReflectionTestUtils.setField(collectorService, "batteryModeStatusService", modeStatusService);
        BatteryCollectorCommandLogService commandLogService = new BatteryCollectorCommandLogService();
        ReflectionTestUtils.setField(commandLogService, "optLogMapper", Mockito.mock(OptLogMapper.class));
        ReflectionTestUtils.setField(collectorService, "commandLogService", commandLogService);
        return collectorService;
    }

    private static class TestCacheAccessor implements BatteryModeStatusService.CacheAccessor {
        private final Map<String, Object> cache = new HashMap<>();

        @Override
        public Object get(String cacheName, String key) {
            return cache.get(cacheName + ":" + key);
        }

        @Override
        public void put(String cacheName, String key, Object value) {
            cache.put(cacheName + ":" + key, value);
        }

        @Override
        public void remove(String cacheName, String key) {
            cache.remove(cacheName + ":" + key);
        }
    }

    @Test
    void shouldBlockAggregateCommandOnModuleChannel() {
        BatteryCollectorCommandResult result = service.execute(
                BatteryAggregateCommandDefinition.GET_BATTERY_GROUP_INFO,
                "battery-rs485-1",
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.isTimeout());
        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertEquals("battery-rs485-1", result.getChannelName());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.GET_BATTERY_GROUP_INFO, result.getCommandDefinition());
        Assertions.assertTrue(result.getMessage().contains("不能直接发送"));
    }

    @Test
    void shouldMapSingleResistanceTestToModuleCommand() {
        BatteryCollectorCommandResult result = service.singleInternalResistanceTest(
                "battery-rs485-1",
                1,
                8,
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertNotNull(result.getModuleControlCommand());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(8, result.getModuleControlCommand().getAddress());
    }

    @Test
    void shouldMapConnectResistanceTestToBroadcastModuleCommand() {
        BatteryCollectorCommandResult result = service.connectResistanceTest("battery-rs485-1", 1, 24, 1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
        Assertions.assertEquals(1, result.getModuleControlCommand().getConnectResistanceNextAddress());
        Assertions.assertEquals(24, result.getModuleControlCommand().getConnectResistanceMaxAddress());
        Assertions.assertEquals(1, result.getModuleControlCommand().getBatteryGroup());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE,
                result.getModuleControlCommand().getMode());
        Assertions.assertEquals(BatteryTestEnum._2.getDictValue(), result.getModuleControlCommand().getOptLogType());
    }

    @Test
    void shouldRejectConnectResistanceTestWhenCountInvalid() {
        BatteryCollectorCommandResult zeroCount = service.connectResistanceTest("battery-rs485-1", 1, 0, 1000L);
        BatteryCollectorCommandResult negativeCount = service.connectResistanceTest("battery-rs485-1", 1, -1, 1000L);
        BatteryCollectorCommandResult tooLargeCount = service.connectResistanceTest("battery-rs485-1", 1, 246, 1000L);

        Assertions.assertFalse(zeroCount.isSuccess());
        Assertions.assertFalse(zeroCount.isMappedToModuleCommand());
        Assertions.assertNull(zeroCount.getModuleControlCommand());
        Assertions.assertFalse(negativeCount.isSuccess());
        Assertions.assertFalse(negativeCount.isMappedToModuleCommand());
        Assertions.assertNull(negativeCount.getModuleControlCommand());
        Assertions.assertFalse(tooLargeCount.isSuccess());
        Assertions.assertFalse(tooLargeCount.isMappedToModuleCommand());
        Assertions.assertNull(tooLargeCount.getModuleControlCommand());
    }

    @Test
    void shouldRejectConnectResistanceTestWhenChannelOrGroupInvalid() {
        BatteryCollectorCommandResult blankChannel = service.connectResistanceTest(" ", 1, 24, 1000L);
        BatteryCollectorCommandResult invalidGroup = service.connectResistanceTest("battery-rs485-1", 0, 24, 1000L);

        Assertions.assertFalse(blankChannel.isSuccess());
        Assertions.assertNull(blankChannel.getModuleControlCommand());
        Assertions.assertTrue(blankChannel.getMessage().contains("通道名称不能为空"));
        Assertions.assertFalse(invalidGroup.isSuccess());
        Assertions.assertNull(invalidGroup.getModuleControlCommand());
        Assertions.assertTrue(invalidGroup.getMessage().contains("电池组编号无效"));
    }

    @Test
    void shouldMapClearBatteryGroupDebugDataToBroadcastModuleCommand() {
        BatteryCollectorCommandResult result = service.clearBatteryGroupDebugData("battery-rs485-1", 1, 1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.CLEAR_INDIVIDUAL_DEBUGGING_DATA,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertEquals(1, result.getModuleControlCommand().getBatteryGroup());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0x0F}, result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldMapAutomaticModuleAddressFromBatteryPackConfig() {
        BatteryCollectorCommandResult result = service.autoSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                24,
                2,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(246, result.getModuleControlCommand().getAddress());
        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM,
                result.getModuleControlCommand().getMode());
        Assertions.assertEquals(24, result.getModuleControlCommand().getAutoAddressBatteryCount());
        Assertions.assertEquals(2, result.getModuleControlCommand().getAutoAddressBatterySpecification());
        Assertions.assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 1},
                result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldRejectAutomaticModuleAddressWhenSpecificationUnsupported() {
        BatteryCollectorCommandResult result = service.autoSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                24,
                7,
                1000L);

        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
    }

    @Test
    void shouldRejectAutomaticModuleAddressWhenChannelOrGroupInvalid() {
        BatteryCollectorCommandResult blankChannel = service.autoSetSubmoduleAddress(
                " ",
                1,
                24,
                2,
                1000L);
        BatteryCollectorCommandResult invalidGroup = service.autoSetSubmoduleAddress(
                "battery-rs485-1",
                0,
                24,
                2,
                1000L);

        Assertions.assertFalse(blankChannel.isSuccess());
        Assertions.assertFalse(blankChannel.isMappedToModuleCommand());
        Assertions.assertNull(blankChannel.getModuleControlCommand());
        Assertions.assertFalse(invalidGroup.isSuccess());
        Assertions.assertFalse(invalidGroup.isMappedToModuleCommand());
        Assertions.assertNull(invalidGroup.getModuleControlCommand());
    }

    @Test
    void shouldMapManualModuleAddressToModuleCommand() {
        BatteryCollectorCommandResult result = service.manualSetSubmoduleAddress(
                "battery-rs485-1",
                1,
                8,
                9,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.SET_SUBMODULE_ID, result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(8, result.getModuleControlCommand().getAddress());
        Assertions.assertArrayEquals(new byte[]{9}, result.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldRejectBalanceWhenWorkModeAlreadyRunning() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.singleBatteryBalance(
                "battery-rs485-1",
                1,
                8,
                1,
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("其他测试运行中"));
    }

    @Test
    void shouldAllowBalanceWhenOtherPackWorkModeAlreadyRunning() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.singleBatteryBalance(
                "battery-rs485-2",
                2,
                8,
                1,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getModuleControlCommand());
        Assertions.assertEquals(2, result.getModuleControlCommand().getBatteryGroup());
        Assertions.assertEquals(BatteryModeStatusService.MODE_BALANCE, result.getModuleControlCommand().getMode());
    }

    @Test
    void shouldAllowBalanceWhenSyncedModeIsStopped() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryModeInfo stoppedMode = new BatteryModeInfo();
        stoppedMode.setPackNum(1);
        stoppedMode.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        stoppedMode.setStatus(0);
        modeStatusService.putFromM460(stoppedMode);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.singleBatteryBalance(
                "battery-rs485-1",
                1,
                8,
                1,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getModuleControlCommand());
        Assertions.assertEquals(BatteryModeStatusService.MODE_BALANCE, result.getModuleControlCommand().getMode());
    }

    @Test
    void shouldRejectBalanceWhenSyncedStatusIsRunningEvenIfModeIdle() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryModeInfo runningIdleMode = new BatteryModeInfo();
        runningIdleMode.setPackNum(1);
        runningIdleMode.setMode(BatteryModeStatusService.MODE_IDLE);
        runningIdleMode.setStatus(1);
        modeStatusService.putFromM460(runningIdleMode);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.singleBatteryBalance(
                "battery-rs485-1",
                1,
                8,
                1,
                1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("其他测试运行中"));
    }

    @Test
    void shouldMapInternalResistanceCoefficientWithM460FloatPayload() {
        BatteryCollectorCommandResult result = service.setInternalResistanceCoefficient(
                "battery-rs485-1",
                1,
                0,
                1000,
                1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT,
                result.getCommandDefinition());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT,
                result.getModuleControlCommand().getProtocolCode());
        Assertions.assertEquals(0, result.getModuleControlCommand().getAddress());
        Assertions.assertNull(result.getModuleControlCommand().getResponseCode());
        Assertions.assertArrayEquals(new byte[]{0x00, 0x00, (byte) 0x80, 0x3F},
                result.getModuleControlCommand().getPayload());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueueMappedModuleCommandWhenCollectorChannelExists() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorService collectorService = newCollectorService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
        ReflectionTestUtils.setField(service, "collectorService", collectorService);

        BatteryCollectorCommandResult result = service.singleInternalResistanceTest(
                "battery-rs485-1",
                1,
                8,
                1000L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                state.getQueuedModuleCommands().peek().getProtocolCode());
        Assertions.assertEquals(BatteryTestEnum._6.getDictValue(),
                state.getQueuedModuleCommands().peek().getOptLogType());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getRequestCode(),
                result.getRequestCode());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getResponseCode(),
                result.getResponseCode());
        Assertions.assertTrue(result.getMessage().contains("加入串口下发队列"));
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(1, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(8, modeInfo.getAddress());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldQueueConnectResistanceTestWhenCollectorChannelExists() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorService collectorService = newCollectorService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
        ReflectionTestUtils.setField(service, "collectorService", collectorService);

        BatteryCollectorCommandResult result = service.connectResistanceTest(
                "battery-rs485-1",
                1,
                24,
                1000L);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        Assertions.assertEquals(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST,
                state.getQueuedModuleCommands().peek().getProtocolCode());
        Assertions.assertEquals(BatteryTestEnum._2.getDictValue(),
                state.getQueuedModuleCommands().peek().getOptLogType());
        Assertions.assertEquals(1, state.getQueuedModuleCommands().peek().getConnectResistanceNextAddress());
        Assertions.assertEquals(24, state.getQueuedModuleCommands().peek().getConnectResistanceMaxAddress());
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(1, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(0, modeInfo.getAddress());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopRunningTestAndCancelQueuedSameModeCommands() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorService collectorService = newCollectorService(modeStatusService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
        ReflectionTestUtils.setField(service, "collectorService", collectorService);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 0);
        state.getQueuedModuleCommands().add(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST)
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE)
                .build());
        state.getQueuedModuleCommands().add(BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .batteryGroup(2)
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .build());

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("已取消未下发命令1条"));
        Assertions.assertEquals(1, state.getQueuedModuleCommands().size());
        Assertions.assertEquals(2, state.getQueuedModuleCommands().peek().getBatteryGroup());
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._2.getDictValue());
    }

    @Test
    void shouldRejectStopRunningTestWhenNoRunningMode() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("未停止，原因是当前电池组没有正在执行的测试"));
    }

    @Test
    void shouldRejectStopRunningTestWhenPackNumOrModeInvalid() {
        BatteryCollectorCommandResult nullPack = service.stopRunningTest(
                null, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        BatteryCollectorCommandResult zeroPack = service.stopRunningTest(
                0, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        BatteryCollectorCommandResult nullMode = service.stopRunningTest(1, null);

        Assertions.assertFalse(nullPack.isSuccess());
        Assertions.assertTrue(nullPack.getMessage().contains("未停止，原因是电池组编号无效"));
        Assertions.assertFalse(zeroPack.isSuccess());
        Assertions.assertTrue(zeroPack.getMessage().contains("未停止，原因是电池组编号无效"));
        Assertions.assertFalse(nullMode.isSuccess());
        Assertions.assertTrue(nullMode.getMessage().contains("未停止，原因是测试类型不支持停止"));
    }

    @Test
    void shouldRejectStopRunningTestWhenCachedRunningPackDifferent() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 0);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("未停止，原因是当前电池组没有正在执行的测试"));
        BatteryModeInfo modeInfo = modeStatusService.get(2);
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(2, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getMode());
    }
    @Test
    void shouldRejectStopRunningTestWhenModeDifferent() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("未停止，原因是当前运行测试类型与停止类型不一致"));
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
    }
    @Test
    void shouldKeepAmbiguousSubmoduleIdUnsupported() {
        BatteryCollectorCommandResult result = service.execute(
                BatteryAggregateCommandDefinition.SET_DEVICE_ID,
                "battery-rs485-1",
                1000L,
                8);

        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("不能直接发送"));
    }

    @Test
    void shouldKeepAggregateCommandsWithInsufficientContextUnsupported() {
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.SET_SYSTEM_STATE,
                "battery-rs485-1",
                1000L,
                1, 1).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.CLEAR_HOST_DEBUGGING_DATA,
                "battery-rs485-1",
                1000L).isMappedToModuleCommand());
        Assertions.assertFalse(service.execute(
                BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT,
                "battery-rs485-1",
                1000L,
                1, 1, 0x03, 0xE8).isMappedToModuleCommand());
    }

    @Test
    void shouldMapExplicitAggregateCommandOnlyWhenPayloadIsComplete() {
        BatteryCollectorCommandResult incomplete = service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1, 2, 3);
        BatteryCollectorCommandResult complete = service.execute(
                BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                "battery-rs485-1",
                1000L,
                1, 2, 3, 4, 5, 6, 7);

        Assertions.assertFalse(incomplete.isMappedToModuleCommand());
        Assertions.assertTrue(complete.isMappedToModuleCommand());
        Assertions.assertEquals(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS,
                complete.getModuleControlCommand().getProtocolCode());
        Assertions.assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6, 7},
                complete.getModuleControlCommand().getPayload());
    }

    @Test
    void shouldEnsureRunningOptLogClearedAfterStopWithoutQueuedCommands() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 0);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE);

        Assertions.assertTrue(result.isSuccess());
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._2.getDictValue());
    }

    @Test
    void shouldResolveChannelNameByBatteryGroup() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        BatteryCollectorChannelConfig first = new BatteryCollectorChannelConfig();
        first.setName("battery-rs485-1");
        first.setConfigId(10L);
        first.setBatteryGroup(1);
        BatteryCollectorChannelConfig second = new BatteryCollectorChannelConfig();
        second.setName("battery-rs485-2");
        second.setConfigId(20L);
        second.setBatteryGroup(2);
        properties.getChannels().add(first);
        properties.getChannels().add(second);
        ReflectionTestUtils.setField(service, "properties", properties);

        String channelName = service.resolveChannelName(2);

        Assertions.assertEquals("battery-rs485-2", channelName);
    }

    @Test
    void shouldAcceptConnectResistanceTestAtMaxBoundary245() {
        BatteryCollectorCommandResult result = service.connectResistanceTest("battery-rs485-1", 1, 245, 1000L);

        Assertions.assertTrue(result.isMappedToModuleCommand());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNotNull(result.getModuleControlCommand());
        Assertions.assertEquals(245, result.getModuleControlCommand().getConnectResistanceMaxAddress());
    }

    @Test
    void shouldMapSingleInternalResistanceTestWithBoundaryAddresses() {
        // address = 1 (minimum valid)
        BatteryCollectorCommandResult result1 = service.singleInternalResistanceTest("battery-rs485-1", 1, 1, 1000L);
        Assertions.assertTrue(result1.isMappedToModuleCommand());
        Assertions.assertEquals(1, result1.getModuleControlCommand().getAddress());

        // address = 245 (maximum valid)
        BatteryCollectorCommandResult result245 = service.singleInternalResistanceTest("battery-rs485-1", 1, 245, 1000L);
        Assertions.assertTrue(result245.isMappedToModuleCommand());
        Assertions.assertEquals(245, result245.getModuleControlCommand().getAddress());
    }

    // ---- TASK-AI-VERIFY-STOP-002: stop internal resistance闭环 tests ----

    @Test
    void shouldRejectSingleInternalResistanceWhenAddressExceedsActualBatteryCount() {
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(24);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);

        BatteryCollectorCommandResult result = service.singleInternalResistanceTest("battery-rs485-1", 1, 25, 1000L);

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertFalse(result.isMappedToModuleCommand());
        Assertions.assertNull(result.getModuleControlCommand());
        Assertions.assertTrue(result.getMessage().contains("单体编号超过电池组实际单体数"));
    }

    @Test
    void shouldUseProtocolMaxWhenActualBatteryCountMissingOrUnavailable() {
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        Mockito.when(batteryPackService.getBatteryMaxNumber(1)).thenReturn(null);
        Mockito.when(batteryPackService.getBatteryMaxNumber(2)).thenThrow(new RuntimeException("db unavailable"));
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);

        BatteryCollectorCommandResult missingCount = service.singleInternalResistanceTest("battery-rs485-1", 1, 245, 1000L);
        BatteryCollectorCommandResult unavailableCount = service.singleInternalResistanceTest("battery-rs485-1", 2, 245, 1000L);
        BatteryCollectorCommandResult aboveProtocolMax = service.singleInternalResistanceTest("battery-rs485-1", 1, 246, 1000L);

        Assertions.assertTrue(missingCount.isMappedToModuleCommand());
        Assertions.assertEquals(245, missingCount.getModuleControlCommand().getAddress());
        Assertions.assertTrue(unavailableCount.isMappedToModuleCommand());
        Assertions.assertEquals(245, unavailableCount.getModuleControlCommand().getAddress());
        Assertions.assertFalse(aboveProtocolMax.isMappedToModuleCommand());
        Assertions.assertNull(aboveProtocolMax.getModuleControlCommand());
    }
    @Test
    void shouldStopInternalResistanceTestWithCorrectOptLogType() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);

        Assertions.assertTrue(result.isSuccess());
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._6.getDictValue());
    }

    @Test
    void shouldNotAffectLegacyInternalResistanceWhenStoppingSingleIR() {
        BatteryModeStatusService modeStatusService = newModeStatusService();
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);

        Assertions.assertTrue(result.isSuccess());
        // _6 stop calls doStopTest with _6 type, NOT _1 type
        Mockito.verify(optLogService).doStopTest(1, BatteryTestEnum._6.getDictValue());
        Mockito.verify(optLogService, Mockito.never()).doStopTest(Mockito.anyInt(), Mockito.eq(BatteryTestEnum._1.getDictValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldNotOverwriteLogWhenStopFollowedByChannelClose() {
        // STOP-004: 停止与通道关闭并发边界 —— 先停止取消队列，再通道关闭处理 pending，日志不重复覆盖
        BatteryModeStatusService modeStatusService = newModeStatusService();
        BatteryCollectorService collectorService = newCollectorService(modeStatusService);
        BatteryCollectorCommandQueueService commandQueueService = new BatteryCollectorCommandQueueService();
        BatteryCollectorCommandLogService commandLogService = (BatteryCollectorCommandLogService)
                ReflectionTestUtils.getField(collectorService, "commandLogService");
        ReflectionTestUtils.setField(commandQueueService, "batteryModeStatusService", modeStatusService);
        ReflectionTestUtils.setField(commandQueueService, "commandLogService", commandLogService);
        ReflectionTestUtils.setField(commandQueueService, "frameCodec", new BatteryCollectorFrameCodec());
        ReflectionTestUtils.setField(collectorService, "commandQueueService", commandQueueService);
        ReflectionTestUtils.setField(collectorService, "frameIoService", Mockito.mock(com.shanhe.project.collector.battery.runtime.BatteryCollectorFrameIoService.class));
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-rs485-1");
        BatteryCollectorChannelState state = new BatteryCollectorChannelState(channelConfig);
        List<BatteryCollectorChannelState> channelStates =
                (List<BatteryCollectorChannelState>) ReflectionTestUtils.getField(collectorService, "channelStates");
        channelStates.add(state);
        ReflectionTestUtils.setField(service, "collectorService", collectorService);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        // 队列中同组同模式的未下发命令（会被 stop 取消）
        BatteryModuleControlCommand queuedCmd = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(9)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .batteryGroup(1)
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .optLogId(100L)
                .build();
        state.getQueuedModuleCommands().add(queuedCmd);

        // 已发出正在等待响应的显式命令（stop 不处理 pending，由 close 收口）
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST,
                8, new byte[0], false);
        pendingRequest.setBatteryGroup(1);
        pendingRequest.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        pendingRequest.setOptLogId(200L);
        state.setPendingCommand(pendingRequest);

        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 200L);

        // Step 1: 停止测试 —— 取消队列命令、关闭运行日志、标记模式停止
        BatteryCollectorCommandResult result = service.stopRunningTest(
                1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertTrue(result.getMessage().contains("已取消未下发命令1条"));

        // Step 2: 通道关闭 —— pending 命令被收口为 FAILED，队列已空不应产生额外写入
        ReflectionTestUtils.invokeMethod(collectorService, "closeQuietly", state);

        // 队列命令被 stop 标记为 CANCELLED（原因=命令已取消）
        OptLogMapper optLogMapper = (OptLogMapper) ReflectionTestUtils.getField(
                ReflectionTestUtils.getField(collectorService, "commandLogService"), "optLogMapper");
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(100L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.CANCELLED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.any(),
                Mockito.anyString(),
                Mockito.isNull());

        // pending 命令被 close 标记为 FAILED（原因含"采集通道关闭，命令未完成"）
        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(200L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                Mockito.eq(1),
                Mockito.any(),
                Mockito.any(),
                Mockito.contains("采集通道关闭，命令未完成"),
                Mockito.any());

        // 模式最终为 IDLE
        BatteryModeInfo modeInfo = modeStatusService.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getLastMode());

        // 总共只有 2 次日志写入：stop 取消 1 次 + close 收口 pending 1 次，无重复覆盖
        Mockito.verify(optLogMapper, Mockito.times(2)).updateCommandStatus(
                Mockito.anyLong(), Mockito.anyString(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
    }
}
