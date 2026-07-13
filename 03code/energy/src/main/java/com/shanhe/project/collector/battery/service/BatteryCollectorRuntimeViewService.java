package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorMetrics;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 蓄电池采集运行态视图服务，负责构造状态快照和指标 DTO。
 *
 * @author wjh
 * @since 2026-06-16
 */
@Service
public class BatteryCollectorRuntimeViewService {

    /** 构造采集通道状态快照列表。 */
    public List<BatteryCollectorChannelSnapshot> getChannelSnapshots(List<BatteryCollectorChannelState> states) {
        List<BatteryCollectorChannelSnapshot> snapshots = new ArrayList<>();
        for (BatteryCollectorChannelState state : safeStates(states)) {
            snapshots.add(buildSnapshot(state));
        }
        return snapshots;
    }

    /** 构造采集运行指标，并聚合各通道的实时快照统计。 */
    public BatteryCollectorMetrics getMetrics(List<BatteryCollectorChannelState> states,
                                              BatteryModuleRealtimeSnapshotService realtimeSnapshotService) {
        BatteryCollectorMetrics metrics = new BatteryCollectorMetrics();
        metrics.setGeneratedAt(System.currentTimeMillis());
        metrics.setChannelCount(0);
        metrics.setEnabledChannelCount(0);
        metrics.setOpenedChannelCount(0);
        metrics.setRunningChannelCount(0);
        metrics.setTotalActiveModuleAddressCount(0);
        metrics.setTotalQueuedModuleCommandCount(0);
        metrics.setTotalTimeoutCount(0);
        metrics.setTotalSnapshotCellCount(0);
        metrics.setTotalSnapshotStaleCellCount(0);
        metrics.setTotalSnapshotMissingCellCount(0);
        List<BatteryCollectorChannelMetrics> channels = new ArrayList<>();
        for (BatteryCollectorChannelState state : safeStates(states)) {
            BatteryCollectorChannelMetrics channel = buildChannelMetrics(state, realtimeSnapshotService);
            channels.add(channel);
            metrics.setChannelCount(metrics.getChannelCount() + 1);
            if (Boolean.TRUE.equals(channel.getEnabled())) {
                metrics.setEnabledChannelCount(metrics.getEnabledChannelCount() + 1);
            }
            if (Boolean.TRUE.equals(channel.getOpened())) {
                metrics.setOpenedChannelCount(metrics.getOpenedChannelCount() + 1);
            }
            if (channel.getRunState() != null) {
                metrics.setRunningChannelCount(metrics.getRunningChannelCount() + 1);
            }
            metrics.setTotalActiveModuleAddressCount(metrics.getTotalActiveModuleAddressCount()
                    + safeInt(channel.getActiveModuleAddressCount()));
            metrics.setTotalQueuedModuleCommandCount(metrics.getTotalQueuedModuleCommandCount()
                    + safeInt(channel.getQueuedModuleCommandCount()));
            metrics.setTotalTimeoutCount(metrics.getTotalTimeoutCount() + safeInt(channel.getTimeoutCount()));
            metrics.setTotalSnapshotCellCount(metrics.getTotalSnapshotCellCount()
                    + safeInt(channel.getSnapshotCellCount()));
            metrics.setTotalSnapshotStaleCellCount(metrics.getTotalSnapshotStaleCellCount()
                    + safeInt(channel.getSnapshotStaleCellCount()));
            metrics.setTotalSnapshotMissingCellCount(metrics.getTotalSnapshotMissingCellCount()
                    + safeInt(channel.getSnapshotMissingCellCount()));
        }
        metrics.setChannels(channels);
        return metrics;
    }

    /** 构造单个通道的运行指标。 */
    BatteryCollectorChannelMetrics buildChannelMetrics(BatteryCollectorChannelState state,
                                                       BatteryModuleRealtimeSnapshotService realtimeSnapshotService) {
        BatteryCollectorChannelMetrics metrics = new BatteryCollectorChannelMetrics();
        if (state == null) {
            return metrics;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        metrics.setName(config == null ? null : config.getName());
        metrics.setBatteryGroup(config == null ? null : config.getBatteryGroup());
        metrics.setEnabled(config == null ? null : Boolean.TRUE.equals(config.getEnabled()));
        metrics.setOpened(Boolean.TRUE.equals(state.getOpened().get())
                && state.getSerialPort() != null
                && state.getSerialPort().isOpen());
        metrics.setRunState(state.getRunState());
        metrics.setLastReceiveTime(state.getLastReceiveTime());
        metrics.setLastSendTime(state.getLastSendTime());
        metrics.setLastPollTime(state.getLastPollTime());
        metrics.setLastTimeoutTime(state.getLastTimeoutTime());
        metrics.setTimeoutCount(state.getTimeoutCount());
        metrics.setCurrentRetryCount(state.getCurrentRetryCount());
        metrics.setCurrentPollBatchNo(state.getCurrentPollBatchNo());
        metrics.setCurrentPollStartedAt(state.getCurrentPollStartedAt());
        metrics.setCurrentPollElapsedMs(elapsedSince(state.getCurrentPollStartedAt()));
        metrics.setCurrentPollAddress(state.getCurrentPollAddress());
        metrics.setPollRoundCount(state.getPollRoundCount());
        metrics.setCurrentFullDiscovery(state.isCurrentFullDiscovery());
        metrics.setLastFullDiscoveryTime(state.getLastFullDiscoveryTime());
        metrics.setActiveModuleAddressCount(state.getActiveModuleAddresses().size());
        metrics.setQueuedModuleCommandCount(state.getQueuedModuleCommands().size());
        metrics.setLastCompletedModuleCommandName(state.getLastCompletedModuleCommandName());
        metrics.setLastCompletedModuleCommandSuccess(state.isLastCompletedModuleCommandSuccess());
        metrics.setLastCompletedModuleCommandTime(state.getLastCompletedModuleCommandTime());
        metrics.setReceiveBufferSize(state.getReceiveBuffer().size());
        metrics.setChannelHealth(resolveChannelHealth(metrics, state));
        BatteryPendingRequest pendingRequest = state.getPendingCommand();
        if (pendingRequest != null) {
            metrics.setPendingCommandName(pendingRequest.getName());
            metrics.setPendingAutoPoll(pendingRequest.isAutoPoll());
        }
        fillSnapshotMetrics(metrics, config, realtimeSnapshotService);
        return metrics;
    }

    /** 构造单个通道的运行状态快照。 */
    BatteryCollectorChannelSnapshot buildSnapshot(BatteryCollectorChannelState state) {
        BatteryCollectorChannelSnapshot snapshot = new BatteryCollectorChannelSnapshot();
        if (state == null) {
            return snapshot;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        snapshot.setName(config == null ? null : config.getName());
        snapshot.setPortName(config == null ? null : config.getPortName());
        snapshot.setBatteryGroup(config == null ? null : config.getBatteryGroup());
        snapshot.setDeviceAddress(config == null ? null : config.getDeviceAddress());
        snapshot.setOpened(Boolean.TRUE.equals(state.getOpened().get())
                && state.getSerialPort() != null
                && state.getSerialPort().isOpen());
        snapshot.setRunState(state.getRunState());
        snapshot.setLastReceiveTime(state.getLastReceiveTime());
        snapshot.setLastSendTime(state.getLastSendTime());
        snapshot.setLastPollTime(state.getLastPollTime());
        snapshot.setLastTimeoutTime(state.getLastTimeoutTime());
        snapshot.setTimeoutCount(state.getTimeoutCount());
        snapshot.setCurrentRetryCount(state.getCurrentRetryCount());
        snapshot.setLastRequestCode(state.getLastRequestCode());
        snapshot.setExpectedResponseCode(state.getExpectedResponseCode());
        snapshot.setLastResponseCode(state.getLastResponseCode());
        snapshot.setLastPendingCompletedAt(state.getLastPendingCompletedAt());
        snapshot.setLastPendingTimedOut(state.isLastPendingTimedOut());
        snapshot.setLastCompletedModuleCommandName(state.getLastCompletedModuleCommandName());
        snapshot.setLastCompletedModuleResponseCode(state.getLastCompletedModuleResponseCode());
        snapshot.setLastCompletedModuleCommandSuccess(state.isLastCompletedModuleCommandSuccess());
        snapshot.setLastCompletedModuleCommandTime(state.getLastCompletedModuleCommandTime());
        snapshot.setCurrentPollBatchNo(state.getCurrentPollBatchNo());
        snapshot.setCurrentPollStartedAt(state.getCurrentPollStartedAt());
        snapshot.setCurrentPollAddress(state.getCurrentPollAddress());
        snapshot.setPollRoundCount(state.getPollRoundCount());
        snapshot.setCurrentFullDiscovery(state.isCurrentFullDiscovery());
        snapshot.setLastFullDiscoveryTime(state.getLastFullDiscoveryTime());
        List<Integer> activeAddresses = sortedActiveModuleAddresses(state);
        snapshot.setActiveModuleAddressCount(activeAddresses.size());
        snapshot.setActiveModuleAddresses(activeAddresses.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        snapshot.setReceiveBufferSize(state.getReceiveBuffer().size());
        BatteryPendingRequest pendingRequest = state.getPendingCommand();
        if (pendingRequest != null) {
            snapshot.setPendingCommandName(pendingRequest.getName());
            snapshot.setPendingRequestCode(pendingRequest.getRequestCode());
            snapshot.setPendingResponseCode(pendingRequest.getResponseCode());
            snapshot.setPendingAutoPoll(pendingRequest.isAutoPoll());
        }
        snapshot.setQueuedModuleCommandCount(state.getQueuedModuleCommands().size());
        return snapshot;
    }

    /** 从标准实时快照缓存补充当前通道的数据新鲜度指标。 */
    private void fillSnapshotMetrics(BatteryCollectorChannelMetrics metrics,
                                     BatteryCollectorChannelConfig config,
                                     BatteryModuleRealtimeSnapshotService realtimeSnapshotService) {
        if (metrics == null || realtimeSnapshotService == null || config == null || config.getBatteryGroup() == null) {
            return;
        }
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService.getCachedSnapshot(config.getBatteryGroup());
        if (snapshot == null) {
            return;
        }
        metrics.setSnapshotCellCount(snapshot.getCells().size());
        metrics.setSnapshotCurrentBatchCellCount(snapshot.getCurrentBatchCellNums().size());
        metrics.setSnapshotStaleCellCount(snapshot.getStaleCellNums().size());
        metrics.setSnapshotMissingCellCount(snapshot.getMissingCellNums().size());
        metrics.setSnapshotHitRate(snapshotHitRate(config, snapshot));
        metrics.setSnapshotAgeMs(snapshotAgeMs(snapshot));
        metrics.setSnapshotPollBatchNo(snapshot.getPollBatchNo());
        metrics.setSnapshotPollStartedAt(snapshot.getPollStartedAt());
        metrics.setSnapshotRefreshedAt(snapshot.getRefreshedAt());
        metrics.setSnapshotDataReady(snapshot.isDataReady());
    }

    private Long elapsedSince(long startedAt) {
        return startedAt <= 0 ? null : Math.max(0L, System.currentTimeMillis() - startedAt);
    }

    private Long snapshotAgeMs(BatteryModuleRealtimeSnapshot snapshot) {
        return snapshot.getRefreshedAt() == null ? null
                : Math.max(0L, System.currentTimeMillis() - snapshot.getRefreshedAt().getTime());
    }

    private Double snapshotHitRate(BatteryCollectorChannelConfig config, BatteryModuleRealtimeSnapshot snapshot) {
        Integer expected = config == null ? null : config.getExpectedCellCount();
        if (expected == null || expected <= 0) {
            expected = snapshot.getBatSinSize();
        }
        if (expected == null || expected <= 0) {
            return null;
        }
        return Math.min(1.0d, snapshot.getCurrentBatchCellNums().size() * 1.0d / expected);
    }

    private String resolveChannelHealth(BatteryCollectorChannelMetrics metrics, BatteryCollectorChannelState state) {
        if (!Boolean.TRUE.equals(metrics.getEnabled())) {
            return "DISABLED";
        }
        if (!Boolean.TRUE.equals(metrics.getOpened())) {
            return "CLOSED";
        }
        if (state.isLastPendingTimedOut()) {
            return "PENDING_TIMEOUT";
        }
        if (safeInt(metrics.getTimeoutCount()) > 0) {
            return "TIMEOUTS";
        }
        return "OK";
    }

    /** 拷贝通道状态列表，避免遍历时受并发修改影响。 */
    private List<BatteryCollectorChannelState> safeStates(List<BatteryCollectorChannelState> states) {
        return states == null ? Collections.emptyList() : new ArrayList<>(states);
    }

    /** 返回排序后的活跃模块地址，用于稳定展示。 */
    private List<Integer> sortedActiveModuleAddresses(BatteryCollectorChannelState state) {
        List<Integer> addresses = new ArrayList<>(state.getActiveModuleAddresses());
        Collections.sort(addresses);
        return addresses;
    }

    /** 空值按 0 聚合。 */
    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
