package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 蓄电池采集缓存维护服务，负责模块地址缓存、实时快照淘汰和状态去重缓存清理。
 *
 * @author wjh
 * @since 2026-06-16
 */
@Service
public class BatteryCollectorCacheService {

    /** 按通道重置模块地址缓存。 */
    public boolean resetModuleAddressCache(List<BatteryCollectorChannelState> channelStates,
                                           BatteryModuleRealtimeSnapshotService realtimeSnapshotService,
                                           String channelName) {
        boolean matched = false;
        for (BatteryCollectorChannelState state : safeStates(channelStates)) {
            if (channelName == null || channelName.trim().isEmpty()
                    || channelName.equals(state.getConfig().getName())) {
                resetModuleAddressCache(state, realtimeSnapshotService);
                matched = true;
            }
        }
        return matched;
    }

    /** 按电池组重置模块地址缓存。 */
    public boolean resetModuleAddressCacheByBatteryGroup(List<BatteryCollectorChannelState> channelStates,
                                                         BatteryModuleRealtimeSnapshotService realtimeSnapshotService,
                                                         Integer batteryGroup) {
        boolean matched = false;
        for (BatteryCollectorChannelState state : safeStates(channelStates)) {
            BatteryCollectorChannelConfig config = state == null ? null : state.getConfig();
            if (batteryGroup == null || (config != null && Objects.equals(batteryGroup, config.getBatteryGroup()))) {
                resetModuleAddressCache(state, realtimeSnapshotService);
                matched = true;
            }
        }
        return matched;
    }

    /** 清理设备状态去重缓存。 */
    public int clearDeviceStateDedupCacheByBatteryGroup(Map<String, String> lastStateValues,
                                                        List<BatteryCollectorChannelState> channelStates,
                                                        Integer batteryGroup) {
        if (lastStateValues == null) {
            return 0;
        }
        if (batteryGroup == null) {
            int size = lastStateValues.size();
            lastStateValues.clear();
            return size;
        }
        int before = lastStateValues.size();
        String packPrefix = batteryGroup + ":";
        List<String> channelPrefixes = new ArrayList<>();
        for (BatteryCollectorChannelState state : safeStates(channelStates)) {
            BatteryCollectorChannelConfig config = state == null ? null : state.getConfig();
            if (config != null && Objects.equals(batteryGroup, config.getBatteryGroup())
                    && config.getName() != null && !config.getName().trim().isEmpty()) {
                channelPrefixes.add(config.getName() + ":");
            }
        }
        lastStateValues.keySet().removeIf(key ->
                key != null && (key.startsWith(packPrefix) || startsWithAny(key, channelPrefixes)));
        return before - lastStateValues.size();
    }

    /** 重置单个通道的模块地址缓存，并淘汰对应的标准实时快照。 */
    public void resetModuleAddressCache(BatteryCollectorChannelState state,
                                       BatteryModuleRealtimeSnapshotService realtimeSnapshotService) {
        if (state == null) {
            return;
        }
        state.getActiveModuleAddresses().clear();
        state.getModuleAddressMissCounts().clear();
        state.getFullDiscoveryRequested().set(true);
        clearRealtimeSnapshot(state, realtimeSnapshotService);
    }

    /** 清理指定通道的实时快照。 */
    public void clearRealtimeSnapshot(BatteryCollectorChannelState state,
                                     BatteryModuleRealtimeSnapshotService realtimeSnapshotService) {
        if (realtimeSnapshotService == null || state == null || state.getConfig() == null) {
            return;
        }
        realtimeSnapshotService.evict(state.getConfig().getBatteryGroup());
    }

    private List<BatteryCollectorChannelState> safeStates(List<BatteryCollectorChannelState> states) {
        return states == null ? Collections.emptyList() : new ArrayList<>(states);
    }

    private boolean startsWithAny(String value, List<String> prefixes) {
        if (value == null || prefixes == null || prefixes.isEmpty()) {
            return false;
        }
        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
