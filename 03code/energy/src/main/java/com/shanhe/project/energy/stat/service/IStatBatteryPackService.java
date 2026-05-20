package com.shanhe.project.energy.stat.service;

import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;

import java.util.List;
import java.util.Map;


/**
 * 电池组统计Service接口
 *
 * @author zhoubin
 * @date 2025-07-15
 */
public interface IStatBatteryPackService {
    /**
     * 查询电池组统计列表
     *
     * @return 电池组统计集合
     */
    List<StatBatteryPack> selectList(StatBatteryPack statBatteryPack);

    /**
     * 新增电池组统计
     *
     */
    void insertList(Integer packNum, Map<String, Object> packMap, List<BatteryMonitor> batteryList);

    /**
     * 删除记录
     *
     * @param packNum 电池组编号；为空时删除默认设备全部电池组统计
     */
    void deleteByPackNum(Integer packNum);

    /**
     * 导出数据
     *
     * @param params 查询参数
     */
    void export(StatBatteryPack params);
}
