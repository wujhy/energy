package com.shanhe.project.collector.battery.runtime;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.service.BatteryCollectorProtocolLogService;
import com.shanhe.project.collector.battery.service.BatteryModulePollContextHolder;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeConsumer;
import com.shanhe.project.collector.battery.state.BatteryCollectorDeviceStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 蓄电池采集轮询循环编排服务。
 *
 * <p>只负责轮询编排：什么时候轮询、按什么顺序遍历地址、什么时候触发全量发现。
 * 不负责帧 I/O、命令队列、超时判断或状态持久化。</p>
 *
 * @author wjh
 * @since 2026-06-18
 */
@Slf4j
@Component
public class BatteryCollectorPollingService {

    @Resource
    private BatteryCollectorProperties properties;

    @Resource
    private BatteryCollectorProtocolLogService protocolLogService;

    @Resource
    private BatteryModuleRealtimeConsumer realtimeConsumer;
    @Resource
    private BatteryCollectorDeviceStateService collectorDeviceStateService;

    /**
     * 满足轮询间隔条件时触发一次轮询。
     *
     * @param state 通道状态
     * @param sendCommandAction 发送命令的回调（address → void）
     * @param waitForPendingAction 等待 pending 完成的回调
     * @param processQueuedCommandAction 处理排队命令的回调
     * @param checkTimeoutAction 检查超时的回调
     */
    public void pollIfNecessary(BatteryCollectorChannelState state,
                                 java.util.function.IntConsumer sendCommandAction,
                                 Runnable waitForPendingAction,
                                 java.util.function.Function<BatteryCollectorChannelState, Boolean> processQueuedCommandAction,
                                 Runnable checkTimeoutAction) {
        if (state.getPendingCommand() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        long gap = resolveRequestGapMs();
        if (state.getLastSendTime() > 0 && now - state.getLastSendTime() < gap) {
            return;
        }
        long interval = resolvePollIntervalMs(state.getConfig());
        if (state.getLastPollTime() > 0 && now - state.getLastPollTime() < interval) {
            return;
        }
        pollOnce(state, sendCommandAction, waitForPendingAction, processQueuedCommandAction, checkTimeoutAction);
        state.setLastPollTime(now);
    }

    /**
     * 执行一轮 01/81 全量或增量采集。
     *
     * @param state 通道状态
     * @param sendCommandAction 发送命令的回调
     * @param waitForPendingAction 等待 pending 完成的回调
     * @param processQueuedCommandAction 处理排队命令的回调
     * @param checkTimeoutAction 检查超时的回调
     */
    public void pollOnce(BatteryCollectorChannelState state,
                          java.util.function.IntConsumer sendCommandAction,
                          Runnable waitForPendingAction,
                          java.util.function.Function<BatteryCollectorChannelState, Boolean> processQueuedCommandAction,
                          Runnable checkTimeoutAction) {
        List<String> polledCommands = new ArrayList<>();
        List<String> completedCommands = new ArrayList<>();
        long startedAt = System.currentTimeMillis();
        boolean fullDiscovery = shouldRunFullDiscovery(state, startedAt);
        String batchNo = buildPollBatchNo(state, startedAt);
        state.setCurrentPollBatchNo(batchNo);
        state.setCurrentPollStartedAt(startedAt);
        state.setPollRoundCount(state.getPollRoundCount() + 1);
        state.setCurrentFullDiscovery(fullDiscovery);
        if (fullDiscovery) {
            state.setLastFullDiscoveryTime(startedAt);
        }
        BatteryModulePollContextHolder.set(BatteryModulePollContext.builder()
                .pollBatchNo(batchNo)
                .pollStartedAt(new Date(startedAt))
                .build());
        try {
            BatteryDeviceProtocolCode pollingCommand = BatteryDeviceProtocolCode.MODULE_INFO;
            int expectedCellCount = resolveExpectedCellCount(state.getConfig());
            int completedCellCount = 0;
            boolean skipRemainingCellAddresses = false;
            for (Integer address : resolvePollingAddresses(state, fullDiscovery)) {
                if (skipRemainingCellAddresses && isCellModuleAddress(address)) {
                    continue;
                }
                state.setCurrentPollAddress(address);
                polledCommands.add(String.format("%02X:%02X/%02X",
                        address,
                        pollingCommand.getRequestCode(),
                        pollingCommand.getResponseCode()));
                sendCommandAction.accept(address);
                waitForPendingAction.run();
                boolean responded = state.getPendingCommand() == null && !state.isLastPendingTimedOut();
                if (responded) {
                    completedCommands.add(String.format("%02X:%02X/%02X",
                            address,
                            pollingCommand.getRequestCode(),
                            pollingCommand.getResponseCode()));
                    if (isCellModuleAddress(address)) {
                        completedCellCount++;
                    }
                }
                if (shouldSkipRemainingCellDiscovery(fullDiscovery, address, completedCellCount, expectedCellCount)) {
                    skipRemainingCellAddresses = true;
                }
                // 处理排队的显式命令
                while (state.getPendingCommand() == null && !state.getQueuedModuleCommands().isEmpty()) {
                    if (!processQueuedCommandAction.apply(state)) {
                        break;
                    }
                    waitForPendingAction.run();
                }
            }
            realtimeConsumer.flushCurrentPollBatch(state.getConfig());
        } finally {
            BatteryModulePollContextHolder.clear();
            state.setCurrentPollAddress(0);
            state.setCurrentFullDiscovery(false);
        }
        protocolLogService.logPollSummary(state, fullDiscovery, polledCommands, completedCommands);
    }

    /**
     * 判断是否需要全量发现。
     */
    public boolean shouldRunFullDiscovery(BatteryCollectorChannelState state, long now) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return true;
        }
        if (state.getFullDiscoveryRequested().getAndSet(false)) {
            return true;
        }
        if (!hasActiveCellModuleAddress(state)) {
            return true;
        }
        Long interval = properties.getModuleAddressFullDiscoveryIntervalMs();
        return interval != null && interval > 0
                && state.getLastFullDiscoveryTime() > 0
                && now - state.getLastFullDiscoveryTime() >= interval;
    }

    /**
     * 解析本轮轮询的模块地址列表。
     */
    public List<Integer> resolvePollingAddresses(BatteryCollectorChannelState state, boolean fullDiscovery) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled()) || fullDiscovery) {
            return fullModuleAddressRange(state.getConfig());
        }
        List<Integer> activeAddresses = sortedActiveModuleAddresses(state);
        if (activeAddresses.isEmpty()) {
            state.getFullDiscoveryRequested().set(true);
            return fullModuleAddressRange(state.getConfig());
        }
        appendRequiredGroupModuleAddress(activeAddresses);
        return activeAddresses;
    }

    /**
     * 判断是否应跳过剩余单体地址发现。
     */
    public boolean shouldSkipRemainingCellDiscovery(boolean fullDiscovery,
                                                      Integer currentAddress,
                                                      int completedCellCount,
                                                      int expectedCellCount) {
        return fullDiscovery
                && isCellModuleAddress(currentAddress)
                && expectedCellCount > 0
                && completedCellCount >= expectedCellCount;
    }

    /**
     * 更新模块地址缓存：响应成功时加入/保留，连续未响应时移除。
     */
    public void updateModuleAddressCache(BatteryCollectorChannelState state, int address, boolean responded) {
        if (address == 246) {
            collectorDeviceStateService.persistGroup246Freshness(state.getConfig(), responded);
        }
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return;
        }
        if (!isCellModuleAddress(address)) {
            return;
        }
        if (responded) {
            state.getActiveModuleAddresses().add(address);
            state.getModuleAddressMissCounts().remove(address);
            collectorDeviceStateService.persistModuleActive(state.getConfig().getName(), state.getConfig(), address, true);
            collectorDeviceStateService.clearModuleTimeout(state.getConfig().getName(), state.getConfig(), address);
        } else {
            Integer missCount = state.getModuleAddressMissCounts().getOrDefault(address, 0);
            int threshold = resolveModuleAddressMissThreshold();
            if (threshold > 0 && missCount + 1 >= threshold) {
                state.getActiveModuleAddresses().remove(address);
                state.getModuleAddressMissCounts().remove(address);
                collectorDeviceStateService.persistModuleActive(state.getConfig().getName(), state.getConfig(), address, false);
            } else {
                state.getModuleAddressMissCounts().put(address, missCount + 1);
            }
        }
    }

    // --- 内部辅助方法 ---

    private boolean hasActiveCellModuleAddress(BatteryCollectorChannelState state) {
        for (Integer address : state.getActiveModuleAddresses()) {
            if (isCellModuleAddress(address)) {
                return true;
            }
        }
        return false;
    }

    private List<Integer> fullModuleAddressRange(BatteryCollectorChannelConfig config) {
        List<Integer> addresses = new ArrayList<>();
        int start = config.getModuleAddressStart() != null ? config.getModuleAddressStart() : 1;
        int end = config.getModuleAddressEnd() != null ? config.getModuleAddressEnd() : 246;
        for (int address = start; address <= end; address++) {
            addresses.add(address);
        }
        appendRequiredGroupModuleAddress(addresses);
        return addresses;
    }

    private List<Integer> sortedActiveModuleAddresses(BatteryCollectorChannelState state) {
        List<Integer> addresses = new ArrayList<>(state.getActiveModuleAddresses());
        Collections.sort(addresses);
        return addresses;
    }

    private void appendRequiredGroupModuleAddress(List<Integer> addresses) {
        int groupModuleAddress = 246;
        if (!addresses.contains(groupModuleAddress)) {
            addresses.add(groupModuleAddress);
            Collections.sort(addresses);
        }
    }

    private boolean isCellModuleAddress(Integer address) {
        return address != null && address >= 1 && address <= 245;
    }

    private String buildPollBatchNo(BatteryCollectorChannelState state, long startedAt) {
        String channelName = state.getConfig() == null ? "channel" : state.getConfig().getName();
        return channelName + "-" + startedAt;
    }

    private int resolveExpectedCellCount(BatteryCollectorChannelConfig config) {
        Integer count = config.getExpectedCellCount();
        if (count == null || count <= 0) {
            return 0;
        }
        return Math.min(count, 245);
    }

    int resolveRequestGapMs() {
        return resolvePositiveInt(properties.getRequestGapMs(), 120);
    }

    long resolvePollIntervalMs(BatteryCollectorChannelConfig config) {
        Long value = config == null ? null : config.getPollIntervalMs();
        return resolvePositiveLong(value, 3000L);
    }

    int resolveModuleAddressMissThreshold() {
        return resolvePositiveInt(properties.getModuleAddressMissThreshold(), 3);
    }

    private int resolvePositiveInt(Number value, int defaultValue) {
        if (value == null || value.longValue() <= 0 || value.longValue() > Integer.MAX_VALUE) {
            return defaultValue;
        }
        return value.intValue();
    }

    private long resolvePositiveLong(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
