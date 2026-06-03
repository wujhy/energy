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
        }
        appendCellDirectStatus(context, cells);
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

    /** 追加电池组通信状态告警。 */
    private void appendGroupDirectStatus(BatteryModuleAlarmContext context, BatteryModuleGroupRealtime group) {
        if (group.getGroupModuleFresh() != null) {
            context.putPackWarn(ItemCode.TXZT.getCode(), Boolean.TRUE.equals(group.getGroupModuleFresh()) ? "0" : "1");
        }
    }

    /** 追加单体漏液状态告警。 */
    private void appendCellDirectStatus(BatteryModuleAlarmContext context, List<BatteryModuleCellRealtime> cells) {
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
        if (timeoutStates != null && !timeoutStates.isEmpty()) {
            // 存在模块超时记录，标记该组有模块无响应
            context.putPackWarn(ItemCode.TXZT.getCode(), "1");
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
                if ("inactive".equals(state.getStateValue())) {
                    context.putPackWarn(ItemCode.TXZT.getCode(), "1");
                    return;
                }
            }
        }
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
