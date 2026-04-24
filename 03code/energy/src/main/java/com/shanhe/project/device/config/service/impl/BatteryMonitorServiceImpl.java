package com.shanhe.project.device.config.service.impl;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.mapper.BatteryMonitorMapper;
import com.shanhe.project.device.config.service.IBatteryMonitorService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 电池Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class BatteryMonitorServiceImpl implements IBatteryMonitorService
{
    @Resource
    private BatteryMonitorMapper batteryMonitorMapper;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    // 缓存枚举
    CacheKeyEnum batteryCache = CacheKeyEnum.BATTERY;

    @Override
    public void insertBatchBatteryMonitor(List<BatteryMonitor> listBattery) {
        List<Long> updateBatteryList = new ArrayList<>();
        List<BatteryMonitor> newBatteryList = new ArrayList<>();
        for (BatteryMonitor battery : listBattery) {
            // 取缓存
            String key = String.format(batteryCache.getKey(), battery.getConfigId(), battery.getPackNum(), battery.getBatNum());
            BatteryMonitor cacheBattery = (BatteryMonitor) CacheUtils.get(batteryCache.getCache(), key);
            // 无记录 或 哈希值不一致，则插入新纪录
            if (cacheBattery == null || !StrUtil.equals(cacheBattery.getHashCode(), battery.getHashCode())) {
                // 插入新纪录
                battery.setId(IdUtils.getSnowflakeId());
                newBatteryList.add(battery);
                CacheUtils.put(batteryCache.getCache(), key, battery);
                continue;
            }

            // 只需更新时间
            updateBatteryList.add(cacheBattery.getId());
        }

        // 更新时间
        if (!updateBatteryList.isEmpty()) {
            batteryMonitorMapper.updateMonitor(updateBatteryList);
        }

        // 增加记录
        if (!newBatteryList.isEmpty()) {
            batteryMonitorMapper.insertBatchBatteryMonitor(newBatteryList);
        }
    }

    @Override
    public List<BatteryMonitor> selectBatteryMonitor(BatteryMonitor batteryMonitor) {
        return batteryMonitorMapper.selectBatteryMonitor(batteryMonitor);
    }

    @Override
    public List<BatteryMonitor> selectLast(Long configId, Integer packNum) {
        BatteryPack batteryInfo = batteryPackService.selectBatteryInfoByPackNum(configId, packNum);
        if (batteryInfo == null) {
            return null;
        }
        List<BatteryMonitor> list = batteryMonitorMapper.selectLast(configId, packNum, batteryInfo.getBatSinSize());
        if (!list.isEmpty()) {
            // 单体电池告警记录
            List<AlarmLog> alarmLogs = alarmLogService.selectAlarmLogListCache(configId, packNum);
            Map<Integer, List<AlarmLog>> batAlarmMap = alarmLogs.stream()
                    .filter(item -> item.getModelNum() != null)
                    .collect(Collectors.groupingBy(AlarmLog::getModelNum));

            // 单体电池告警记录
            list.forEach(entity -> {
                entity.setAlarmList(batAlarmMap.getOrDefault(entity.getBatNum(), new ArrayList<>()));
            });
        }

        return list;
    }

    @Override
    public BatteryMonitor lastCache(Long configId, Integer packNum, Integer batNum) {
        // 取缓存
        String key = String.format(batteryCache.getKey(), configId, packNum, batNum);
        Object object = CacheUtils.get(batteryCache.getCache(), key);
        return object != null ? (BatteryMonitor) object : null;
    }

    @Override
    public int deleteByIds(String ids) {
        String[] idArr = Convert.toStrArray(ids);
        if (idArr.length == 0) {
            return 0;
        }
        List<BatteryMonitor> list = batteryMonitorMapper.listByIds(idArr);
        for (BatteryMonitor battery : list) {
            String key = String.format(batteryCache.getKey(), battery.getConfigId(), battery.getPackNum(), battery.getBatNum());
            BatteryMonitor cacheBattery = (BatteryMonitor) CacheUtils.get(batteryCache.getCache(), key);
            if (Objects.equals(battery.getId(), cacheBattery.getId())) {
                CacheUtils.remove(batteryCache.getCache(), key);
            }
        }

        return batteryMonitorMapper.deleteByIds(idArr);
    }

    @Override
    public void deleteBatteryDays(Integer dayNum) {
        batteryMonitorMapper.deleteBatteryDays(dayNum);
    }

    @Override
    public void updateCache() {
        // 查所有最新电池记录
        List<BatteryMonitor> batteryList = batteryMonitorMapper.lastMonitor();
        // 删除全部缓存
        CacheUtils.removeAll(batteryCache.getCache());
        for (BatteryMonitor battery : batteryList) {
            /* battery.配置id.包编号.模块编号 */
            String key = String.format(batteryCache.getKey(), battery.getConfigId(), battery.getPackNum(), battery.getBatNum());
            // 告警记录存在重复，则把旧告警处理
            CacheUtils.put(batteryCache.getCache(), key, battery);
        }
    }
}
