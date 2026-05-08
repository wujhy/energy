package com.shanhe.project.iot.service;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author zhoubin
 * @date 2025/9/22
 */
@Service
public class DataService {

    // 告警存储间隔：秒
    @Value("${storage.interval.alarm:30}")
    private Integer intervalAlarm;

    // 正常存储间隔：秒
    @Value("${storage.interval.data:60}")
    private Integer intervalData;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IHostService hostService;

    CacheKeyEnum cache = CacheKeyEnum.STORAGE_TIME;

    /**
     * 是否存储
     * @param configId 配置ID
     * @param grouping 组合ID
     *                    1、开关量、模拟量：空
     *                    2、指令数据：指令编号
     *                    3、蓄电池数据：组编号
     * @param isPack 是否是蓄电池组数据
     * @return true 存储，false 不存储
     */
    public boolean isInsert(Long configId, String grouping, boolean isPack) {
        boolean b = doInsert(configId, grouping, isPack);

        if (b) {
            String key = String.format(cache.getKey(), configId, grouping);
            // 记录最后存储时间
            CacheUtils.put(cache.getCache(), key, new Date());
        }
        return b;
    }

    private boolean doInsert(Long configId, String grouping, boolean isPack) {
        String key = String.format(cache.getKey(), configId, grouping);
        Object o = CacheUtils.get(cache.getCache(), key);
        // 首次存储
        if (o == null) {
            return true;
        }

        // 类型检查
        if (!(o instanceof Date)) {
            // 缓存数据异常，重新初始化
            return true;
        }


        long currentTimeMillis = System.currentTimeMillis();

        Date date = (Date) o;
        long lastTime = date.getTime();

        // 超过正常存储间隔则存储
        if (lastTime + getInterval() * 1000 <= currentTimeMillis) {
            return true;
        }
        // 没有达到告警存储间隔则不存储
        if (lastTime + intervalAlarm * 1000 > currentTimeMillis) {
            return false;
        }

        Integer packNum = isPack ? Integer.parseInt(grouping) : null;
        Integer alarmByCache = alarmLogService.isAlarmByCache(configId, packNum);
        // 1 不告警，0 告警
        if (alarmByCache == null || alarmByCache == 1) {
            return false;
        }
        // 告警中，30 秒存储一次；否则，按配置存储一次
        return lastTime + intervalAlarm * 1000 <= currentTimeMillis;
    }

    /**
     * 获取存储间隔
     */
    private Integer getInterval() {
        // 主机
        Host host = hostService.getDetail();
        if (host == null) {
            return intervalData;
        }
        if (host.getStorageTime() == null) {
            return intervalData;
        }
        return host.getStorageTime();
    }
}
