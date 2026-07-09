package com.shanhe.project.manage.capacity.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;

import java.util.List;

/**
 * 蓄电池预测服务类
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface BatteryPredictorService {

    /**
     * 基于当前标准实时数据统计蓄电池放电结束后的容量
     *
     * @param group 组信息
     * @param cells 单体信息
     */
    void doTotalBatteryStep(BatteryModuleGroupRealtime group, List<BatteryModuleCellRealtime> cells);

}
