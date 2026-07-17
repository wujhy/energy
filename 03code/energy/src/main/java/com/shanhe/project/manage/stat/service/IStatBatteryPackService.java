package com.shanhe.project.manage.stat.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.stat.domain.StatBatteryPack;

import java.util.List;


/**
 * 电池组统计Service接口
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface IStatBatteryPackService {
    /**
     * 查询电池组统计列表
     *
     * @param statBatteryPack 查询条件
     * @return 电池组统计集合
     */
    List<StatBatteryPack> selectList(StatBatteryPack statBatteryPack);

    /**
     * 新增标准实时模型电池组统计。
     *
     * @param packNum 电池组编号
     * @param group 组实时数据
     * @param cells 单体实时数据
     */
    void insertRealtime(Integer packNum, BatteryModuleGroupRealtime group, List<BatteryModuleCellRealtime> cells);

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
