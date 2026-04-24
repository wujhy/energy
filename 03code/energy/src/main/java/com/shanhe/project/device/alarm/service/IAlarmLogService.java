package com.shanhe.project.device.alarm.service;

import java.util.List;
import java.util.Map;

import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;

/**
 * 设备历史记录Service接口
 * 
 * @author wjh
 * @since 2024-12-31
 */
public interface IAlarmLogService 
{
    /**
     * 通过缓存获取告警记录
     */
    AlarmLog getByCache(Long configId, Integer packNum, Integer modelNum, String itemCode);

    /**
     * 查询设备历史记录
     * 
     * @param alarmId 设备历史记录主键
     * @return 设备历史记录
     */
    AlarmLog selectAlarmLogByAlarmId(Long alarmId);

    /**
     * 设备是否告警
     *
     * @return 是否存在告警记录
     */
    Integer isAlarm();

    /**
     * 查设备是否告警
     *
     * @param configId 设备主键
     * @return 设备历史记录
     */
    Integer isAlarmByCache(Long configId, Integer packNum);

    /**
     * 查设备告警记录数
     *
     * @param configId 设备主键
     * @return 告警记录数
     */
    Long alarmNum(Long configId);

    /**
     * 查设备告警记录数
     *
     * @return 告警记录数
     */
    Long alarmNum();

    /**
     * 总告警数量
     */
    Long alarmAllNum();

    /**
     * 告警设备数量
     */
    Long alarmDeviceNum();

    /**
     * 查询设备历史记录列表
     * 
     * @param alarmLog 设备历史记录
     * @return 设备历史记录集合
     */
    List<AlarmLog> selectAlarmLogList(AlarmLog alarmLog);

    /**
     * 当前未处理的告警项
     *
     * @return 告警日志列表
     */
    List<AlarmLog> cacheAlarmList();

    /**
     * 告警（蓄电池）
     *
     * @param config 设备配置
     * @param packNum 组编码
     * @param modelNum 模块编号
     * @param warnParam 告警参数
     * @param batteryReportLog 电池上报记录
     */
    void alarmBattery(Config config, Integer packNum, Integer modelNum, Map<String, String> warnParam, BatteryReportLog batteryReportLog);

    /**
     * 蓄电池故障告警（值）
     *
     * @param config 设备配置
     * @param packNum 组编码
     * @param modelNum 模块编号
     * @param warnParam 告警参数
     */
    void alarmBatteryValue(Config config, Integer packNum, Integer modelNum, Map<String, String> warnParam);

    /**
     * 关闭告警
     *
     * @param configId 配置id
     * @param packNum 包序号
     * @param isModel 是否模块属性
     * @param excludeModelNum 排除的单体电池序号
     * @param includeCode 包含属性
     */
    void alarmFix(Long configId, Integer packNum, Boolean isModel, List<Integer> excludeModelNum, List<String> includeCode);

    /**
     * 告警验证
     *
     * @param configAttribute 属性字段
     * @param value 最新值
     */
    void alarmValid(ConfigAttribute configAttribute, String value);

    /**
     * 告警验证
     *
     * @param configAttribute 属性字段
     * @param modelNum 模块编号
     * @param value 最新值
     * @param type 设备类型
     */
    void alarmValid(ConfigAttribute configAttribute, Integer modelNum, String value, Integer type);

    /**
     * 基于缓存判断是否告警
     *
     * @param configAttribute 属性
     * @return 是否告警
     */
    Integer isAlarm(ConfigAttribute configAttribute);

    /**
     * 基于属性关闭告警
     */
    void closeAlarmLog(ConfigAttribute configAttribute);

    /**
     * 基于设备关闭告警
     */
    void closeAlarmLog(Long configId);

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
     * 修改设备历史记录
     *
     * @param alarmLog 设备历史记录
     * @return 结果
     */
    int shiedAlarmLog(AlarmLog alarmLog);

    /**
     * 批量删除设备历史记录
     * 
     * @param alarmIds 需要删除的设备历史记录主键集合
     * @return 结果
     */
    int deleteAlarmLogByAlarmIds(String alarmIds);

    /**
     * 删除设备历史记录
     *
     * @param configIds 设备id
     */
    void deleteAlarmLogByConfigIds(String[] configIds);

    /**
     * 删除设备历史记录信息
     * 
     * @param alarmId 设备历史记录主键
     */
    void deleteAlarmLogByAlarmId(Long alarmId);

    /**
     * 更新告警缓存
     */
    void updateCache();

    /**
     * 查询告警缓存
     *
     * @param configId 设备id
     * @param packNum 包序号
     * @return 告警缓存列表
     */
    List<AlarmLog> selectAlarmLogListCache(Long configId, Integer packNum);

    /**
     * 获取当前是否告警
     *
     * @param itemCode 属性字段
     * @return 告警数量
     */
    Long getCurrentIsAlarm(String itemCode);

    /**
     * 导出
     *
     * @param alarmLog 导出
     */
    void export(AlarmLog alarmLog);

    void deleteALL();

    /**
     * 删除设备历史记录信息
     *
     * @param configId 设备id
     * @param packNum 包序号
     */
    void deleteAlarmLogByConfigIdPackNum(Long configId, Integer packNum);
}
