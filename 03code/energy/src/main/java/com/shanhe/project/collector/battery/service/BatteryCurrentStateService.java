package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCurrentAlarmSummary;
import com.shanhe.project.collector.battery.model.BatteryCurrentCellState;
import com.shanhe.project.collector.battery.model.BatteryCurrentGroupState;
import com.shanhe.project.collector.battery.model.BatteryCurrentState;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.OptLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Unified current battery state query service.
 *
 * @author wjh
 * @since 2026-06-15
 */
@Service
public class BatteryCurrentStateService {

    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private OptLogService optLogService;

    public BatteryCurrentState getCurrentState(Integer packNum) {
        BatteryCurrentState state = new BatteryCurrentState();
        state.setPackNum(packNum);
        BatteryPack pack = packNum == null || batteryPackService == null
                ? null : batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (pack == null) {
            state.setFreshness(BatteryCurrentState.FRESHNESS_NO_CONFIG);
            return state;
        }
        state.setPackId(pack.getPackId());
        state.setExpectedCellCount(pack.getBatSinSize());

        BatteryModuleRealtimeSnapshot snapshot = snapshotService == null ? null : snapshotService.getCachedSnapshot(packNum);
        BatteryModuleGroupRealtime group = snapshot == null ? readGroup(packNum) : snapshot.getGroup();
        List<BatteryModuleCellRealtime> cells = snapshot == null ? readCells(packNum) : snapshot.getCells();
        state.setGroup(toGroupState(group));
        state.setCells(toCellStates(cells));
        state.setDeviceStates(batteryDeviceStateService == null
                ? Collections.emptyList() : safeStates(batteryDeviceStateService.selectByPackNum(packNum)));
        state.setAlarms(toAlarmSummaries(alarmLogService == null
                ? Collections.emptyList() : alarmLogService.selectBatteryAlarmLogListCache(packNum)));
        state.setRunningOptLogs(optLogService == null
                ? Collections.emptyList() : optLogService.selectRunningList(packNum));
        state.setLastPollBatchNo(resolveLastPollBatchNo(group, cells));
        state.setFreshness(resolveFreshness(pack.getBatSinSize(), group, cells));
        return state;
    }

    private BatteryModuleGroupRealtime readGroup(Integer packNum) {
        return realtimeMapper == null ? null : realtimeMapper.selectGroup(packNum);
    }

    private List<BatteryModuleCellRealtime> readCells(Integer packNum) {
        return realtimeMapper == null ? Collections.emptyList() : safeCells(realtimeMapper.selectCells(packNum));
    }

    private String resolveFreshness(Integer expectedCellCount,
                                    BatteryModuleGroupRealtime group,
                                    List<BatteryModuleCellRealtime> cells) {
        if (group == null && (cells == null || cells.isEmpty())) {
            return BatteryCurrentState.FRESHNESS_NOT_COLLECTED;
        }
        if (group != null && Boolean.FALSE.equals(group.getDataFresh())) {
            return BatteryCurrentState.FRESHNESS_STALE;
        }
        if (expectedCellCount != null && expectedCellCount > 0
                && cells != null && cells.size() < expectedCellCount) {
            return BatteryCurrentState.FRESHNESS_PARTIAL;
        }
        return BatteryCurrentState.FRESHNESS_FRESH;
    }

    private String resolveLastPollBatchNo(BatteryModuleGroupRealtime group, List<BatteryModuleCellRealtime> cells) {
        if (group != null && group.getPollBatchNo() != null) {
            return group.getPollBatchNo();
        }
        if (cells == null || cells.isEmpty()) {
            return null;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell != null && cell.getPollBatchNo() != null) {
                return cell.getPollBatchNo();
            }
        }
        return null;
    }

    private BatteryCurrentGroupState toGroupState(BatteryModuleGroupRealtime source) {
        if (source == null) {
            return null;
        }
        BatteryCurrentGroupState target = new BatteryCurrentGroupState();
        target.setPackNum(source.getPackNum());
        target.setPackVoltage(source.getPackVoltage());
        target.setPackCurrent(source.getPackCurrent());
        target.setChargeDischargeCurrent(source.getChargeDischargeCurrent());
        target.setFloatCurrent(source.getFloatCurrent());
        target.setExternalVoltage(source.getExternalVoltage());
        target.setEnvironmentTemperature1(source.getEnvironmentTemperature1());
        target.setEnvironmentTemperature2(source.getEnvironmentTemperature2());
        target.setPollBatchNo(source.getPollBatchNo());
        target.setPollStartedAt(source.getPollStartedAt());
        target.setCellCount(source.getCellCount());
        target.setOnlineCellCount(source.getOnlineCellCount());
        target.setStaleCellCount(source.getStaleCellCount());
        target.setDataFresh(source.getDataFresh());
        target.setLatestCellUpdateTime(source.getLatestCellUpdateTime());
        target.setLatestGroupUpdateTime(source.getLatestGroupUpdateTime());
        target.setGroupModuleFresh(source.getGroupModuleFresh());
        target.setBatteryPackStatus(source.getBatteryPackStatus());
        target.setResistanceTestStatus(source.getResistanceTestStatus());
        target.setDeviceWorkStatus(source.getDeviceWorkStatus());
        return target;
    }

    private List<BatteryCurrentCellState> toCellStates(List<BatteryModuleCellRealtime> sources) {
        List<BatteryCurrentCellState> targets = new ArrayList<>();
        for (BatteryModuleCellRealtime source : safeCells(sources)) {
            BatteryCurrentCellState target = new BatteryCurrentCellState();
            target.setPackNum(source.getPackNum());
            target.setBatNum(source.getBatNum());
            target.setVoltage(source.getVoltage());
            target.setResistance(source.getResistance());
            target.setTemperature(source.getTemperature());
            target.setCapacity(source.getCapacity());
            target.setResistanceRageSlip(source.getResistanceRageSlip());
            target.setResistanceRateChange(source.getResistanceRateChange());
            target.setSwollenVoltage(source.getSwollenVoltage());
            target.setLeakageStatus(source.getLeakageStatus());
            target.setPollBatchNo(source.getPollBatchNo());
            target.setPollStartedAt(source.getPollStartedAt());
            targets.add(target);
        }
        targets.sort(Comparator.comparing(BatteryCurrentCellState::getBatNum,
                Comparator.nullsLast(Integer::compareTo)));
        return targets;
    }

    private List<BatteryCurrentAlarmSummary> toAlarmSummaries(List<AlarmLog> sources) {
        List<BatteryCurrentAlarmSummary> targets = new ArrayList<>();
        if (sources == null) {
            return targets;
        }
        for (AlarmLog source : sources) {
            if (source == null) {
                continue;
            }
            BatteryCurrentAlarmSummary target = new BatteryCurrentAlarmSummary();
            target.setAlarmId(source.getAlarmId());
            target.setPackNum(source.getPackNum());
            target.setModelNum(source.getModelNum());
            target.setItemCode(source.getItemCode());
            target.setAlarmLevel(source.getAlarmLevel());
            target.setDataInfo(source.getDataInfo());
            target.setStatus(source.getStatus());
            target.setCreateTime(source.getCreateTime());
            target.setUpdateTime(source.getUpdateTime());
            targets.add(target);
        }
        return targets;
    }

    private List<BatteryModuleCellRealtime> safeCells(List<BatteryModuleCellRealtime> cells) {
        return cells == null ? Collections.emptyList() : cells;
    }

    private List<BatteryDeviceState> safeStates(List<BatteryDeviceState> states) {
        return states == null ? Collections.emptyList() : states;
    }
}
