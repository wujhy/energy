package com.shanhe.project.manage.config.service;

import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;

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
     *
     * @param packNum 电池组编号
     * @param packParam 电池组参数
     * @param batteryList 单体电池列表
     * @param isInsert 是否新增
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

    /** 更新缓存 */
    void updateCache();


    /**
     * 删除告警记录
     *
     * @param packNum 电池组编号；为空时删除默认设备全部历史
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 导出
     *
     * @param params 查询参数
     */
    void export(BatteryReportLog params);
}
