package com.shanhe.project.collector.battery.realtime;

import com.shanhe.framework.enums.ResistanceTestStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.capacity.vo.PreBatteryGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
    /** 测试/维护工作模式状态服务，缺失时按非测试兼容投影处理。 */
    @Autowired(required = false)
    private BatteryModeStatusService batteryModeStatusService;

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
        fillStatus(channelConfig, group);
        fillCapacityCache(channelConfig, group);
    }

    /** 填充计算字段的旧协议别名。 */
    private void fillCalculatedAliases(BatteryModuleGroupRealtime group) {
        group.setBatteryAvgTemperature(group.getAvgCellTemperature());
        group.setBatteryVoltageRange(group.getVoltageRange());
    }

    /** 根据充放电电流和缺省规则设置电池组及内阻测试状态。 */
    private void fillStatus(BatteryCollectorChannelConfig channelConfig, BatteryModuleGroupRealtime group) {
        Double current = group.getChargeDischargeCurrent();
        if (current == null) {
            group.setBatteryPackStatus(null);
        } else if (current < -0.1) {
            // BACKUP 备电
            group.setBatteryPackStatus(5);
        } else if (current > 0.1) {
            // CHARGE 充电
            group.setBatteryPackStatus(1);
        } else {
            // MONITOR 监控
            group.setBatteryPackStatus(0);
        }

        Integer packNum = resolvePackNum(channelConfig, group);
        if (isResistanceModeRunning(packNum)) {
            group.setResistanceTestStatus(Integer.valueOf(ResistanceTestStatusEnum.TESTING.getCode()));
            return;
        }
        if (group.getResistanceTestStatus() == null) {
            // NOT_TESTING 不在内阻测试
            group.setResistanceTestStatus(Integer.valueOf(ResistanceTestStatusEnum.NOT_TESTING.getCode()));
        }
    }

    private Integer resolvePackNum(BatteryCollectorChannelConfig channelConfig, BatteryModuleGroupRealtime group) {
        if (group.getPackNum() != null) {
            return group.getPackNum();
        }
        return channelConfig == null ? null : channelConfig.getBatteryGroup();
    }

    private boolean isResistanceModeRunning(Integer packNum) {
        if (batteryModeStatusService == null || packNum == null) {
            return false;
        }
        BatteryModeInfo modeInfo = batteryModeStatusService.get(packNum);
        return modeInfo != null
                && Objects.equals(modeInfo.getStatus(), BatteryModeStatusService.STATUS_RUNNING)
                && (Objects.equals(modeInfo.getMode(), BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                || Objects.equals(modeInfo.getMode(), BatteryModeStatusService.MODE_CONNECT_RESISTANCE));
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
