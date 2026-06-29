package com.shanhe.project.device.alarm.service.impl;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.config.service.IConfigAttributeService;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class AlarmLogServiceImplTest {

    private static CacheManager cacheManager;

    private final AlarmLogServiceImpl service = new AlarmLogServiceImpl();
    private BatteryDeviceStateService batteryDeviceStateService;

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
        batteryDeviceStateService = Mockito.mock(BatteryDeviceStateService.class);
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", batteryDeviceStateService);
    }

    @AfterEach
    void tearDown() {
        CacheUtils.removeAll(CacheKeyEnum.ALARM.getCache());
    }

    @Test
    void shouldAppendCommunicationStateAlarmsToBatteryAlarmCacheList() {
        Mockito.when(batteryDeviceStateService.selectByPackNum(2)).thenReturn(Arrays.asList(
                state(2, null, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "open failed",
                        BatteryDeviceStateConstants.StateLevel.ERROR),
                state(2, 8, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "01/81",
                        BatteryDeviceStateConstants.StateLevel.WARN),
                state(2, null, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "fresh",
                        BatteryDeviceStateConstants.StateLevel.NORMAL)));

        List<AlarmLog> alarmLogs = service.selectBatteryAlarmLogListCache(2);

        List<String> itemCodes = alarmLogs.stream().map(AlarmLog::getItemCode).collect(Collectors.toList());
        Assertions.assertEquals(2, alarmLogs.size());
        Assertions.assertTrue(itemCodes.contains(ItemCode.DTTXZT.getCode()));
        Assertions.assertTrue(itemCodes.contains(ItemCode.TXZT.getCode()));
        Assertions.assertTrue(alarmLogs.stream().allMatch(log -> YesNoEnum.NO.getDictValue().equals(log.getStatus())));
    }

    @Test
    void shouldUseCommunicationStateWhenCheckingAlarmByCache() {
        Mockito.when(batteryDeviceStateService.selectByPackNum(2)).thenReturn(Collections.singletonList(
                state(2, null, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "stale",
                        BatteryDeviceStateConstants.StateLevel.WARN)));

        Integer isAlarm = service.isAlarmByCache(2);

        Assertions.assertEquals(YesNoEnum.YES.getDictValue(), isAlarm);
    }

    @Test
    void shouldAppendCommunicationStateAlarmsToCacheAlarmListAndBatteryAlarmNum() {
        Mockito.when(batteryDeviceStateService.selectList(Mockito.any(BatteryDeviceState.class))).thenReturn(Arrays.asList(
                state(2, null, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "stale",
                        BatteryDeviceStateConstants.StateLevel.WARN),
                state(3, null, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "open failed",
                        BatteryDeviceStateConstants.StateLevel.ERROR)));

        List<AlarmLog> alarmLogs = service.cacheAlarmList();

        Assertions.assertEquals(2, alarmLogs.size());
        Assertions.assertEquals(2L, service.batteryAlarmNum());
        Assertions.assertTrue(alarmLogs.stream().anyMatch(log -> Integer.valueOf(2).equals(log.getPackNum())
                && ItemCode.TXZT.getCode().equals(log.getItemCode())));
        Assertions.assertTrue(alarmLogs.stream().anyMatch(log -> Integer.valueOf(3).equals(log.getPackNum())
                && ItemCode.DTTXZT.getCode().equals(log.getItemCode())));
    }

    @Test
    void shouldNotDoubleCountStateAlarmWhenSameCachedAlarmExistsInBatteryAlarmNum() {
        AlarmLog cachedAlarm = new AlarmLog();
        cachedAlarm.setPackNum(2);
        cachedAlarm.setModelNum(null);
        cachedAlarm.setItemCode(ItemCode.TXZT.getCode());
        cachedAlarm.setStatus(YesNoEnum.NO.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 2, null, ItemCode.TXZT.getCode()),
                cachedAlarm);
        Mockito.when(batteryDeviceStateService.selectList(Mockito.any(BatteryDeviceState.class))).thenReturn(Collections.singletonList(
                state(2, null, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "stale",
                        BatteryDeviceStateConstants.StateLevel.WARN)));

        Assertions.assertEquals(1L, service.batteryAlarmNum());
        Assertions.assertEquals(1, service.cacheAlarmList().size());
    }

    @Test
    void shouldIgnoreRecoveredCommunicationStates() {
        Mockito.when(batteryDeviceStateService.selectByPackNum(2)).thenReturn(Arrays.asList(
                state(2, null, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "cleared",
                        BatteryDeviceStateConstants.StateLevel.NORMAL),
                state(2, 8, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "recovered",
                        BatteryDeviceStateConstants.StateLevel.NORMAL),
                state(2, 8, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "active",
                        BatteryDeviceStateConstants.StateLevel.NORMAL)));

        Assertions.assertTrue(service.selectBatteryAlarmLogListCache(2).isEmpty());
        Assertions.assertEquals(YesNoEnum.NO.getDictValue(), service.isAlarmByCache(2));
    }

    @Test
    void shouldNotAppendStateAlarmWhenSameCachedAlarmExists() {
        AlarmLog handledCacheAlarm = new AlarmLog();
        handledCacheAlarm.setPackNum(2);
        handledCacheAlarm.setItemCode(ItemCode.TXZT.getCode());
        handledCacheAlarm.setStatus(YesNoEnum.YES.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 2, null, ItemCode.TXZT.getCode()),
                handledCacheAlarm);
        Mockito.when(batteryDeviceStateService.selectByPackNum(2)).thenReturn(Collections.singletonList(
                state(2, null, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "stale",
                        BatteryDeviceStateConstants.StateLevel.WARN)));

        List<AlarmLog> alarmLogs = service.selectBatteryAlarmLogListCache(2);

        Assertions.assertEquals(1, alarmLogs.size());
        Assertions.assertSame(handledCacheAlarm, alarmLogs.get(0));
        Assertions.assertEquals(YesNoEnum.YES.getDictValue(), alarmLogs.get(0).getStatus());
    }

    @Test
    void shouldMapChannelTimeoutCountToCommunicationAlarm() {
        Mockito.when(batteryDeviceStateService.selectByPackNum(2)).thenReturn(Collections.singletonList(
                state(2, null, BatteryDeviceStateConstants.StateCode.CHANNEL_TIMEOUT_COUNT, "3",
                        BatteryDeviceStateConstants.StateLevel.WARN)));

        List<AlarmLog> alarmLogs = service.selectBatteryAlarmLogListCache(2);

        Assertions.assertEquals(1, alarmLogs.size());
        Assertions.assertEquals(ItemCode.TXZT.getCode(), alarmLogs.get(0).getItemCode());
    }

    @Test
    void shouldAllowNullConfigWhenValidatingBatteryAlarmValue() {
        IConfigAttributeService configAttributeService = Mockito.mock(IConfigAttributeService.class);
        ReflectionTestUtils.setField(service, "configAttributeService", configAttributeService);
        Mockito.when(configAttributeService.getCacheBy(2, ItemCode.TXZT.getCode())).thenReturn(null);

        Assertions.assertDoesNotThrow(() -> service.alarmBatteryValue(
                null, 2, null, Collections.singletonMap(ItemCode.TXZT.getCode(), "1")));
    }

    @Test
    void shouldIsolateCellAlarmCacheByModelNum() {
        AlarmLog cell1Alarm = new AlarmLog();
        cell1Alarm.setPackNum(1);
        cell1Alarm.setModelNum(1);
        cell1Alarm.setItemCode(ItemCode.DTDYGC.getCode());
        cell1Alarm.setStatus(YesNoEnum.NO.getDictValue());
        CacheUtils.put(CacheKeyEnum.ALARM.getCache(),
                String.format(CacheKeyEnum.ALARM.getKey(), 1, 1, ItemCode.DTDYGC.getCode()),
                cell1Alarm);

        AlarmLog cell2Alarm = new AlarmLog();
        cell2Alarm.setPackNum(1);
        cell2Alarm.setModelNum(2);
        cell2Alarm.setItemCode(ItemCode.DTDYGC.getCode());
        cell2Alarm.setStatus(YesNoEnum.YES.getDictValue());
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

    private BatteryDeviceState state(Integer packNum, Integer modelNum, String stateCode,
                                     String stateValue, String stateLevel) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setPackNum(packNum);
        state.setModelNum(modelNum);
        state.setStateCode(stateCode);
        state.setStateValue(stateValue);
        state.setStateLevel(stateLevel);
        return state;
    }
}
