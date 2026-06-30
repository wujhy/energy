package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCurrentState;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.service.OptLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.shanhe.project.iot.model.BatteryModeInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class BatteryCurrentStateServiceTest {

    @Test
    void shouldReturnNoConfigWithoutReadingRealtimeTables() {
        BatteryCurrentStateService service = newService();
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(null);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(1, state.getPackNum());
        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_NO_CONFIG, state.getFreshness());
        Assertions.assertNull(state.getGroup());
        Assertions.assertTrue(state.getCells().isEmpty());
        Mockito.verifyNoInteractions(realtimeMapper);
    }

    @Test
    void shouldReturnFreshCurrentStateWithStatusesAndAlarms() {
        BatteryCurrentStateService service = newService();
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryDeviceStateService deviceStateService = Mockito.mock(BatteryDeviceStateService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", deviceStateService);
        ReflectionTestUtils.setField(service, "alarmLogService", alarmLogService);

        BatteryPack pack = new BatteryPack();
        pack.setPackId(10L);
        pack.setBatSinSize(2);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPackVoltage(54.2d);
        group.setPollBatchNo("batch-1");
        group.setDataFresh(true);
        group.setCellCount(2);
        group.setOnlineCellCount(2);
        BatteryModuleCellRealtime cellTwo = cell(1, 2, 2.12d);
        BatteryModuleCellRealtime cellOne = cell(1, 1, 2.10d);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(2)
                .group(group)
                .cells(Arrays.asList(cellTwo, cellOne))
                .build());
        BatteryDeviceState deviceState = new BatteryDeviceState();
        deviceState.setPackNum(1);
        deviceState.setStateCode(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        deviceState.setStateValue("true");
        Mockito.when(deviceStateService.selectByPackNum(1)).thenReturn(Collections.singletonList(deviceState));
        AlarmLog alarm = new AlarmLog();
        alarm.setAlarmId(3L);
        alarm.setPackNum(1);
        alarm.setModelNum(2);
        alarm.setItemCode("batteryOvercharge");
        alarm.setStatus(1);
        alarm.setCreateTime(new Date(100L));
        Mockito.when(alarmLogService.selectBatteryAlarmLogListCache(1)).thenReturn(Collections.singletonList(alarm));

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(10L, state.getPackId());
        Assertions.assertEquals(2, state.getExpectedCellCount());
        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_FRESH, state.getFreshness());
        Assertions.assertEquals("batch-1", state.getLastPollBatchNo());
        Assertions.assertEquals(54.2d, state.getGroup().getPackVoltage());
        Assertions.assertEquals(2, state.getCells().size());
        Assertions.assertEquals(1, state.getCells().get(0).getBatNum());
        Assertions.assertEquals(2, state.getCells().get(1).getBatNum());
        Assertions.assertEquals(1, state.getDeviceStates().size());
        Assertions.assertEquals(1, state.getAlarms().size());
        Assertions.assertEquals("batteryOvercharge", state.getAlarms().get(0).getItemCode());
        Mockito.verify(realtimeMapper, Mockito.never()).selectGroup(1);
        Mockito.verify(realtimeMapper, Mockito.never()).selectCells(1);
        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(snapshotService, Mockito.never()).getSnapshot(Mockito.anyInt());
    }

    @Test
    void shouldFallbackToRealtimeTablesWhenSnapshotIsMissing() {
        BatteryCurrentStateService service = newServiceWithPack(1, 2);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPollBatchNo("batch-db");
        group.setDataFresh(true);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);
        Mockito.when(realtimeMapper.selectGroup(1)).thenReturn(group);
        Mockito.when(realtimeMapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 1, 2.10d), cell(1, 2, 2.11d)));

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_FRESH, state.getFreshness());
        Assertions.assertEquals("batch-db", state.getLastPollBatchNo());
        Assertions.assertEquals(2, state.getCells().size());
        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(snapshotService, Mockito.never()).getSnapshot(Mockito.anyInt());
        Mockito.verify(realtimeMapper).selectGroup(1);
        Mockito.verify(realtimeMapper).selectCells(1);
    }

    @Test
    void shouldExposePartialWhenCollectedCellsAreLessThanConfiguredCount() {
        BatteryCurrentStateService service = newServiceWithPack(1, 4);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setDataFresh(true);
        Mockito.when(realtimeMapper.selectGroup(1)).thenReturn(group);
        Mockito.when(realtimeMapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 1, 2.10d), cell(1, 3, 2.11d)));

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_PARTIAL, state.getFreshness());
        Assertions.assertEquals(2, state.getCells().size());
    }

    @Test
    void shouldExposeStaleWhenGroupRealtimeIsMarkedStale() {
        BatteryCurrentStateService service = newServiceWithPack(1, 1);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setDataFresh(false);
        Mockito.when(realtimeMapper.selectGroup(1)).thenReturn(group);
        Mockito.when(realtimeMapper.selectCells(1)).thenReturn(Collections.singletonList(cell(1, 1, 2.10d)));

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_STALE, state.getFreshness());
    }

    @Test
    void shouldExposeNotCollectedWhenRealtimeIsEmpty() {
        BatteryCurrentStateService service = newServiceWithPack(1, 1);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        Mockito.when(realtimeMapper.selectGroup(1)).thenReturn(null);
        Mockito.when(realtimeMapper.selectCells(1)).thenReturn(Collections.emptyList());

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertEquals(BatteryCurrentState.FRESHNESS_NOT_COLLECTED, state.getFreshness());
        Assertions.assertNull(state.getGroup());
        Assertions.assertTrue(state.getCells().isEmpty());
    }

    @Test
    void shouldIncludeRunningOptLogsInCurrentState() {
        BatteryCurrentStateService service = newServiceWithPack(1, 2);
        BatteryModuleRealtimeMapper realtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "realtimeMapper", realtimeMapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setDataFresh(true);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(2)
                .group(group)
                .cells(Arrays.asList(cell(1, 1, 2.10d), cell(1, 2, 2.11d)))
                .build());

        OptLog runningLog = new OptLog();
        runningLog.setId(100L);
        runningLog.setPackNum(1);
        runningLog.setType(BatteryTestEnum._2.getDictValue());
        runningLog.setStatus(BatteryDeviceStateConstants.CommandStatus.PENDING);
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.singletonList(runningLog));

        BatteryCurrentState state = service.getCurrentState(1);

        Assertions.assertNotNull(state.getRunningOptLogs());
        Assertions.assertEquals(1, state.getRunningOptLogs().size());
        Assertions.assertEquals(100L, state.getRunningOptLogs().get(0).getId());
        Mockito.verify(optLogService).selectRunningList(1);
    }

    private BatteryCurrentStateService newServiceWithPack(Integer packNum, Integer batSinSize) {
        BatteryCurrentStateService service = newService();
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryPack pack = new BatteryPack();
        pack.setPackId(10L);
        pack.setBatSinSize(batSinSize);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(packNum)).thenReturn(pack);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", Mockito.mock(BatteryDeviceStateService.class));
        ReflectionTestUtils.setField(service, "alarmLogService", Mockito.mock(IAlarmLogService.class));
        return service;
    }

    private BatteryCurrentStateService newService() {
        return new BatteryCurrentStateService();
    }

    private BatteryModuleCellRealtime cell(Integer packNum, Integer batNum, Double voltage) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(packNum);
        cell.setBatNum(batNum);
        cell.setVoltage(voltage);
        cell.setPollBatchNo("batch-1");
        return cell;
    }

    /**
     * READ-003: 当前态接口对失败/取消/超时后的运行态读取回归。
     * 确认 _2/_6 失败、取消、超时后 modeInfo 不再显示 running，命令最近完成状态仍可追踪。
     */
    @Test
    void shouldReturnIdleModeInfoAfterTestStoppedWithLastModePreserved() {
        // 使用真实的 BatteryModeStatusService + TestCacheAccessor
        BatteryModeStatusService modeStatusService = newRealModeStatusService();
        BatteryCurrentStateService service = newServiceWithPack(1, 2);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        // 模拟内阻测试(_6)启动后失败停止
        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 200L);
        modeStatusService.markStopped(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, false, 200L);

        // 停止后 runningOptLogs 应为空（命令已完成）
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.emptyList());

        BatteryCurrentState state = service.getCurrentState(1);

        // modeInfo 应显示 IDLE，不再显示 running
        BatteryModeInfo modeInfo = state.getModeInfo();
        Assertions.assertNotNull(modeInfo);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(Integer.valueOf(0), modeInfo.getStatus());
        // 上一次模式应被保留，用于追踪
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getLastMode());
        Assertions.assertEquals(Integer.valueOf(1), modeInfo.getLastPackNum());
        // 运行日志应为空
        Assertions.assertTrue(state.getRunningOptLogs().isEmpty());
    }

    @Test
    void shouldReturnIdleModeInfoAfterConnectResistanceStopped() {
        // READ-003 补充：连接条电阻(_2) 测试停止后同样回归 IDLE
        BatteryModeStatusService modeStatusService = newRealModeStatusService();
        BatteryCurrentStateService service = newServiceWithPack(1, 2);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 1, 100L);
        modeStatusService.markStopped(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 1, true, 100L);
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.emptyList());

        BatteryCurrentState state = service.getCurrentState(1);

        BatteryModeInfo modeInfo = state.getModeInfo();
        Assertions.assertNotNull(modeInfo);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(Integer.valueOf(0), modeInfo.getStatus());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, modeInfo.getLastMode());
        Assertions.assertTrue(state.getRunningOptLogs().isEmpty());
    }

    @Test
    void shouldReturnRunningModeInfoWhileTestIsActive() {
        // READ-003 对照：测试运行中 modeInfo 应显示 running
        BatteryModeStatusService modeStatusService = newRealModeStatusService();
        BatteryCurrentStateService service = newServiceWithPack(1, 2);
        ReflectionTestUtils.setField(service, "batteryModeStatusService", modeStatusService);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);

        modeStatusService.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 200L);
        OptLog runningLog = new OptLog();
        runningLog.setId(200L);
        runningLog.setPackNum(1);
        runningLog.setType(BatteryTestEnum._6.getDictValue());
        runningLog.setStatus(BatteryDeviceStateConstants.CommandStatus.PENDING);
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.singletonList(runningLog));

        BatteryCurrentState state = service.getCurrentState(1);

        BatteryModeInfo modeInfo = state.getModeInfo();
        Assertions.assertNotNull(modeInfo);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(Integer.valueOf(1), modeInfo.getStatus());
        Assertions.assertEquals(1, state.getRunningOptLogs().size());
    }

    private BatteryModeStatusService newRealModeStatusService() {
        BatteryModeStatusService modeStatusService = new BatteryModeStatusService();
        ReflectionTestUtils.setField(modeStatusService, "cacheAccessor", new TestCacheAccessor());
        return modeStatusService;
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
}
