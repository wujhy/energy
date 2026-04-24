package com.shanhe.project.device.config.service;

import com.shanhe.project.device.config.domain.BatteryMonitor;

import java.util.List;

/**
 * 设备Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IBatteryMonitorService
{
    /**
     * 新增【单体电池主机实时数据】
     *
     * @param listBattery 单体电池主机实时数据
     */
    void insertBatchBatteryMonitor(List<BatteryMonitor> listBattery);

    /**
     * 查电池历史
     *
     * @param batteryMonitor 电池
     * @return 结果
     */
    List<BatteryMonitor> selectBatteryMonitor(BatteryMonitor batteryMonitor);

    /**
     * 获取设备最新电池记录
     *
     * @param configId 设备id
     * @param packNum 电池组编号
     * @return 电池列表
     */
    List<BatteryMonitor> selectLast(Long configId, Integer packNum);

    /**
     * 获取单体电池最新记录（缓存）
     *
     * @param configId 设备id
     * @param packNum 电池组编号
     * @param batNum 电池序号
     * @return 电池
     */
    BatteryMonitor lastCache(Long configId, Integer packNum, Integer batNum);

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
    void deleteBatteryDays(Integer dayNum);
    /**
     * 更新缓存
     */
    void updateCache();
}
