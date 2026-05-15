package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
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
    void shouldCheckBatteryPackOnlineCacheInSerialMode() {
        DeviceOnlineJob job = newJob(pack(1, YesNoEnum.YES.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(job, "batteryReportLogService");
        BatteryReportLog reportLog = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog);
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(alarmLogService).alarmBattery(Mockito.any(Config.class), Mockito.eq(1), Mockito.isNull(),
                Mockito.argThat(params -> "0".equals(params.get(ItemCode.TXZT.getCode()))), Mockito.same(reportLog));
    }

    @Test
    void shouldIgnoreDisabledBatteryPackWhenCheckingOnlineStatus() {
        DeviceOnlineJob job = newJob(
                pack(1, YesNoEnum.YES.getDictValue()),
                pack(2, YesNoEnum.NO.getDictValue()));
        IAlarmLogService alarmLogService = (IAlarmLogService) ReflectionTestUtils.getField(job, "alarmLogService");
        BatteryReportLogService reportLogService = (BatteryReportLogService) ReflectionTestUtils.getField(job, "batteryReportLogService");
        BatteryReportLog reportLog = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(reportLog);
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(),
                String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), 1),
                new Date());

        job.cmdDevice();

        Mockito.verify(reportLogService).lastCache(1);
        Mockito.verify(reportLogService, Mockito.never()).lastCache(2);
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(2), Mockito.eq(false), Mockito.isNull(),
                Mockito.eq(Collections.singletonList(ItemCode.TXZT.getCode())));
        Mockito.verify(alarmLogService, Mockito.never()).alarmBattery(Mockito.any(Config.class), Mockito.eq(2),
                Mockito.any(), Mockito.anyMap(), Mockito.any());
    }

    private DeviceOnlineJob newJob(BatteryPack... packs) {
        DeviceOnlineJob job = new DeviceOnlineJob();
        IConfigService configService = Mockito.mock(IConfigService.class);
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);

        Config config = new Config();
        Mockito.when(configService.selectDefaultConfig()).thenReturn(config);
        Mockito.when(batteryPackService.selectBatteryPackListCache(null))
                .thenReturn(packs == null ? Collections.emptyList() : Arrays.asList(packs));

        ReflectionTestUtils.setField(job, "configService", configService);
        ReflectionTestUtils.setField(job, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(job, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(job, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(job, "isStart", false);
        ReflectionTestUtils.setField(job, "maxOffline", 5);
        return job;
    }

    private BatteryPack pack(Integer packNum, Integer isEnabled) {
        BatteryPack pack = new BatteryPack();
        pack.setPackNum(packNum);
        pack.setIsEnabled(isEnabled);
        return pack;
    }
}
