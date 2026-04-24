package com.shanhe.project.device.alarm.mapper;

import com.shanhe.project.device.alarm.domain.AlarmLevel;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AlarmLevelMapper {
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
     * @return 结果
     */
    int insertAlarmLevel(AlarmLevel alarmLevel);

    /**
     * 修改告警级别
     *
     * @param alarmLevel 告警级别
     * @return 结果
     */
    int updateAlarmLevel(AlarmLevel alarmLevel);

    /**
     * 删除
     *
     * @param id 主键
     */
    void deleteById(Long id);
}
