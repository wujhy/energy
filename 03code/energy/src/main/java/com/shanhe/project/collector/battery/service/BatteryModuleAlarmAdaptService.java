package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 600节模块端实时数据告警适配服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Slf4j
@Service
public class BatteryModuleAlarmAdaptService {

    /** 电池设备状态服务。 */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /**
     * 根据当前轮采集结果构建告警候选。
     *
     * @param group 组实时数据
     * @param cells 单体实时数据
     * @return 告警适配上下文
     */
    public BatteryModuleAlarmContext buildContext(BatteryModuleGroupRealtime group,
                                                  List<BatteryModuleCellRealtime> cells) {
        BatteryModuleAlarmContext context = new BatteryModuleAlarmContext();
        if (group != null) {
            context.setPackNum(group.getPackNum());
            appendGroupDirectStatus(context, group);
            appendGroupThreshold(context, group);
        }
        appendCellStatus(context, cells);
        return context;
    }

    /**
     * 根据 battery_device_state 通信状态构建设备故障告警候选。
     *
     * @param packNum 电池组编号
     * @param channelName 通道名称
     * @return 告警适配上下文
     */
    public BatteryModuleAlarmContext buildCommunicationAlarmContext(Integer packNum, String channelName) {
        BatteryModuleAlarmContext context = new BatteryModuleAlarmContext();
        context.setPackNum(packNum);
        try {
            Boolean channelAlarm = appendChannelStatus(context, channelName);
            if (Boolean.FALSE.equals(channelAlarm)) {
                context.putPackWarn(ItemCode.DTTXZT.getCode(), "0");
            }
            Boolean onlineAlarm = appendOnlineStatus(context, packNum);
            Boolean moduleTimeoutAlarm = appendModuleTimeout(context, packNum, channelName);
            Boolean moduleActiveAlarm = appendModuleActive(context, packNum, channelName);
            Boolean groupFreshnessAlarm = appendGroup246Freshness(context, packNum);
            boolean hasPackSignal = onlineAlarm != null || moduleTimeoutAlarm != null
                    || moduleActiveAlarm != null || groupFreshnessAlarm != null;
            boolean packCommunicationAlarm = Boolean.TRUE.equals(onlineAlarm)
                    || Boolean.TRUE.equals(moduleTimeoutAlarm)
                    || Boolean.TRUE.equals(moduleActiveAlarm)
                    || Boolean.TRUE.equals(groupFreshnessAlarm);
            if (hasPackSignal && !packCommunicationAlarm) {
                context.putPackWarn(ItemCode.TXZT.getCode(), "0");
            }
        } catch (Exception e) {
            log.warn("构建通信告警上下文失败, 电池组={}, 原因={}", packNum, e.getMessage());
        }
        return context;
    }

    /** 追加单体阈值告警候选，按单体编号隔离 itemCode，避免多个单体互相覆盖。 */
    private void appendCellThreshold(BatteryModuleAlarmContext context, BatteryModuleCellRealtime cell) {
        Integer batNum = cell.getBatNum();
        if (cell.getVoltage() != null) {
            String value = String.valueOf(cell.getVoltage());
            context.putCellWarn(batNum, ItemCode.DTDYGC.getCode(), value);
            context.putCellWarn(batNum, ItemCode.DTDYGF.getCode(), value);
        }
        if (cell.getResistance() != null) {
            String value = String.valueOf(cell.getResistance());
            context.putCellWarn(batNum, ItemCode.DTNZGD.getCode(), value);
            context.putCellWarn(batNum, ItemCode.DTNZGX.getCode(), value);
        }
        if (cell.getTemperature() != null) {
            String value = String.valueOf(cell.getTemperature());
            context.putCellWarn(batNum, ItemCode.DTDCWDG.getCode(), value);
            context.putCellWarn(batNum, ItemCode.DTDCWDD.getCode(), value);
        }
        if (cell.getSwollenVoltage() != null) {
            context.putCellWarn(batNum, ItemCode.DTGB.getCode(), String.valueOf(cell.getSwollenVoltage()));
        }
    }

    /** 追加组阈值告警候选。 */
    private void appendGroupThreshold(BatteryModuleAlarmContext context, BatteryModuleGroupRealtime group) {
        Double groupVoltage = groupVoltage(group);
        if (groupVoltage != null) {
            String value = String.valueOf(groupVoltage);
            context.putPackWarn(ItemCode.ZDYGC.getCode(), value);
            context.putPackWarn(ItemCode.ZDYGF.getCode(), value);
        }
        Double current = group.getChargeDischargeCurrent();
        if (current != null) {
            context.putPackWarn(ItemCode.ZCGDLGJ.getCode(), String.valueOf(current));
        }
        if (group.getEnvironmentTemperature1() != null) {
            String value = String.valueOf(group.getEnvironmentTemperature1());
            context.putPackWarn(ItemCode.ZWDG.getCode(), value);
            context.putPackWarn(ItemCode.ZWDD.getCode(), value);
        }
        if (group.getBatteryPackSoc() != null) {
            context.putPackWarn(ItemCode.ZSOCDGJ.getCode(), String.valueOf(group.getBatteryPackSoc()));
        }
        if (group.getBatteryPackSoh() != null) {
            context.putPackWarn(ItemCode.ZSOHDGJ.getCode(), String.valueOf(group.getBatteryPackSoh()));
        }
    }

    private Double groupVoltage(BatteryModuleGroupRealtime group) {
        if (group.getPackVoltage() != null) {
            return group.getPackVoltage();
        }
        if (group.getBatteryPackOuterVoltage() != null) {
            return group.getBatteryPackOuterVoltage();
        }
        return group.getExternalVoltage();
    }

    /** 追加电池组通信状态告警。 */
    private void appendGroupDirectStatus(BatteryModuleAlarmContext context, BatteryModuleGroupRealtime group) {
        if (group.getGroupModuleFresh() != null) {
            context.putPackWarn(ItemCode.TXZT.getCode(), Boolean.TRUE.equals(group.getGroupModuleFresh()) ? "0" : "1");
        }
    }

    /** 追加单体直接状态和阈值告警。 */
    private void appendCellStatus(BatteryModuleAlarmContext context, List<BatteryModuleCellRealtime> cells) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null) {
                continue;
            }
            if (cell.getLeakageStatus() != null) {
                context.putCellWarn(cell.getBatNum(), ItemCode.DTLYGJ.getCode(), toAlarmValue(cell.getLeakageStatus()));
            }
            appendCellThreshold(context, cell);
        }
    }

    /** 将状态值转换为告警值字符串。 */
    private String toAlarmValue(Integer value) {
        return value != null && value == 1 ? "1" : "0";
    }

    /** 追加通道串口状态告警（CHANNEL_OPEN + CHANNEL_ERROR）。 */
    private Boolean appendChannelStatus(BatteryModuleAlarmContext context, String channelName) {
        if (channelName == null) {
            return null;
        }
        boolean hasSignal = false;
        BatteryDeviceState channelOpen = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        if (channelOpen != null) {
            hasSignal = true;
            if (BatteryDeviceStateConstants.StateLevel.ERROR.equals(channelOpen.getStateLevel())) {
                context.putPackWarn(ItemCode.DTTXZT.getCode(), "1");
                return true;
            }
        }
        BatteryDeviceState channelError = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        if (channelError != null) {
            hasSignal = true;
            if (BatteryDeviceStateConstants.StateLevel.ERROR.equals(channelError.getStateLevel())) {
                context.putPackWarn(ItemCode.DTTXZT.getCode(), "1");
                return true;
            }
        }
        return hasSignal ? Boolean.FALSE : null;
    }

    /** 追加模块超时告警（MODULE_TIMEOUT）。 */
    private Boolean appendModuleTimeout(BatteryModuleAlarmContext context, Integer packNum, String channelName) {
        if (channelName == null) {
            return null;
        }
        List<BatteryDeviceState> timeoutStates = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        if (timeoutStates == null || timeoutStates.isEmpty()) {
            return null;
        }
        boolean hasSignal = false;
        for (BatteryDeviceState state : timeoutStates) {
            if (state == null || !belongsToPack(state, packNum)) {
                continue;
            }
            hasSignal = true;
            if (!BatteryDeviceStateConstants.StateLevel.NORMAL.equals(state.getStateLevel())
                    && !"recovered".equals(state.getStateValue())) {
                context.putPackWarn(ItemCode.TXZT.getCode(), "1");
                return true;
            }
        }
        return hasSignal ? Boolean.FALSE : null;
    }

    /** 追加模块活跃状态告警（MODULE_ACTIVE=inactive）。 */
    private Boolean appendModuleActive(BatteryModuleAlarmContext context, Integer packNum, String channelName) {
        if (channelName == null) {
            return null;
        }
        List<BatteryDeviceState> activeStates = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        if (activeStates == null || activeStates.isEmpty()) {
            return null;
        }
        boolean hasSignal = false;
        for (BatteryDeviceState state : activeStates) {
            if (state == null || !belongsToPack(state, packNum)) {
                continue;
            }
            hasSignal = true;
            if ("inactive".equals(state.getStateValue())) {
                context.putPackWarn(ItemCode.TXZT.getCode(), "1");
                return true;
            }
        }
        return hasSignal ? Boolean.FALSE : null;
    }

    /** 判断通道级模块状态是否属于当前电池组。 */
    private boolean belongsToPack(BatteryDeviceState state, Integer packNum) {
        return packNum != null && packNum.equals(state.getPackNum());
    }

    /** 追加 246 组模块新鲜度告警。 */
    private Boolean appendGroup246Freshness(BatteryModuleAlarmContext context, Integer packNum) {
        if (packNum == null) {
            return null;
        }
        BatteryDeviceState freshnessState = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
        if (freshnessState == null) {
            return null;
        }
        if ("stale".equals(freshnessState.getStateValue())) {
            context.putPackWarn(ItemCode.TXZT.getCode(), "1");
            return true;
        }
        return false;
    }

    /** 追加电池组在线状态告警（ONLINE=offline）。 */
    private Boolean appendOnlineStatus(BatteryModuleAlarmContext context, Integer packNum) {
        if (packNum == null) {
            return null;
        }
        BatteryDeviceState onlineState = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.ONLINE);
        if (onlineState == null) {
            return null;
        }
        if (BatteryDeviceStateConstants.StateLevel.WARN.equals(onlineState.getStateLevel())
                && "offline".equals(onlineState.getStateValue())) {
            context.putPackWarn(ItemCode.TXZT.getCode(), "1");
            return true;
        }
        if (BatteryDeviceStateConstants.StateLevel.NORMAL.equals(onlineState.getStateLevel())
                && "online".equals(onlineState.getStateValue())) {
            return false;
        }
        return null;
    }
}
