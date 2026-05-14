package com.shanhe.project.energy.stat.service;


import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.energy.stat.domain.StatBatteryRes;

import java.util.List;
import java.util.Map;


/**
 * 单体内阻变化统计（内阻测试后）Service接口
 *
 * @author zhoubin
 * @date 2025-07-21
 */
public interface IStatBatteryResService {
    /**
     * 获取内阻报表
     *
     * @param packNum 电池组编号
     * @return 内阻报表
     */
    Map<String, Object> getResistanceReport(Integer packNum);

    /**
     * 初始化
     *
     * @param packNum 电池组信息
     * @param oldInfo 旧数据
     */
    void init(Integer packNum, Map<String, Object> packMap, List<BatteryMonitor> batteryList, BatteryReportLog oldInfo);

    /**
     * 获取最新数据
     *
     * @param packNum 电池组编号
     * @return 最新数据
     */
    Map<Integer, Integer> last(Integer packNum);

    /**
     * 获取内阻数据
     *
     * @param packNum 电池组编号
     * @param batNum 单体编号
     * @return 内阻数据
     */
    List<StatBatteryRes> listResistance(Integer packNum, Integer batNum);

    /**
     * 删除
     *
     * @param packNum 电池组编号；为空时删除默认设备全部内阻统计
     */
    void deleteByConfigId(Integer packNum);

    /**
     * 导出
     *
     * @param packNum 电池组编号
     */
    void export(Integer packNum, String exportPath);
}
