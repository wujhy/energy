package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标准实时模型到旧 BatteryReportLog 格式的适配器。
 * <p>
 * 将 600 模块端采集的 BatteryModuleGroupRealtime / BatteryModuleCellRealtime
 * 转换为旧统计/日志/内阻 service 期望的 BatteryReportLog 格式，
 * 避免在新链路中继续依赖 Map&lt;String,Object&gt; 扩散字段。
 *
 * @author wjh
 * @since 2026-06-05
 */
public class RealtimeToReportLogAdapter {

    private RealtimeToReportLogAdapter() {
    }

    /**
     * 将标准实时模型转换为 BatteryReportLog。
     *
     * @param packNum 电池组编号
     * @param group 组实时数据（可为 null）
     * @param cells 单体实时数据列表（可为 null）
     * @return BatteryReportLog，packParam 和 batteryList 已填充
     */
    public static BatteryReportLog adapt(Integer packNum,
                                          BatteryModuleGroupRealtime group,
                                          List<BatteryModuleCellRealtime> cells) {
        BatteryReportLog report = new BatteryReportLog();
        report.setConfigId(Constants.DEFAULT_CONFIG_ID);
        report.setPackNum(packNum);
        if (group != null) {
            report.setCreateTime(resolveRealtimeCreateTime(group));
        }
        report.setPackParam(buildPackParam(group));
        report.setBatteryList(buildBatteryList(packNum, cells));
        return report;
    }

    private static Date resolveRealtimeCreateTime(BatteryModuleGroupRealtime group) {
        if (group.getCreateTime() != null) {
            return group.getCreateTime();
        }
        if (group.getLatestGroupUpdateTime() != null) {
            return group.getLatestGroupUpdateTime();
        }
        if (group.getLatestCellUpdateTime() != null) {
            return group.getLatestCellUpdateTime();
        }
        return group.getPollStartedAt();
    }

    /** 将组实时数据转换为 packParam Map。 */
    private static Map<String, Object> buildPackParam(BatteryModuleGroupRealtime group) {
        Map<String, Object> packParam = new HashMap<>(32);
        if (group == null) {
            return packParam;
        }
        putIfNotNull(packParam, "packVoltage", group.getPackVoltage());
        putIfNotNull(packParam, "batteryPackOuterVoltage", group.getExternalVoltage());
        putIfNotNull(packParam, "packCurrent", group.getChargeDischargeCurrent());
        putIfNotNull(packParam, "batteryPackFloatCurrent", group.getFloatCurrent());
        putIfNotNull(packParam, "environmentTemperature1", group.getEnvironmentTemperature1());
        putIfNotNull(packParam, "environmentTemperature2", group.getEnvironmentTemperature2());
        putIfNotNull(packParam, "maxVoltageBatteryNumber", group.getMaxVoltageBatNum());
        putIfNotNull(packParam, "batteryMaxVoltage", group.getMaxCellVoltage());
        putIfNotNull(packParam, "minVoltageBatteryNumber", group.getMinVoltageBatNum());
        putIfNotNull(packParam, "batteryMinVoltage", group.getMinCellVoltage());
        putIfNotNull(packParam, "batteryAvgVoltage", group.getAvgCellVoltage());
        putIfNotNull(packParam, "batteryVoltageDeviation", group.getBatteryVoltageDeviation());
        putIfNotNull(packParam, "batteryVoltageRange", group.getVoltageRange());
        putIfNotNull(packParam, "maxResistanceBatteryNumber", group.getMaxResistanceBatNum());
        putIfNotNull(packParam, "batteryMaxResistance", group.getMaxInternalResistance());
        putIfNotNull(packParam, "minResistanceBatteryNumber", group.getMinResistanceBatNum());
        putIfNotNull(packParam, "batteryMinEsistance", group.getMinInternalResistance());
        putIfNotNull(packParam, "batteryAvgResistance", group.getAvgInternalResistance());
        putIfNotNull(packParam, "maxTemperatureBatteryNumber", group.getMaxTemperatureBatNum());
        putIfNotNull(packParam, "batteryMaxTemperature", group.getMaxCellTemperature());
        putIfNotNull(packParam, "minTemperatureBatteryNumber", group.getMinTemperatureBatNum());
        putIfNotNull(packParam, "batteryMinTemperature", group.getMinCellTemperature());
        putIfNotNull(packParam, "batteryAvgTemperature", group.getAvgCellTemperature());
        putIfNotNull(packParam, "batteryPackSoc", group.getBatteryPackSoc());
        putIfNotNull(packParam, "batteryPackSoh", group.getBatteryPackSoh());
        putIfNotNull(packParam, "residualDischargeDuration", group.getResidualDischargeDuration());
        putIfNotNull(packParam, "backupDuration", group.getBackupDuration());
        putIfNotNull(packParam, "rippleVoltage", group.getRippleVoltage());
        putIfNotNull(packParam, "hydrogenConcentration", group.getHydrogenConcentration());
        putIfNotNull(packParam, "positiveinsulationResistance", group.getPositiveInsulationResistance());
        putIfNotNull(packParam, "negativeinsulationResistance", group.getNegativeInsulationResistance());
        putIfNotNull(packParam, "groundingBatteryUpperLimit", group.getGroundingBatteryUpperLimit());
        putIfNotNull(packParam, "groundingBatteryLowerLimit", group.getGroundingBatteryLowerLimit());
        putIfNotNull(packParam, "maxResistanceRateChangeBatteryNumber", group.getMaxResistanceRateChangeBatNum());
        putIfNotNull(packParam, "maxResistanceRateChange", group.getMaxResistanceRateChange());
        putIfNotNull(packParam, "batteryPackStatus", group.getBatteryPackStatus());
        putIfNotNull(packParam, "resistanceTestStatus", group.getResistanceTestStatus());
        putIfNotNull(packParam, "deviceWorkStatus", group.getDeviceWorkStatus());
        putIfNotNull(packParam, "deviceWorkIOStatus", group.getDeviceWorkIoStatus());
        putIfNotNull(packParam, "bcapacity", group.getBcapacity());
        putIfNotNull(packParam, "capacity", group.getCapacity());
        putIfNotNull(packParam, "disChargeCapacity", group.getDisChargeCapacity());
        putIfNotNull(packParam, "disChargeDuration", group.getDisChargeDuration());
        return packParam;
    }

    /** 将单体实时数据列表转换为 BatteryMonitor 列表。 */
    private static List<BatteryMonitor> buildBatteryList(Integer packNum, List<BatteryModuleCellRealtime> cells) {
        List<BatteryMonitor> list = new ArrayList<>();
        if (cells == null) {
            return list;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null) {
                continue;
            }
            BatteryMonitor monitor = new BatteryMonitor();
            monitor.setConfigId(Constants.DEFAULT_CONFIG_ID);
            monitor.setPackNum(packNum);
            monitor.setBatNum(cell.getBatNum());
            monitor.setVoltage(cell.getVoltage());
            monitor.setResistance(cell.getResistance());
            monitor.setTemperature(cell.getTemperature());
            monitor.setBcapacity(cell.getCapacity());
            monitor.setResistancerageslip(cell.getResistanceRageSlip());
            monitor.setResistanceRateChange(cell.getResistanceRateChange());
            monitor.setGbvoltage(cell.getSwollenVoltage());
            monitor.setCreateTime(cell.getCreateTime());
            list.add(monitor);
        }
        return list;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
