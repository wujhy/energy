package com.shanhe.project.device.alarm.mapper;

import java.util.List;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import org.apache.ibatis.annotations.Param;

/**
 * 设备历史记录Mapper接口
 * 
 * @author wjh
 * @since 2024-12-31
 */
public interface AlarmLogMapper 
{
    /**
     * 查询设备历史记录
     * 
     * @param alarmId 设备历史记录主键
     * @return 设备历史记录
     */
    AlarmLog selectAlarmLogByAlarmId(Long alarmId);

    /**
     * 告警设备数
     *
     * @return 设备数
     */
    Long alarmDeviceNum();

    /**
     * 设备告警数
     *
     * @return 设备告警记录数
     */
    Long alarmAllNum();

    /**
     * 查设备告警数
     *
     * @param alarmLog 查询条件
     * @return 设备告警记录数
     */
    Long alarmNum(AlarmLog alarmLog);

    /**
     * 启动设备全部告警记录
     *
     * @return 设备告警记录
     */
    List<AlarmLog> allAlarmLog();

    /**
     * 查询设备历史记录列表
     * 
     * @param alarmLog 设备历史记录
     * @return 设备历史记录集合
     */
    List<AlarmLog> selectAlarmLogList(AlarmLog alarmLog);

    /**
     * 新增设备历史记录
     * 
     * @param alarmLog 设备历史记录
     * @return 结果
     */
    int insertAlarmLog(AlarmLog alarmLog);

    /**
     * 修改设备历史记录
     * 
     * @param alarmLog 设备历史记录
     * @return 结果
     */
    int updateAlarmLog(AlarmLog alarmLog);

    /**
     * 删除设备历史记录
     *
     * @param configIds 设备id
     */
    void deleteAlarmLogByConfigIds(String[] configIds);

    /**
     * 删除设备历史记录
     *
     * @param alarmId 设备历史记录主键
     */
    void deleteAlarmLogByAlarmId(Long alarmId);

    /**
     * 批量删除设备历史记录
     * 
     * @param alarmIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteAlarmLogByAlarmIds(String[] alarmIds);

    /**
     * 清空设备历史记录
     *
     * @param configId 设备id
     */
    void clear(Long configId);

    /**
     * 删除设备告警记录信息
     */
    void deleteALL();

    /**
     * 删除设备告警记录信息
     */
    void deleteAlarmLogByConfigIdPackNum(@Param("configId") Long configId, @Param("packNum") Integer packNum);
}
