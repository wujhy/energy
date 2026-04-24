package com.shanhe.project.energy.stat.service;

import com.shanhe.project.energy.stat.vo.BatteryHealthReport;

import java.util.Map;

/**
 * @author zhoubin
 * @date 2025/9/26
 */
public interface IConfigurationBatteryService {

    /**
     * 获取健康报告
     */
    BatteryHealthReport getBatteryHealthReport(Long configId, Integer packNum);

    /**
     * 获取温度报警线
     */
    Map<String, Object> getTempWarnLine(Long configId, Integer packNum);

    /**
     * 获取内阻报警线
     */
    Map<String, Object> getResWarnLine(Long configId, Integer packNum);
}
