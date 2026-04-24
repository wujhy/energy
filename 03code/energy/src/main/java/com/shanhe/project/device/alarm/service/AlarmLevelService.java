package com.shanhe.project.device.alarm.service;

import com.shanhe.project.device.alarm.domain.AlarmLevel;

import java.util.List;
import java.util.Map;

/**
 * 告警级别
 *
 * @author wjh
 * @since 2025/6/11
 */
public interface AlarmLevelService {

    /**
     * 查询告警级别
     *
     * @param id 告警级别主键
     * @return 告警级别
     */
    AlarmLevel selectAlarmLevelById(Long id);

    /**
     * 查询告警级别列表
     *
     * @param alarmLevel 告警级别
     * @return 告警级别集合
     */
    List<AlarmLevel> selectAlarmLevelList(AlarmLevel alarmLevel);

    /**
     * 新增告警级别
     *
     * @param alarmLevel 告警级别
     */
    void insertAlarmLevel(AlarmLevel alarmLevel);

    /**
     * 修改告警级别
     *
     * @param alarmLevel 告警级别
     */
    void updateAlarmLevel(AlarmLevel alarmLevel);

    Map<String, AlarmLevel> mapAll();

    Map<String, String> map();

    List<AlarmLevel> refreshCache();

    /**
     * 批量删除告警级别
     *
     * @param ids 需要删除的告警级别主键集合
     * @return 结果
     */
    int deleteByIds(String ids);
}
