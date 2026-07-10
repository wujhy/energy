package com.shanhe.project.collector.battery.service;

import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 600节实时数据到旧上报模型的适配服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Slf4j
@Service
public class BatteryModuleReportLogAdapterService {

    /** 实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;

    /**
     * 构建兼容旧 BatteryReportLog 的实时数据对象。
     *
     * @param packNum 电池组编号
     * @return 兼容旧实时上报结构的数据对象
     */
    public BatteryReportLog buildReportLog(Integer packNum) {
        BatteryModuleRealtimeSnapshot snapshot =
                snapshotService == null ? null : snapshotService.getCachedSnapshot(packNum);
        if (snapshot == null) {
            return null;
        }
        return buildReportLog(packNum, snapshot.getGroup(), snapshot.getCells());
    }

    /**
     * 获取当前上报模型：仅使用标准实时模型，不再回退旧上报缓存。
     *
     * @param packNum 电池组编号
     * @return 当前可用上报模型
     */
    public BatteryReportLog currentOrLastCache(Integer packNum) {
        return currentOrLastCache(packNum, false);
    }

    /**
     * 获取当前上报模型：仅使用标准实时模型，不再回退旧上报缓存。
     *
     * @param packNum 电池组编号
     * @param requireBatteryList 是否要求单体列表可用
     * @return 当前可用上报模型
     */
    public BatteryReportLog currentOrLastCache(Integer packNum, boolean requireBatteryList) {
        try {
            BatteryReportLog realtimeLog = buildReportLog(packNum);
            if (isUsable(realtimeLog, requireBatteryList)) {
                return realtimeLog;
            }
        } catch (Exception e) {
            log.warn("标准实时数据构建当前上报模型失败, packNum={}", packNum, e);
        }
        return null;
    }

    private boolean isUsable(BatteryReportLog log, boolean requireBatteryList) {
        if (log == null || log.getPackParam() == null || log.getPackParam().isEmpty()) {
            return false;
        }
        return !requireBatteryList || (log.getBatteryList() != null && !log.getBatteryList().isEmpty());
    }

    /**
     * 构建兼容旧 BatteryReportLog 的实时数据对象。
     *
     * @param packNum 电池组编号
     * @param group 组实时数据
     * @param cells 单体实时数据
     * @return 兼容旧实时上报结构的数据对象
     */
    public BatteryReportLog buildReportLog(Integer packNum,
                                           BatteryModuleGroupRealtime group,
                                           List<BatteryModuleCellRealtime> cells) {
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        reportLog.setPackNum(packNum);
        if (group != null) {
            reportLog.setCreateTime(group.getCreateTime());
        }

        Map<String, Object> packParam = toPackParam(group);
        List<BatteryMonitor> batteryList = toBatteryList(packNum, cells);
        reportLog.setPackParam(packParam);
        reportLog.setBatteryList(batteryList);
        reportLog.setPackData(JSON.toJSONString(packParam));
        reportLog.setMonitorData(JSON.toJSONString(batteryList));
        return reportLog;
    }

    /**
     * 转换为旧 pack_data 字段结构。
     *
     * @param group 组实时数据
     * @return 旧 pack_data 兼容 Map
     */
    public Map<String, Object> toPackParam(BatteryModuleGroupRealtime group) {
        Map<String, Object> packMap = new LinkedHashMap<>();
        if (group == null) {
            return packMap;
        }
        put(packMap, "packVoltage", group.getPackVoltage());
        put(packMap, "batteryPackOuterVoltage", group.getExternalVoltage());
        put(packMap, "packCurrent", group.getChargeDischargeCurrent());
        put(packMap, "batteryPackFloatCurrent", group.getFloatCurrent());
        put(packMap, "environmentTemperature1", group.getEnvironmentTemperature1());
        put(packMap, "environmentTemperature2", group.getEnvironmentTemperature2());

        put(packMap, "maxVoltageBatteryNumber", group.getMaxVoltageBatNum());
        put(packMap, "batteryMaxVoltage", group.getMaxCellVoltage());
        put(packMap, "minVoltageBatteryNumber", group.getMinVoltageBatNum());
        put(packMap, "batteryMinVoltage", group.getMinCellVoltage());
        put(packMap, "batteryAvgVoltage", group.getAvgCellVoltage());
        put(packMap, "batteryVoltageDeviation", group.getBatteryVoltageDeviation());
        put(packMap, "batteryVoltageRange", group.getVoltageRange());

        put(packMap, "maxResistanceBatteryNumber", group.getMaxResistanceBatNum());
        put(packMap, "batteryMaxResistance", group.getMaxInternalResistance());
        put(packMap, "minResistanceBatteryNumber", group.getMinResistanceBatNum());
        put(packMap, "batteryMinEsistance", group.getMinInternalResistance());
        put(packMap, "batteryAvgResistance", group.getAvgInternalResistance());

        put(packMap, "maxTemperatureBatteryNumber", group.getMaxTemperatureBatNum());
        put(packMap, "batteryMaxTemperature", group.getMaxCellTemperature());
        put(packMap, "minTemperatureBatteryNumber", group.getMinTemperatureBatNum());
        put(packMap, "batteryMinTemperature", group.getMinCellTemperature());
        put(packMap, "batteryAvgTemperature", group.getAvgCellTemperature());

        put(packMap, "batteryPackSoc", group.getBatteryPackSoc());
        put(packMap, "batteryPackSoh", group.getBatteryPackSoh());
        put(packMap, "residualDischargeDuration", group.getResidualDischargeDuration());
        put(packMap, "backupDuration", group.getBackupDuration());
        put(packMap, "rippleVoltage", group.getRippleVoltage());
        put(packMap, "hydrogenConcentration", group.getHydrogenConcentration());
        put(packMap, "positiveinsulationResistance", group.getPositiveInsulationResistance());
        put(packMap, "negativeinsulationResistance", group.getNegativeInsulationResistance());
        put(packMap, "groundingBatteryUpperLimit", group.getGroundingBatteryUpperLimit());
        put(packMap, "groundingBatteryLowerLimit", group.getGroundingBatteryLowerLimit());
        put(packMap, "maxResistanceRateChangeBatteryNumber", group.getMaxResistanceRateChangeBatNum());
        put(packMap, "maxResistanceRateChange", group.getMaxResistanceRateChange());
        put(packMap, "deviceWorkStatus", group.getDeviceWorkStatus());
        put(packMap, "deviceWorkIOStatus", group.getDeviceWorkIoStatus());
        put(packMap, "batteryPackStatus", group.getBatteryPackStatus());
        put(packMap, "resistanceTestStatus", group.getResistanceTestStatus());
        put(packMap, "bcapacity", group.getBcapacity());
        put(packMap, "capacity", group.getCapacity());
        put(packMap, "disChargeCapacity", group.getDisChargeCapacity());
        put(packMap, "disChargeDuration", group.getDisChargeDuration());
        return packMap;
    }

    /**
     * 转换为旧 monitor_data 字段结构。
     *
     * @param packNum 电池组编号
     * @param cells 单体实时数据
     * @return 旧 monitor_data 兼容列表
     */
    public List<BatteryMonitor> toBatteryList(Integer packNum,
                                              List<BatteryModuleCellRealtime> cells) {
        List<BatteryMonitor> result = new ArrayList<>();
        if (cells == null || cells.isEmpty()) {
            return result;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null) {
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
            result.add(monitor);
        }
        return result;
    }

    private void put(Map<String, Object> packMap, String key, Object value) {
        if (value != null) {
            packMap.put(key, String.valueOf(value));
        }
    }
}
