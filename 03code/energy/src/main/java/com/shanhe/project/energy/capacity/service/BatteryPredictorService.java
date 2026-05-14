package com.shanhe.project.energy.capacity.service;


import com.shanhe.project.device.config.domain.BatteryReportLog;

/**
 * 蓄电池预测服务类
 * @author xuxw
 */
public interface BatteryPredictorService {

    /**
     * 统计蓄电池状态变化过程
     */
    void doTotalBatteryStep(Integer packNum, String batteryStatus, BatteryReportLog oldInfo);

}
