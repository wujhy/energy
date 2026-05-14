package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 电池组兼容字段扩展填充服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Service
public class BatteryModuleGroupCompatibilityFillService {

    /**
     * 旧预估容量缓存服务，缺失时不影响600节实时采集。
     */
    @Autowired(required = false)
    private PreBatteryGroupService preBatteryGroupService;

    /**
     * 在轮询结束组计算后补充旧 pack_data 兼容字段。
     *
     * @param channelConfig 采集通道配置
     * @param group 电池组实时数据
     */
    public void fillAfterCalculation(BatteryCollectorChannelConfig channelConfig, BatteryModuleGroupRealtime group) {
        if (group == null) {
            return;
        }
        fillCalculatedAliases(group);
        fillStatus(group);
        fillCapacityCache(channelConfig, group);
    }

    private void fillCalculatedAliases(BatteryModuleGroupRealtime group) {
        group.setBatteryAvgTemperature(group.getAvgCellTemperature());
        group.setBatteryVoltageRange(group.getVoltageRange());
    }

    private void fillStatus(BatteryModuleGroupRealtime group) {
        if (group.getPackCurrent() == null) {
            return;
        }
        if (group.getPackCurrent() < 0) {
            group.setBatteryPackStatus(5);
        } else {
            group.setBatteryPackStatus(null);
        }
    }

    private void fillCapacityCache(BatteryCollectorChannelConfig channelConfig, BatteryModuleGroupRealtime group) {
        if (preBatteryGroupService == null
                || channelConfig == null
                || channelConfig.getBatteryGroup() == null) {
            return;
        }
        PreBatteryGroup preBatteryGroup = preBatteryGroupService.lastCache(channelConfig.getBatteryGroup());
        if (preBatteryGroup == null) {
            return;
        }
        if (preBatteryGroup.getBcapacity() != null) {
            group.setBcapacity(preBatteryGroup.getBcapacity());
        }
        if (preBatteryGroup.getBackUpDuration() != null) {
            group.setBackupDuration(preBatteryGroup.getBackUpDuration());
        }
        if (preBatteryGroup.getDischargeCapacity() != null) {
            group.setDisChargeCapacity(preBatteryGroup.getDischargeCapacity());
        }
        if (preBatteryGroup.getSoh() != null) {
            group.setBatteryPackSoh(preBatteryGroup.getSoh());
        }
    }
}
