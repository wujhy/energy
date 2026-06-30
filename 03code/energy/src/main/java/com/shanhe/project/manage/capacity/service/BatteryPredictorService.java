package com.shanhe.project.manage.capacity.service;


import com.shanhe.project.manage.config.domain.BatteryReportLog;

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

    /**
     * Run capacity prediction with caller-provided current realtime data.
     *
     * @param packNum battery pack number
     * @param batteryStatus current battery pack status
     * @param oldInfo previous report data
     * @param currentInfo current realtime data
     */
    void doTotalBatteryStep(Integer packNum, String batteryStatus, BatteryReportLog oldInfo, BatteryReportLog currentInfo);

}
