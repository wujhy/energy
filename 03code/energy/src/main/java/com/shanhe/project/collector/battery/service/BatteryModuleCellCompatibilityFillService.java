package com.shanhe.project.collector.battery.service;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import com.shanhe.project.energy.capacity.vo.PreBatteryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单体兼容字段缓存填充服务。
 *
 * @author wjh
 * @since 2026-04-29
 */
@Service
public class BatteryModuleCellCompatibilityFillService {

    /**
     * 连接条电阻缓存，后续由显式连接条测试流程写入。
     */
    private final Map<String, Double> connectResistanceCache = new ConcurrentHashMap<>();

    /**
     * 旧预估容量缓存服务，缺失时不影响600节实时采集。
     */
    @Autowired(required = false)
    private PreBatteryGroupService preBatteryGroupService;

    /**
     * 从缓存补充600节默认采集不直接提供的单体兼容字段。
     *
     * @param channelConfig 采集通道配置
     * @param cell 单体实时数据
     */
    public void fillFromCache(BatteryCollectorChannelConfig channelConfig, BatteryModuleCellRealtime cell) {
        if (cell == null) {
            return;
        }
        fillBcapacity(channelConfig, cell);
        fillConnectResistance(channelConfig, cell);
    }

    /**
     * 缓存连接条电阻测试结果。
     *
     * @param batteryGroup 电池组编号
     * @param batNum 单体编号
     * @param resistanceRageSlip 连接条电阻
     */
    public void putConnectResistance(Integer batteryGroup, Integer batNum, Double resistanceRageSlip) {
        if (batteryGroup == null || batNum == null || resistanceRageSlip == null) {
            return;
        }
        connectResistanceCache.put(buildCellKey(batteryGroup, batNum), resistanceRageSlip);
    }

    private void fillBcapacity(BatteryCollectorChannelConfig channelConfig, BatteryModuleCellRealtime cell) {
        if (preBatteryGroupService == null
                || channelConfig == null
                || channelConfig.getConfigId() == null
                || channelConfig.getBatteryGroup() == null
                || cell.getBatNum() == null) {
            return;
        }
        PreBatteryGroup group = preBatteryGroupService.lastCache(channelConfig.getConfigId(), channelConfig.getBatteryGroup());
        if (group == null || group.getMapBattery() == null) {
            return;
        }
        PreBatteryVo battery = group.getMapBattery().get(Constants.CAP_BAT + cell.getBatNum());
        if (battery != null && battery.getBcapacity() != null) {
            cell.setCapacity(battery.getBcapacity());
        }
    }

    private void fillConnectResistance(BatteryCollectorChannelConfig channelConfig, BatteryModuleCellRealtime cell) {
        Integer batteryGroup = channelConfig == null ? cell.getPackNum() : channelConfig.getBatteryGroup();
        if (batteryGroup == null || cell.getBatNum() == null) {
            return;
        }
        Double resistanceRageSlip = connectResistanceCache.get(buildCellKey(batteryGroup, cell.getBatNum()));
        if (resistanceRageSlip != null) {
            cell.setResistanceRageSlip(resistanceRageSlip);
        }
    }

    private String buildCellKey(Integer batteryGroup, Integer batNum) {
        return batteryGroup + ":" + batNum;
    }
}
