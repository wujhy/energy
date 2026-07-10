package com.shanhe.project.collector.battery.service;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.host.domain.Host;
import com.shanhe.project.manage.host.service.IHostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * Storage interval guard for battery history data.
 */
@Service
public class BatteryStorageIntervalService {

    @Value("${storage.interval.alarm:30}")
    private Integer intervalAlarm;

    @Value("${storage.interval.data:60}")
    private Integer intervalData;

    @Resource
    private IAlarmLogService alarmLogService;

    @Resource
    private IHostService hostService;

    private final CacheKeyEnum cache = CacheKeyEnum.STORAGE_TIME;

    public boolean shouldInsert(Integer packNum) {
        if (packNum == null) {
            return false;
        }
        String grouping = String.valueOf(packNum);
        boolean insert = doInsert(grouping);
        if (insert) {
            CacheUtils.put(cache.getCache(), String.format(cache.getKey(), grouping), new Date());
        }
        return insert;
    }

    private boolean doInsert(String grouping) {
        Object lastStored = CacheUtils.get(cache.getCache(), String.format(cache.getKey(), grouping));
        if (!(lastStored instanceof Date)) {
            return true;
        }

        long now = System.currentTimeMillis();
        long lastTime = ((Date) lastStored).getTime();
        if (lastTime + getInterval() * 1000L <= now) {
            return true;
        }
        if (lastTime + intervalAlarm * 1000L > now) {
            return false;
        }

        Integer packNum = Integer.parseInt(grouping);
        Integer alarmByCache = alarmLogService.isAlarmByCache(packNum);
        if (alarmByCache == null || alarmByCache == 1) {
            return false;
        }
        return lastTime + intervalAlarm * 1000L <= now;
    }

    private Integer getInterval() {
        Host host = hostService.getDetail();
        if (host == null || host.getStorageTime() == null) {
            return intervalData;
        }
        return host.getStorageTime();
    }
}