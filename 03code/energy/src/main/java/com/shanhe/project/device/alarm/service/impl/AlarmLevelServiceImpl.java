package com.shanhe.project.device.alarm.service.impl;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.StringUtils;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.device.alarm.domain.AlarmLevel;
import com.shanhe.project.device.alarm.mapper.AlarmLevelMapper;
import com.shanhe.project.device.alarm.service.AlarmLevelService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 告警级别
 *
 * @author wjh
 * @since 2025/6/11
 */
@Service
public class AlarmLevelServiceImpl implements AlarmLevelService {

    @Resource
    private AlarmLevelMapper alarmLevelMapper;

    CacheKeyEnum alarmLevelCache = CacheKeyEnum.ALARM_LEVEL;

    /**
     * 查询告警级别
     *
     * @param id 告警级别主键
     * @return 告警级别
     */
    @Override
    public AlarmLevel selectAlarmLevelById(Long id) {
        return alarmLevelMapper.selectAlarmLevelById(id);
    }

    /**
     * 查询告警级别列表
     *
     * @param alarmLevel 告警级别
     * @return 告警级别
     */
    @Override
    public List<AlarmLevel> selectAlarmLevelList(AlarmLevel alarmLevel) {
        List<AlarmLevel> alarmLevels;
        Object cacheObj = CacheUtils.get(alarmLevelCache.getCache(), alarmLevelCache.getKey());
        if (StringUtils.isNull(cacheObj)) {
            alarmLevels = refreshCache();
        } else {
            alarmLevels = StringUtils.cast(cacheObj);
        }
        if (CollectionUtils.isEmpty(alarmLevels)) {
            return new ArrayList<>();
        }

        // 根据 alarmLevel 过滤
        if (alarmLevel != null) {
            // 根据名称、编号过滤
            if (StringUtils.isNotEmpty(alarmLevel.getLevelName())) {
                alarmLevels = alarmLevels.stream().filter(item -> item.getLevelName().contains(alarmLevel.getLevelName())).collect(Collectors.toList());
            }
            if (StringUtils.isNotEmpty(alarmLevel.getLevelCode())) {
                alarmLevels = alarmLevels.stream().filter(item -> item.getLevelCode().contains(alarmLevel.getLevelCode())).collect(Collectors.toList());
            }
        }
        return alarmLevels;
    }

    /**
     * 新增告警级别
     *
     * @param alarmLevel 告警级别
     */
    @Override
    public void insertAlarmLevel(AlarmLevel alarmLevel) {
        alarmLevel.setId(IdUtils.getSnowflakeId());
        alarmLevelMapper.insertAlarmLevel(alarmLevel);
        refreshCache();
    }

    /**
     * 修改告警级别
     *
     * @param alarmLevel 告警级别
     */
    @Override
    public void updateAlarmLevel(AlarmLevel alarmLevel) {
        alarmLevelMapper.updateAlarmLevel(alarmLevel);
        refreshCache();
    }

    @Override
    public Map<String, AlarmLevel> mapAll() {
        return selectAlarmLevelList(new AlarmLevel()).stream().collect(Collectors.toMap(AlarmLevel::getLevelCode, Function.identity(), (v1, v2) -> v2));
    }

    @Override
    public Map<String, String> map() {
        return selectAlarmLevelList(new AlarmLevel()).stream().collect(Collectors.toMap(AlarmLevel::getLevelCode, AlarmLevel::getLevelName, (v1, v2) -> v2));
    }

    @Override
    public List<AlarmLevel> refreshCache() {
        List<AlarmLevel> alarmLevels = alarmLevelMapper.selectAlarmLevelList(new AlarmLevel());
        if (CollectionUtils.isEmpty(alarmLevels)) {
            return null;
        }
        CacheUtils.put(alarmLevelCache.getCache(), alarmLevelCache.getKey(), alarmLevels);
        return alarmLevels;
    }

    @Override
    public int deleteByIds(String ids) {
        String[] idArr = Convert.toStrArray(ids);
        for (String id : idArr) {
            alarmLevelMapper.deleteById(Long.valueOf(id));
        }
        refreshCache();
        return 1;
    }

}
