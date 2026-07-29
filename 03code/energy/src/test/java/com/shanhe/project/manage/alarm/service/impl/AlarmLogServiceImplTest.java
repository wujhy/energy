package com.shanhe.project.manage.alarm.service.impl;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class AlarmLogServiceImplTest {

    private static CacheManager cacheManager;

    private final AlarmLogServiceImpl service = new AlarmLogServiceImpl();

    @BeforeAll
    static void setupCacheManager() {
        Configuration configuration = new Configuration();
        configuration.setName("alarmLogServiceImplTest");
        configuration.addCache(new CacheConfiguration(CacheKeyEnum.ALARM.getCache(), 100).eternal(true));
        cacheManager = CacheManager.newInstance(configuration);

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("cacheManager", cacheManager);
        new SpringUtils().postProcessBeanFactory(beanFactory);
    }

    @BeforeEach
    void setUp() {
        CacheUtils.removeAll(CacheKeyEnum.ALARM.getCache());
    }

    @AfterEach
    void tearDown() {
        CacheUtils.removeAll(CacheKeyEnum.ALARM.getCache());
    }

    @Test
    void shouldUseOnlyCachedAlarmLogsWhenCheckingAlarmByCache() {
        Assertions.assertEquals(YesNoEnum.NO.getDictValue(), service.isAlarmByCache(2));

        AlarmLog cachedAlarm = alarm(2, null, ItemCode.TXZT.getCode(), YesNoEnum.NO.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 2, null, ItemCode.TXZT.getCode()),
                cachedAlarm);

        Assertions.assertEquals(YesNoEnum.YES.getDictValue(), service.isAlarmByCache(2));
        Assertions.assertEquals(YesNoEnum.NO.getDictValue(), service.isAlarmByCache(3));
    }

    @Test
    void shouldCountOnlyOpenBatteryAlarmLogsFromCache() {
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 2, null, ItemCode.TXZT.getCode()),
                alarm(2, null, ItemCode.TXZT.getCode(), YesNoEnum.NO.getDictValue()));
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 2, 1, ItemCode.DTDYGC.getCode()),
                alarm(2, 1, ItemCode.DTDYGC.getCode(), YesNoEnum.YES.getDictValue()));

        Assertions.assertEquals(2, service.selectBatteryAlarmLogListCache(2).size());
        Assertions.assertEquals(1L, service.batteryAlarmNum());
        Assertions.assertEquals(2, service.cacheAlarmList().size());
    }

    @Test
    void shouldAllowNullConfigWhenValidatingBatteryAlarmValue() {
        IConfigAttributeService configAttributeService = Mockito.mock(IConfigAttributeService.class);
        ReflectionTestUtils.setField(service, "configAttributeService", configAttributeService);
        Mockito.when(configAttributeService.getCacheBy(2, ItemCode.TXZT.getCode())).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> service.alarmBatteryValue(
                2, null, Collections.singletonMap(ItemCode.TXZT.getCode(), "1")));
    }

    @Test
    void shouldIsolateCellAlarmCacheByModelNum() {
        AlarmLog cell1Alarm = alarm(1, 1, ItemCode.DTDYGC.getCode(), YesNoEnum.NO.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 1, 1, ItemCode.DTDYGC.getCode()),
                cell1Alarm);

        AlarmLog cell2Alarm = alarm(1, 2, ItemCode.DTDYGC.getCode(), YesNoEnum.YES.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 1, 2, ItemCode.DTDYGC.getCode()),
                cell2Alarm);

        AlarmLog cached1 = (AlarmLog) CacheUtils.get(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 1, 1, ItemCode.DTDYGC.getCode()));
        AlarmLog cached2 = (AlarmLog) CacheUtils.get(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 1, 2, ItemCode.DTDYGC.getCode()));

        Assertions.assertNotNull(cached1);
        Assertions.assertNotNull(cached2);
        Assertions.assertEquals(YesNoEnum.NO.getDictValue(), cached1.getStatus());
        Assertions.assertEquals(YesNoEnum.YES.getDictValue(), cached2.getStatus());
        Assertions.assertNotSame(cached1, cached2);
    }

    private AlarmLog alarm(Integer packNum, Integer modelNum, String itemCode, Integer status) {
        AlarmLog alarm = new AlarmLog();
        alarm.setPackNum(packNum);
        alarm.setModelNum(modelNum);
        alarm.setItemCode(itemCode);
        alarm.setStatus(status);
        return alarm;
    }
}
