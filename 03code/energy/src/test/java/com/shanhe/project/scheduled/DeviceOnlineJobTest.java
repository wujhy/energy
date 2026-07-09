package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        configuration.addCache(new CacheConfiguration("sys-cache", 100).eternal(true));
        cacheManager = CacheManager.newInstance(configuration);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("cacheManager", cacheManager);
        new SpringUtils().postProcessBeanFactory(beanFactory);
    }

    @BeforeEach
    void clearCache() {
        CacheUtils.removeAll(CacheKeyEnum.BATTERY_ONLINE.getCache());
    }

    @Test
    void shouldUseRealtimeSnapshotForOnlineAlarmContext() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(1));
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        ArgumentCaptor<BatteryReportLog> captor = ArgumentCaptor.forClass(BatteryReportLog.class);
        Mockito.verify(alarmLogService).alarmBattery(Mockito.eq(1), Mockito.isNull(),
                Mockito.argThat(params -> "0".equals(params.get(ItemCode.TXZT.getCode()))), captor.capture());
        Assertions.assertEquals(1, captor.getValue().getPackNum());
        Assertions.assertEquals("48.5", String.valueOf(captor.getValue().getPackParam().get("packVoltage")));
        Assertions.assertEquals(1, captor.getValue().getBatteryList().size());
        Assertions.assertEquals(1, captor.getValue().getBatteryList().get(0).getBatNum());
    }

    @Test
    void shouldUseMinimalAlarmContextWhenRealtimeSnapshotUnavailable() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        ArgumentCaptor<BatteryReportLog> captor = ArgumentCaptor.forClass(BatteryReportLog.class);
        Mockito.verify(alarmLogService).alarmBattery(Mockito.eq(1), Mockito.isNull(),
                Mockito.argThat(params -> "0".equals(params.get(ItemCode.TXZT.getCode()))), captor.capture());
        Assertions.assertEquals(1, captor.getValue().getPackNum());
        Assertions.assertTrue(captor.getValue().getPackParam().isEmpty());
        Assertions.assertTrue(captor.getValue().getBatteryList().isEmpty());
    }

    @Test
    void shouldIgnoreDisabledBatteryPackWhenCheckingOnlineStatus() {
        DeviceOnlineJob job = newJob(
                pack(1, YesNoEnum.YES.getDictValue()),
                pack(2, YesNoEnum.NO.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryModuleRealtimeSnapshotService snapshotService = snapshotService(job);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(snapshot(1));
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verify(snapshotService, Mockito.never()).getCachedSnapshot(2);
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(2), Mockito.eq(false), Mockito.isNull(),
                Mockito.eq(Collections.singletonList(ItemCode.TXZT.getCode())));
        Mockito.verify(alarmLogService, Mockito.never()).alarmBattery(Mockito.eq(2),
                Mockito.any(), Mockito.anyMap(), Mockito.any());
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
