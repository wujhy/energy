package com.shanhe.project.energy.capacity.service;


import com.shanhe.project.device.config.domain.BatteryReportLog;

/**
 * 蓄电池预测服务类
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface BatteryPredictorService {

    /**
     * 统计蓄电池状态变化过程
     *
     * @param packNum 电池组编号
     * @param batteryStatus 电池状态
     * @param oldInfo 旧报告数据
     */
    void doTotalBatteryStep(Integer packNum, String batteryStatus, BatteryReportLog oldInfo);

}
