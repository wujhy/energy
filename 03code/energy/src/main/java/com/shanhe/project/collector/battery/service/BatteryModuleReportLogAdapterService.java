package com.shanhe.project.collector.battery.service;

import com.alibaba.fastjson.JSON;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
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
@Service
public class BatteryModuleReportLogAdapterService {

    /**
     * 600节模块端标准实时数据 Mapper。
     */
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;

    /**
     * 构建兼容旧 BatteryReportLog 的实时数据对象。
     *
     * @param configId 设备配置ID
     * @param packNum 电池组编号
     * @return 兼容旧实时上报结构的数据对象
     */
    public BatteryReportLog buildReportLog(Long configId, Integer packNum) {
        BatteryModuleGroupRealtime group = realtimeMapper.selectGroup(packNum);
        List<BatteryModuleCellRealtime> cells = realtimeMapper.selectCells(packNum);
        return buildReportLog(configId, packNum, group, cells);
    }

    /**
     * 构建兼容旧 BatteryReportLog 的实时数据对象。
     *
     * @param configId 设备配置ID
     * @param packNum 电池组编号
     * @param group 组实时数据
     * @param cells 单体实时数据
     * @return 兼容旧实时上报结构的数据对象
     */
    public BatteryReportLog buildReportLog(Long configId,
                                           Integer packNum,
                                           BatteryModuleGroupRealtime group,
                                           List<BatteryModuleCellRealtime> cells) {
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setConfigId(configId);
        reportLog.setPackNum(packNum);
        if (group != null) {
            reportLog.setCreateTime(group.getCreateTime());
        }

        Map<String, Object> packParam = toPackParam(group);
        List<BatteryMonitor> batteryList = toBatteryList(configId, packNum, cells);
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
        put(packMap, "batteryPackOuterVoltage", group.getBatteryPackOuterVoltage());
        put(packMap, "packCurrent", group.getPackCurrent());
        put(packMap, "batteryPackFloatCurrent", group.getBatteryPackFloatCurrent());
        put(packMap, "environmentTemperature1", group.getEnvironmentTemperature1());
        put(packMap, "environmentTemperature2", group.getEnvironmentTemperature2());

        put(packMap, "maxVoltageBatteryNumber", group.getMaxVoltageBatNum());
        put(packMap, "batteryMaxVoltage", group.getMaxCellVoltage());
        put(packMap, "minVoltageBatteryNumber", group.getMinVoltageBatNum());
        put(packMap, "batteryMinVoltage", group.getMinCellVoltage());
        put(packMap, "batteryAvgVoltage", group.getAvgCellVoltage());
        put(packMap, "batteryVoltageDeviation", group.getBatteryVoltageDeviation());
        put(packMap, "batteryVoltageRange", group.getBatteryVoltageRange());

        put(packMap, "maxResistanceBatteryNumber", group.getMaxResistanceBatNum());
        put(packMap, "batteryMaxResistance", group.getMaxInternalResistance());
        put(packMap, "minResistanceBatteryNumber", group.getMinResistanceBatNum());
        put(packMap, "batteryMinEsistance", group.getMinInternalResistance());
        put(packMap, "batteryAvgResistance", group.getAvgInternalResistance());

        put(packMap, "maxTemperatureBatteryNumber", group.getMaxTemperatureBatNum());
        put(packMap, "batteryMaxTemperature", group.getMaxCellTemperature());
        put(packMap, "minTemperatureBatteryNumber", group.getMinTemperatureBatNum());
        put(packMap, "batteryMinTemperature", group.getMinCellTemperature());
        put(packMap, "batteryAvgTemperature", group.getBatteryAvgTemperature());

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
     * @param configId 设备配置ID
     * @param packNum 电池组编号
     * @param cells 单体实时数据
     * @return 旧 monitor_data 兼容列表
     */
    public List<BatteryMonitor> toBatteryList(Long configId,
                                              Integer packNum,
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
            monitor.setConfigId(configId);
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
