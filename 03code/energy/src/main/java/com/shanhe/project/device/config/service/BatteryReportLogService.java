package com.shanhe.project.device.config.service;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.BatteryReportLogIndex;

import java.util.List;
import java.util.Map;

/**
 * 蓄电池上报日志
 *
 * @author wjh
 * @since 2025/7/9
 */
public interface BatteryReportLogService {
    /**
     * 插入记录
     */
    void insert(Integer packNum, Map<String, Object> packParam, List<BatteryMonitor> batteryList, boolean isInsert);

    /**
     * 查电池历史（告警处理）
     *
     * @param packNum 电池编号
     * @return 结果
     */
    BatteryReportLog selectLastHasAlarm(Integer packNum);

    /**
     * 获取电池组最新记录（缓存）
     *
     * @param packNum 电池组编号
     * @return 电池组
     */
    BatteryReportLog lastCache(Integer packNum);

    /**
     * 计算电电池内阻平均值
     *
     * @param packNum 电池组编号
     * @return 电电池内阻平均值
     */
    Long resistanceValue(Integer packNum);

    /**
     * 查电池历史
     *
     * @param batteryReportLog 电池
     * @return 结果
     */
    List<BatteryReportLog> selectBatteryReportLog(BatteryReportLog batteryReportLog);

    /**
     * 删除记录
     *
     * @param ids 记录id
     * @return 删除结果
     */
    int deleteByIds(String ids);

    /**
     * 删除设备历史记录
     *
     * @param dayNum 天数
     */
    void deleteByDays(Integer dayNum);

    /**
     * 更新缓存
     */
    void updateCache();

    /**
     * 查询电池组最新记录
     *
     * @return 电池组
     */
    List<BatteryReportLogIndex> batteryList();

    /**
     * 删除告警记录
     *
     * @param packNum 电池组编号；为空时删除默认设备全部历史
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 导出
     */
    void export(BatteryReportLog params);
}
