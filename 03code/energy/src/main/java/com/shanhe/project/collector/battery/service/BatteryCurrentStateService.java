package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCurrentAlarmSummary;
import com.shanhe.project.collector.battery.model.BatteryCurrentCellState;
import com.shanhe.project.collector.battery.model.BatteryCurrentGroupState;
import com.shanhe.project.collector.battery.model.BatteryCurrentState;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.service.OptLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一的电池当前状态查询服务
 *
 * @author wjh
 * @since 2026-06-15
 */
@Service
public class BatteryCurrentStateService {

    /** M460不支持的告警类型及原因说明列表，用于在状态接口中告知前端。 */
    private static final List<String> UNSUPPORTED_ALARM_REASONS = Arrays.asList(
            "M460_THERMAL_RUNAWAY: no energy realtime data source",
            "M460_HYDROGEN: no confirmed energy realtime data source",
            "M460_NETWORK: represented only by collector communication state when available",
            "M460_SENSOR: no independent sensor source in energy realtime model"
    );

    /** 电池组配置服务。 */
    @Resource
    private IBatteryPackService batteryPackService;
    /** 实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;
    /** 电池设备状态服务。 */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** 电池模式状态服务。 */
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

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
        fillSnapshotDiagnosticFields(state, snapshot);
        BatteryModuleGroupRealtime group = snapshot == null ? null : snapshot.getGroup();
        List<BatteryModuleCellRealtime> cells = snapshot == null ? Collections.emptyList() : snapshot.getCells();
        state.setGroup(toGroupState(group));
        state.setCells(toCellStates(cells));
        state.setDeviceStates(batteryDeviceStateService == null
                ? Collections.emptyList() : safeStates(batteryDeviceStateService.selectByPackNum(packNum)));
        state.setAlarms(toAlarmSummaries(alarmLogService == null
                ? Collections.emptyList() : alarmLogService.selectBatteryAlarmLogListCache(packNum)));
        state.setUnsupportedAlarmReasons(UNSUPPORTED_ALARM_REASONS);
        state.setRunningOptLogs(optLogService == null
                ? Collections.emptyList() : optLogService.selectRunningList(packNum));
        state.setModeInfo(batteryModeStatusService == null ? null : batteryModeStatusService.get(packNum));
        if (!state.getRunningOptLogs().isEmpty()) {
            OptLog latest = state.getRunningOptLogs().get(0);
            state.setLastCommandErrorMessage(latest.getErrorMessage());
            state.setLastCommandStatus(latest.getStatus());
        }
        state.setLastPollBatchNo(resolveLastPollBatchNo(group, cells));
        state.setFreshness(resolveFreshness(pack.getBatSinSize(), group, cells));
        return state;
    }

    private void fillSnapshotDiagnosticFields(BatteryCurrentState state, BatteryModuleRealtimeSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        state.setSnapshotRefreshedAt(snapshot.getRefreshedAt());
        state.setSnapshotDataReady(snapshot.isDataReady());
        state.setSnapshotFresh(snapshot.isFresh());
        state.setCurrentBatchCellNums(sortedIntegers(snapshot.getCurrentBatchCellNums()));
        state.setStaleCellNums(sortedIntegers(snapshot.getStaleCellNums()));
        state.setMissingCellNums(sortedIntegers(snapshot.getMissingCellNums()));
        state.setCellMissCounts(sortedMissCounts(snapshot.getCellMissCounts()));
    }

    private List<Integer> sortedIntegers(Iterable<Integer> values) {
        List<Integer> result = new ArrayList<>();
        if (values != null) {
            for (Integer value : values) {
                if (value != null) {
                    result.add(value);
                }
            }
        }
        result.sort(Integer::compareTo);
        return result;
    }

    private Map<Integer, Integer> sortedMissCounts(Map<Integer, Integer> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<Integer, Integer> result = new LinkedHashMap<>();
        for (Integer key : sortedIntegers(source.keySet())) {
            result.put(key, source.get(key));
        }
        return result;
    }

    private String resolveFreshness(Integer expectedCellCount,
                                    BatteryModuleGroupRealtime group,
                                    List<BatteryModuleCellRealtime> cells) {
        boolean noData = group == null && (cells == null || cells.isEmpty());
        if (noData) {
            return BatteryCurrentState.FRESHNESS_NOT_COLLECTED;
        }
        boolean isStale = group != null && Boolean.FALSE.equals(group.getDataFresh());
        if (isStale) {
            return BatteryCurrentState.FRESHNESS_STALE;
        }
        boolean isPartial = expectedCellCount != null && expectedCellCount > 0
                && cells != null && cells.size() < expectedCellCount;
        if (isPartial) {
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
        target.setPackCurrent(source.getChargeDischargeCurrent());
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
        target.setMaxVoltageBatNum(source.getMaxVoltageBatNum());
        target.setMaxCellVoltage(source.getMaxCellVoltage());
        target.setMinVoltageBatNum(source.getMinVoltageBatNum());
        target.setMinCellVoltage(source.getMinCellVoltage());
        target.setAvgCellVoltage(source.getAvgCellVoltage());
        target.setVoltageRange(source.getVoltageRange());
        target.setMaxTemperatureBatNum(source.getMaxTemperatureBatNum());
        target.setMaxCellTemperature(source.getMaxCellTemperature());
        target.setMinTemperatureBatNum(source.getMinTemperatureBatNum());
        target.setMinCellTemperature(source.getMinCellTemperature());
        target.setAvgCellTemperature(source.getAvgCellTemperature());
        target.setTemperatureRange(source.getTemperatureRange());
        target.setMaxResistanceBatNum(source.getMaxResistanceBatNum());
        target.setMaxInternalResistance(source.getMaxInternalResistance());
        target.setMinResistanceBatNum(source.getMinResistanceBatNum());
        target.setMinInternalResistance(source.getMinInternalResistance());
        target.setAvgInternalResistance(source.getAvgInternalResistance());
        target.setResistanceRange(source.getResistanceRange());
        target.setBatteryPackSoc(source.getBatteryPackSoc());
        target.setBatteryPackSoh(source.getBatteryPackSoh());
        target.setBackupDuration(source.getBackupDuration());
        target.setBcapacity(source.getBcapacity());
        target.setCapacity(source.getCapacity());
        target.setDisChargeCapacity(source.getDisChargeCapacity());
        target.setDisChargeDuration(source.getDisChargeDuration());
        target.setResidualDischargeDuration(source.getResidualDischargeDuration());
        target.setRippleVoltage(source.getRippleVoltage());
        target.setHydrogenConcentration(source.getHydrogenConcentration());
        target.setPositiveInsulationResistance(source.getPositiveInsulationResistance());
        target.setNegativeInsulationResistance(source.getNegativeInsulationResistance());
        target.setGroundingBatteryUpperLimit(source.getGroundingBatteryUpperLimit());
        target.setGroundingBatteryLowerLimit(source.getGroundingBatteryLowerLimit());
        target.setMaxResistanceRateChangeBatNum(source.getMaxResistanceRateChangeBatNum());
        target.setMaxResistanceRateChange(source.getMaxResistanceRateChange());
        target.setBatteryPackStatus(source.getBatteryPackStatus());
        target.setResistanceTestStatus(source.getResistanceTestStatus());
        target.setDeviceWorkStatus(source.getDeviceWorkStatus());
        target.setDeviceWorkIoStatus(source.getDeviceWorkIoStatus());
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
            target.setConnectResistanceStatus(source.getResistanceRageSlip() != null ? "OK" : null);
            target.setResistanceRateChange(source.getResistanceRateChange());
            target.setSwollenVoltage(source.getSwollenVoltage());
            target.setLeakageStatus(source.getLeakageStatus());
            target.setCreateTime(source.getCreateTime());
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
