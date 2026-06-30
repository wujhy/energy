package com.shanhe.project.collector.battery.realtime;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.capacity.vo.PreBatteryGroup;
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

    /** 旧预估容量缓存服务，缺失时不影响600节实时采集。 */
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

    /** 填充计算字段的旧协议别名。 */
    private void fillCalculatedAliases(BatteryModuleGroupRealtime group) {
        group.setBatteryAvgTemperature(group.getAvgCellTemperature());
        group.setBatteryVoltageRange(group.getVoltageRange());
    }

    /** 根据充放电电流和缺省规则设置电池组及内阻测试状态。 */
    private void fillStatus(BatteryModuleGroupRealtime group) {
        if (group.getPackCurrent() == null) {
            group.setBatteryPackStatus(null);
        } else if (group.getPackCurrent() < -0.1) {
            // BACKUP 备电
            group.setBatteryPackStatus(5);
        } else if (group.getPackCurrent() > 0.1) {
            // CHARGE 充电
            group.setBatteryPackStatus(1);
        } else {
            // MONITOR 监控
            group.setBatteryPackStatus(0);
        }

        if (group.getResistanceTestStatus() == null) {
            // NOT_TESTING 不在内阻测试
            group.setResistanceTestStatus(0);
        }
    }

    /** 从预估容量缓存补充兼容字段。 */
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
