package com.shanhe.project.collector.battery.state;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.GROUP_MODULE_ADDRESS;

/**
 * 蓄电池采集设备状态服务，负责设备状态入库、状态去重和去重缓存清理。
 *
 * @author wjh
 * @since 2026-06-16
 */
@Slf4j
@Service
public class BatteryCollectorDeviceStateService {

    /** 设备状态码：串口状态。 */
    private static final String STATE_CODE_SERIAL_PORT = BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN;
    /** 设备状态码：轮询超时计数。 */
    private static final String STATE_CODE_POLL_TIMEOUT = BatteryDeviceStateConstants.StateCode.CHANNEL_TIMEOUT_COUNT;
    /** 去重缓存上限，超过后清理模块和pack级高基数条目。 */
    private static final int DEDUP_CACHE_LIMIT = 1000;

    /**
     * 设备状态持久化服务。
     */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /**
     * 状态去重缓存：key = scopeKey + stateCode，value = 上次写入的 stateValue。
     */
    private final ConcurrentHashMap<String, String> lastStateValues = new ConcurrentHashMap<>();

    /**
     * 持久化通道异常到 battery_device_state。
     *
     * @param state 通道运行态
     * @param e 异常
     */
    public void persistChannelError(BatteryCollectorChannelState state, Exception e) {
        String channelName = state.getConfig().getName();
        String stateValue = e.getMessage() == null ? "unknown" : e.getMessage();
        BatteryDeviceState ds = buildChannelState(channelName, state.getConfig(),
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                stateValue,
                BatteryDeviceStateConstants.StateLevel.ERROR, null);
        persistIfChanged(channelName, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, stateValue, ds);
    }

    /**
     * 持久化通道串口状态；通道打开时清除之前的通道异常状态。
     *
     * @param state 通道运行态
     * @param opened 串口是否打开
     */
    public void persistSerialPortState(BatteryCollectorChannelState state, boolean opened) {
        String stateValue = opened ? "open" : "closed";
        String stateLevel = opened ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.ERROR;
        persistChannelStateThrottled(state.getConfig().getName(), state.getConfig(),
                STATE_CODE_SERIAL_PORT, stateValue, stateLevel, null);
        if (opened) {
            clearChannelError(state.getConfig().getName(), state.getConfig());
        }
    }

    /**
     * 持久化通道轮询超时计数。
     *
     * @param state 通道运行态
     */
    public void persistPollTimeout(BatteryCollectorChannelState state) {
        String stateValue = String.valueOf(state.getTimeoutCount());
        String stateLevel = state.getTimeoutCount() > 0
                ? BatteryDeviceStateConstants.StateLevel.WARN
                : BatteryDeviceStateConstants.StateLevel.NORMAL;
        persistChannelStateThrottled(state.getConfig().getName(), state.getConfig(),
                STATE_CODE_POLL_TIMEOUT, stateValue, stateLevel, null);
    }

    /**
     * 持久化具体模块超时状态。
     *
     * @param state 通道运行态
     * @param pendingRequest 超时的待响应请求
     */
    public void persistModuleTimeout(BatteryCollectorChannelState state, BatteryPendingRequest pendingRequest) {
        if (state == null || pendingRequest == null) {
            return;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        int address = pendingRequest.getRequestAddress();
        if (address < 0 || address > 255) {
            return;
        }
        String scopeKey = config.getName() + ":" + address;
        String stateValue = String.format("%02X/%02X",
                pendingRequest.getRequestCode(),
                pendingRequest.getResponseCode());
        BatteryDeviceState ds = buildModuleState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT,
                stateValue,
                BatteryDeviceStateConstants.StateLevel.WARN,
                pendingRequest.getOptLogId(),
                address);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, stateValue, ds);
    }

    /**
     * 模块重新响应时清除超时状态。
     *
     * @param channelName 通道名称
     * @param config 通道配置
     * @param address 模块地址
     */
    public void clearModuleTimeout(String channelName, BatteryCollectorChannelConfig config, int address) {
        String scopeKey = channelName + ":" + address;
        String cacheKey = scopeKey + ":" + BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT;
        if (lastStateValues.containsKey(cacheKey)) {
            BatteryDeviceState ds = buildModuleState(scopeKey, config,
                    BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "recovered",
                    BatteryDeviceStateConstants.StateLevel.NORMAL, null, address);
            persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "recovered", ds);
        }
    }

    /**
     * 持久化模块活跃状态。
     *
     * @param channelName 通道名称
     * @param config 通道配置
     * @param address 模块地址
     * @param active 是否活跃
     */
    public void persistModuleActive(String channelName, BatteryCollectorChannelConfig config,
                                    int address, boolean active) {
        String scopeKey = channelName + ":" + address;
        String stateValue = active ? "active" : "inactive";
        String stateLevel = active ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.WARN;
        BatteryDeviceState ds = buildModuleState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE,
                stateValue, stateLevel, null, address);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, stateValue, ds);
    }

    /**
     * 持久化246组模块新鲜度状态。
     *
     * @param config 通道配置
     * @param fresh 是否新鲜
     */
    public void persistGroup246Freshness(BatteryCollectorChannelConfig config, boolean fresh) {
        String scopeKey = String.valueOf(config.getBatteryGroup());
        String stateValue = fresh ? "fresh" : "stale";
        String stateLevel = fresh ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.WARN;
        BatteryDeviceState ds = buildChannelState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, stateValue, stateLevel, null);
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, stateValue, ds);
    }

    /**
     * 按电池组清理设备状态去重缓存。
     *
     * @param channelStates 通道运行态列表
     * @param batteryGroup 电池组编号；为空时清理全部
     * @return 清理条数
     */
    public int clearDedupCacheByBatteryGroup(List<BatteryCollectorChannelState> channelStates, Integer batteryGroup) {
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

    /**
     * 判断去重缓存key是否属于高基数状态。
     *
     * @param cacheKey 去重缓存key
     * @return true 表示可在缓存超过阈值时优先清理
     */
    public boolean isHighCardinalityStateCacheKey(String cacheKey) {
        if (cacheKey == null) {
            return false;
        }
        return cacheKey.contains(":" + BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT)
                || cacheKey.contains(":" + BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE)
                || cacheKey.endsWith(":" + BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
    }

    /** 通道重新打开时，清除之前的异常状态。 */
    private void clearChannelError(String channelName, BatteryCollectorChannelConfig config) {
        String cacheKey = channelName + ":" + BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR;
        if (lastStateValues.containsKey(cacheKey)) {
            BatteryDeviceState ds = buildChannelState(channelName, config,
                    BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "cleared",
                    BatteryDeviceStateConstants.StateLevel.NORMAL, null);
            persistIfChanged(channelName, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "cleared", ds);
        }
    }

    /** 通道级状态写入，带去重。 */
    private void persistChannelStateThrottled(String channelName, BatteryCollectorChannelConfig config,
                                              String stateCode, String stateValue, String stateLevel, Long optLogId) {
        String scopeKey = channelName;
        BatteryDeviceState ds = buildChannelState(scopeKey, config, stateCode, stateValue, stateLevel, optLogId);
        persistIfChanged(scopeKey, stateCode, stateValue, ds);
    }

    /** 仅当 stateValue 变化时写库，避免重复写入。 */
    private void persistIfChanged(String scopeKey, String stateCode, String stateValue, BatteryDeviceState ds) {
        String cacheKey = scopeKey + ":" + stateCode;
        String previous = lastStateValues.get(cacheKey);
        if (stateValue.equals(previous)) {
            return;
        }
        try {
            batteryDeviceStateService.upsert(ds);
            lastStateValues.put(cacheKey, stateValue);
            if (lastStateValues.size() > DEDUP_CACHE_LIMIT) {
                lastStateValues.keySet().removeIf(this::isHighCardinalityStateCacheKey);
            }
        } catch (Exception e) {
            log.warn("持久化设备状态失败, scopeKey={}, stateCode={}, 原因={}", scopeKey, stateCode, e.getMessage());
        }
    }

    /** 构造通道级 BatteryDeviceState 对象。 */
    private BatteryDeviceState buildChannelState(String scopeKey, BatteryCollectorChannelConfig config,
                                                 String stateCode, String stateValue, String stateLevel, Long optLogId) {
        BatteryDeviceState ds = new BatteryDeviceState();
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.CHANNEL);
        ds.setScopeKey(scopeKey);
        ds.setChannelName(config.getName());
        ds.setPackNum(config.getBatteryGroup());
        ds.setStateCode(stateCode);
        ds.setStateValue(stateValue);
        ds.setStateLevel(stateLevel);
        ds.setSource(BatteryDeviceStateConstants.Source.COLLECTOR);
        ds.setOptLogId(optLogId);
        ds.setFirstSeenTime(new Date());
        ds.setLastChangeTime(new Date());
        return ds;
    }

    /** 构造模块级 BatteryDeviceState 对象。 */
    private BatteryDeviceState buildModuleState(String scopeKey, BatteryCollectorChannelConfig config,
                                                String stateCode, String stateValue, String stateLevel,
                                                Long optLogId, int address) {
        BatteryDeviceState ds = buildChannelState(scopeKey, config, stateCode, stateValue, stateLevel, optLogId);
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.MODULE);
        ds.setSourceRefId(String.valueOf(address));
        if (isCellModuleAddress(address)) {
            ds.setModelNum(address);
        }
        return ds;
    }

    /** 判断是否为单体模块地址。 */
    private boolean isCellModuleAddress(int address) {
        return address >= 1 && address < GROUP_MODULE_ADDRESS;
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

    Map<String, String> getLastStateValues() {
        return lastStateValues;
    }
}
