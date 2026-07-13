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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            appendChannelStatus(context, channelName);
            appendModuleTimeout(context, packNum, channelName);
            appendModuleActive(context, packNum, channelName);
            appendGroup246Freshness(context, packNum);
        } catch (Exception e) {
            log.warn("构建通信告警上下文失败, 电池组={}, 原因={}", packNum, e.getMessage());
        }
        return context;
    }

    /**
     * 从标准实时模型构建阈值告警参数。
     * <p>
     * 返回 itemCode → 当前值 的映射，供 AlarmLogServiceImpl.alarmBatteryValue 使用。
     * 缺失的实时值不会生成告警参数。
     *
     * @param packNum 电池组编号
     * @param cells 单体实时数据
     * @param group 组实时数据
     * @return 告警参数映射
     * @deprecated 仅保留给扁平单体或组级兼容输入；多单体告警必须使用 {@link #buildContext(BatteryModuleGroupRealtime, List)}
     * 按单体编号隔离，避免相同 itemCode 互相覆盖。
     */
    @Deprecated
    public Map<String, String> buildThresholdAlarmParam(Integer packNum,
                                                         List<BatteryModuleCellRealtime> cells,
                                                         BatteryModuleGroupRealtime group) {
        Map<String, String> warnParam = new HashMap<>(32);
        if (cells != null) {
            for (BatteryModuleCellRealtime cell : cells) {
                if (cell == null || cell.getBatNum() == null) {
                    continue;
                }
                appendCellThreshold(warnParam, cell);
            }
        }
        if (group != null) {
            appendGroupThreshold(warnParam, group);
        }
        return warnParam;
    }

    /** 追加单体阈值告警参数。 */
    private void appendCellThreshold(Map<String, String> warnParam, BatteryModuleCellRealtime cell) {
        // 单体电压
        if (cell.getVoltage() != null) {
            String value = String.valueOf(cell.getVoltage());
            warnParam.put(ItemCode.DTDYGC.getCode(), value);
            warnParam.put(ItemCode.DTDYGF.getCode(), value);
        }
        // 单体内阻
        if (cell.getResistance() != null) {
            String value = String.valueOf(cell.getResistance());
            warnParam.put(ItemCode.DTNZGD.getCode(), value);
            warnParam.put(ItemCode.DTNZGX.getCode(), value);
        }
        // 单体温度
        if (cell.getTemperature() != null) {
            String value = String.valueOf(cell.getTemperature());
            warnParam.put(ItemCode.DTDCWDG.getCode(), value);
            warnParam.put(ItemCode.DTDCWDD.getCode(), value);
        }
        // 单体鼓包
        if (cell.getSwollenVoltage() != null) {
            warnParam.put(ItemCode.DTGB.getCode(), String.valueOf(cell.getSwollenVoltage()));
        }
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

    /** 追加组阈值告警参数。 */
    private void appendGroupThreshold(Map<String, String> warnParam, BatteryModuleGroupRealtime group) {
        // 组电压
        Double groupVoltage = groupVoltage(group);
        if (groupVoltage != null) {
            String value = String.valueOf(groupVoltage);
            warnParam.put(ItemCode.ZDYGC.getCode(), value);
            warnParam.put(ItemCode.ZDYGF.getCode(), value);
        }
        // 充放电电流
        Double current = group.getChargeDischargeCurrent();
        if (current != null) {
            warnParam.put(ItemCode.ZCGDLGJ.getCode(), String.valueOf(current));
        }
        // 环境温度
        if (group.getEnvironmentTemperature1() != null) {
            String value = String.valueOf(group.getEnvironmentTemperature1());
            warnParam.put(ItemCode.ZWDG.getCode(), value);
            warnParam.put(ItemCode.ZWDD.getCode(), value);
        }
        if (group.getBatteryPackSoc() != null) {
            warnParam.put(ItemCode.ZSOCDGJ.getCode(), String.valueOf(group.getBatteryPackSoc()));
        }
        if (group.getBatteryPackSoh() != null) {
            warnParam.put(ItemCode.ZSOHDGJ.getCode(), String.valueOf(group.getBatteryPackSoh()));
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
        return group.getPackVoltage() != null ? group.getPackVoltage() : group.getExternalVoltage();
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
    private void appendChannelStatus(BatteryModuleAlarmContext context, String channelName) {
        if (channelName == null) {
            return;
        }
        // 通道关闭检测
        BatteryDeviceState channelOpen = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        if (channelOpen != null && BatteryDeviceStateConstants.StateLevel.ERROR.equals(channelOpen.getStateLevel())) {
            context.putPackWarn(ItemCode.DTTXZT.getCode(), "1");
            return;
        }
        // 通道异常检测（串口打开失败、读写异常）
        BatteryDeviceState channelError = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        if (channelError != null && BatteryDeviceStateConstants.StateLevel.ERROR.equals(channelError.getStateLevel())) {
            context.putPackWarn(ItemCode.DTTXZT.getCode(), "1");
        }
    }

    /** 追加模块超时告警（MODULE_TIMEOUT）。 */
    private void appendModuleTimeout(BatteryModuleAlarmContext context, Integer packNum, String channelName) {
        if (channelName == null) {
            return;
        }
        List<BatteryDeviceState> timeoutStates = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        if (timeoutStates == null || timeoutStates.isEmpty()) {
            return;
        }
        for (BatteryDeviceState state : timeoutStates) {
            if (state != null
                    && belongsToPack(state, packNum)
                    && !BatteryDeviceStateConstants.StateLevel.NORMAL.equals(state.getStateLevel())
                    && !"recovered".equals(state.getStateValue())) {
                // 存在模块超时记录，标记该组有模块无响应
                context.putPackWarn(ItemCode.TXZT.getCode(), "1");
                return;
            }
        }
    }

    /** 追加模块活跃状态告警（MODULE_ACTIVE=inactive）。 */
    private void appendModuleActive(BatteryModuleAlarmContext context, Integer packNum, String channelName) {
        if (channelName == null) {
            return;
        }
        List<BatteryDeviceState> inactiveStates = batteryDeviceStateService.selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        if (inactiveStates != null) {
            for (BatteryDeviceState state : inactiveStates) {
                if (state != null && belongsToPack(state, packNum) && "inactive".equals(state.getStateValue())) {
                    context.putPackWarn(ItemCode.TXZT.getCode(), "1");
                    return;
                }
            }
        }
    }

    /** 判断通道级模块状态是否属于当前电池组。 */
    private boolean belongsToPack(BatteryDeviceState state, Integer packNum) {
        return packNum != null && packNum.equals(state.getPackNum());
    }

    /** 追加 246 组模块新鲜度告警。 */
    private void appendGroup246Freshness(BatteryModuleAlarmContext context, Integer packNum) {
        if (packNum == null) {
            return;
        }
        BatteryDeviceState freshnessState = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
        if (freshnessState != null && "stale".equals(freshnessState.getStateValue())) {
            // 246 缺失：TXZT = 1 表示通信异常
            context.putPackWarn(ItemCode.TXZT.getCode(), "1");
        }
    }
}
