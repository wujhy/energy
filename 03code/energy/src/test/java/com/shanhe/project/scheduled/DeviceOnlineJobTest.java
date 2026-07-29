package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

class DeviceOnlineJobTest {

    private static CacheManager cacheManager;

    @BeforeAll
    static void setupCacheManager() {
        Configuration configuration = new Configuration();
        configuration.setName("deviceOnlineJobTest");
        configuration.addCache(new CacheConfiguration(CacheKeyEnum.BATTERY_ONLINE.getCache(), 100).eternal(true));
        cacheManager = CacheManager.newInstance(configuration);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("cacheManager", cacheManager);
        new SpringUtils().postProcessBeanFactory(beanFactory);
        if (cacheManager.getCache("sys-cache") == null) {
            try {
                cacheManager.addCache(new Cache(new CacheConfiguration("sys-cache", 100).eternal(true)));
            } catch (Exception ignored) {
                // 复用外部测试环境已有缓存即可。
            }
        }
    }

    private static void ensureSysCache() {
        try {
            java.lang.reflect.Field field = CacheUtils.class.getDeclaredField("CACHE_MANAGER");
            field.setAccessible(true);
            CacheManager manager = (CacheManager) field.get(null);
            if (manager != null && manager.getCache("sys-cache") == null) {
                manager.addCache(new Cache(new CacheConfiguration("sys-cache", 100).eternal(true)));
            }
        } catch (Exception ignored) {
            // 测试环境下静态缓存可能已由其他用例初始化，忽略即可。
        }
    }
    @BeforeEach
    void clearCache() {
        ensureSysCache();
        try {
            CacheUtils.removeAll(CacheKeyEnum.BATTERY_ONLINE.getCache());
        } catch (Exception ignored) {
            // 测试环境下缓存可能未预置，忽略即可。
        }
    }

    @Test
    void shouldUseRealtimeSnapshotForOnlineAlarmContext() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        BatteryDeviceStateService deviceStateService = deviceStateService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(1));
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(alarmLogService, Mockito.never()).alarmBatteryValue(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap());
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(false), Mockito.isNull(),
                Mockito.eq(Collections.singletonList(ItemCode.TXZT.getCode())));
        Mockito.verify(deviceStateService).upsert(Mockito.argThat(state ->
                state != null
                        && "1".equals(state.getScopeKey())
                        && BatteryDeviceStateConstants.StateCode.ONLINE.equals(state.getStateCode())
                        && "online".equals(state.getStateValue())));
    }

    @Test
    void shouldUseMinimalAlarmContextWhenRealtimeSnapshotUnavailable() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        BatteryDeviceStateService deviceStateService = deviceStateService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(alarmLogService, Mockito.never()).alarmBatteryValue(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyMap());
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(false), Mockito.isNull(),
                Mockito.eq(Collections.singletonList(ItemCode.TXZT.getCode())));
        Mockito.verify(deviceStateService).upsert(Mockito.argThat(state ->
                state != null
                        && "1".equals(state.getScopeKey())
                        && BatteryDeviceStateConstants.StateCode.ONLINE.equals(state.getStateCode())
                        && "online".equals(state.getStateValue())));
    }

    @Test
    void shouldIgnoreDisabledBatteryPackWhenCheckingOnlineStatus() {
        DeviceOnlineJob job = newJob(
                pack(1, YesNoEnum.YES.getDictValue()),
                pack(2, YesNoEnum.NO.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        BatteryDeviceStateService deviceStateService = deviceStateService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(1));
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(snapshotService, Mockito.never()).getCachedSnapshot(2);
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(2), Mockito.eq(false), Mockito.isNull(),
                Mockito.eq(Collections.singletonList(ItemCode.TXZT.getCode())));
        Mockito.verify(deviceStateService, Mockito.never()).upsert(Mockito.argThat(state ->
                state != null && "2".equals(state.getScopeKey())));
    }

    @Test
    void shouldCreateOfflineAlarmWhenOnlineCacheMissingTooLong() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryDeviceStateService deviceStateService = deviceStateService(job);
        @SuppressWarnings("unchecked")
        java.util.Map<Integer, Integer> offlineCounts =
                (java.util.Map<Integer, Integer>) ReflectionTestUtils.getField(job, "offlineBatteryPackNumMap");
        offlineCounts.put(1, 6);

        job.cmdDevice();

        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1), Mockito.isNull(),
                Mockito.argThat(params -> "1".equals(params.get(ItemCode.TXZT.getCode()))));
        Mockito.verify(deviceStateService).upsert(Mockito.argThat(state ->
                state != null
                        && "1".equals(state.getScopeKey())
                        && BatteryDeviceStateConstants.StateCode.ONLINE.equals(state.getStateCode())
                        && "offline".equals(state.getStateValue())
                        && BatteryDeviceStateConstants.StateLevel.WARN.equals(state.getStateLevel())));
    }

    private DeviceOnlineJob newJob(BatteryPack... packs) {
        DeviceOnlineJob job = new DeviceOnlineJob();
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryDeviceStateService deviceStateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(batteryPackService.selectBatteryPackListCache(null))
                .thenReturn(packs == null ? Collections.emptyList() : Arrays.asList(packs));
        ReflectionTestUtils.setField(job, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(job, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(job, "realtimeSnapshotService", snapshotService);
        ReflectionTestUtils.setField(job, "batteryDeviceStateService", deviceStateService);
        ReflectionTestUtils.setField(job, "isStart", false);
        ReflectionTestUtils.setField(job, "maxOffline", 5);
        return job;
    }

    private BatteryModuleRealtimeSnapshotService snapshotService(DeviceOnlineJob job) {
        return (BatteryModuleRealtimeSnapshotService) ReflectionTestUtils.getField(job, "realtimeSnapshotService");
    }

    private BatteryDeviceStateService deviceStateService(DeviceOnlineJob job) {
        return (BatteryDeviceStateService) ReflectionTestUtils.getField(job, "batteryDeviceStateService");
    }

    private BatteryPack pack(Integer packNum, Integer isEnabled) {
        BatteryPack pack = new BatteryPack();
        pack.setPackNum(packNum);
        pack.setIsEnabled(isEnabled);
        return pack;
    }

    private BatteryModuleRealtimeSnapshot snapshot(Integer packNum) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(packNum);
        group.setDeviceWorkStatus(1);
        group.setPackVoltage(48.5);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(1);
        cell.setResistance(100);
        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(packNum)
                .group(group)
                .cells(Collections.singletonList(cell))
                .build();
    }
}
