package com.shanhe.project.energy.stat.service;

import com.shanhe.project.energy.stat.vo.BatteryHealthReport;

import java.util.Map;

/**
 * 电池配置服务接口
 *
 * @author wjh
 * @since 2026-05-25
 */
public interface IConfigurationBatteryService {

    /**
     * 获取健康报告
     *
     * @param packNum 电池组编号
     * @return 健康报告
     */
    BatteryHealthReport getBatteryHealthReport(Integer packNum);

    /**
     * 获取温度报警线
     *
     * @param packNum 电池组编号
     * @return 温度告警线
     */
    Map<String, Object> getTempWarnLine(Integer packNum);

    /**
     * 获取内阻报警线
     *
     * @param packNum 电池组编号
     * @return 内阻告警线
     */
    Map<String, Object> getResWarnLine(Integer packNum);
}
