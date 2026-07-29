package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池设备状态告警上下文服务。
 *
 * <p>只消费 battery_device_state 中的通讯、在线和模块状态，不参与实时阈值告警构建。</p>
 *
 * @author wjh
 * @since 2026-07-29
 */
@Slf4j
@Service
public class BatteryAlarmStateContextService {

    /** 电池设备状态服务。 */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /**
     * 根据 battery_device_state 通信状态构建设备故障告警候选。
     *
     * @param packNum 电池组编号
     * @param channelName 通道名称
     * @return 告警适配上下文
     */
    public BatteryAlarmEvaluationContext buildCommunicationAlarmContext(Integer packNum, String channelName) {
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.setPackNum(packNum);
        try {
            Boolean channelAlarm = appendChannelStatus(context, channelName);
            if (Boolean.FALSE.equals(channelAlarm)) {
                context.putPackStatusWarn(ItemCode.DTTXZT.getCode(), "0");
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
                context.putPackStatusWarn(ItemCode.TXZT.getCode(), "0");
            }
        } catch (Exception e) {
            log.warn("构建通信告警上下文失败, 电池组={}, 原因={}", packNum, e.getMessage());
        }
        return context;
    }

    /** 追加通道串口状态告警（CHANNEL_OPEN + CHANNEL_ERROR）。 */
    private Boolean appendChannelStatus(BatteryAlarmEvaluationContext context, String channelName) {
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
                context.putPackStatusWarn(ItemCode.DTTXZT.getCode(), "1");
                return true;
            }
        }
        BatteryDeviceState channelError = batteryDeviceStateService.selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        if (channelError != null) {
            hasSignal = true;
            if (BatteryDeviceStateConstants.StateLevel.ERROR.equals(channelError.getStateLevel())) {
                context.putPackStatusWarn(ItemCode.DTTXZT.getCode(), "1");
                return true;
            }
        }
        return hasSignal ? Boolean.FALSE : null;
    }

    /** 追加模块超时告警（MODULE_TIMEOUT）。 */
    private Boolean appendModuleTimeout(BatteryAlarmEvaluationContext context, Integer packNum, String channelName) {
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
                context.putPackStatusWarn(ItemCode.TXZT.getCode(), "1");
                return true;
            }
        }
        return hasSignal ? Boolean.FALSE : null;
    }

    /** 追加模块活跃状态告警（MODULE_ACTIVE=inactive）。 */
    private Boolean appendModuleActive(BatteryAlarmEvaluationContext context, Integer packNum, String channelName) {
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
                context.putPackStatusWarn(ItemCode.TXZT.getCode(), "1");
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
    private Boolean appendGroup246Freshness(BatteryAlarmEvaluationContext context, Integer packNum) {
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
            context.putPackStatusWarn(ItemCode.TXZT.getCode(), "1");
            return true;
        }
        return false;
    }

    /** 追加电池组在线状态告警（ONLINE=offline）。 */
    private Boolean appendOnlineStatus(BatteryAlarmEvaluationContext context, Integer packNum) {
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
            context.putPackStatusWarn(ItemCode.TXZT.getCode(), "1");
            return true;
        }
        if (BatteryDeviceStateConstants.StateLevel.NORMAL.equals(onlineState.getStateLevel())
                && "online".equals(onlineState.getStateValue())) {
            return false;
        }
        return null;
    }
}