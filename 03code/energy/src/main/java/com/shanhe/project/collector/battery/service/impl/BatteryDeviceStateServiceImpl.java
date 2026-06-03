package com.shanhe.project.collector.battery.service.impl;

import com.shanhe.project.collector.battery.mapper.BatteryDeviceStateMapper;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池设备状态服务实现。
 *
 * @author wjh
 * @since 2026-05-29
 */
@Slf4j
@Service
public class BatteryDeviceStateServiceImpl implements BatteryDeviceStateService {

    @Resource
    private BatteryDeviceStateMapper batteryDeviceStateMapper;

    @Override
    public void upsert(BatteryDeviceState state) {
        if (state == null) {
            return;
        }
        if (isBlank(state.getScopeType()) || isBlank(state.getScopeKey()) || isBlank(state.getStateCode())) {
            log.warn("设备状态写入被跳过, scopeType/scopeKey/stateCode 不能为空, scopeType={}, scopeKey={}, stateCode={}",
                    state.getScopeType(), state.getScopeKey(), state.getStateCode());
            return;
        }
        batteryDeviceStateMapper.upsert(state);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public BatteryDeviceState selectByScope(String scopeType, String scopeKey, String stateCode) {
        return batteryDeviceStateMapper.selectByScope(scopeType, scopeKey, stateCode);
    }

    @Override
    public List<BatteryDeviceState> selectByPackAndCode(Integer packNum, String stateCode) {
        return batteryDeviceStateMapper.selectByPackAndCode(packNum, stateCode);
    }

    @Override
    public List<BatteryDeviceState> selectByChannelAndCode(String channelName, String stateCode) {
        return batteryDeviceStateMapper.selectByChannelAndCode(channelName, stateCode);
    }

    @Override
    public List<BatteryDeviceState> selectByPackNum(Integer packNum) {
        return batteryDeviceStateMapper.selectByPackNum(packNum);
    }

    @Override
    public List<BatteryDeviceState> selectList(BatteryDeviceState state) {
        return batteryDeviceStateMapper.selectList(state);
    }

    @Override
    public void deleteByStateId(Long stateId) {
        batteryDeviceStateMapper.deleteByStateId(stateId);
    }

    @Override
    public void deleteByScope(String scopeType, String scopeKey) {
        batteryDeviceStateMapper.deleteByScope(scopeType, scopeKey);
    }

    @Override
    public int deleteExpired() {
        return batteryDeviceStateMapper.deleteExpired();
    }

    @Override
    public void deleteByPackNum(Integer packNum) {
        if (packNum == null) {
            return;
        }
        batteryDeviceStateMapper.deleteByPackNum(packNum);
    }

    @Override
    public void deleteAll() {
        batteryDeviceStateMapper.deleteAll();
    }

    @Override
    public List<BatteryDeviceState> getPackStatusSummary(Integer packNum) {
        if (packNum == null) {
            return java.util.Collections.emptyList();
        }
        List<BatteryDeviceState> result = new java.util.ArrayList<>();
        // 工作模式
        BatteryDeviceState workMode = selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.WORK_MODE);
        if (workMode != null) {
            result.add(workMode);
        }
        // 在线状态
        BatteryDeviceState online = selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.ONLINE);
        if (online != null) {
            result.add(online);
        }
        // 246 新鲜度
        BatteryDeviceState freshness = selectByScope(
                BatteryDeviceStateConstants.ScopeType.PACK, String.valueOf(packNum),
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
        if (freshness != null) {
            result.add(freshness);
        }
        return result;
    }

    @Override
    public List<BatteryDeviceState> getChannelStatusSummary(String channelName) {
        if (channelName == null || channelName.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<BatteryDeviceState> result = new java.util.ArrayList<>();
        // 通道串口状态
        BatteryDeviceState channelOpen = selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        if (channelOpen != null) {
            result.add(channelOpen);
        }
        // 通道异常状态
        BatteryDeviceState channelError = selectByScope(
                BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        if (channelError != null) {
            result.add(channelError);
        }
        // 超时模块列表
        List<BatteryDeviceState> timeouts = selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        if (timeouts != null) {
            result.addAll(timeouts);
        }
        // 活跃模块列表
        List<BatteryDeviceState> active = selectByChannelAndCode(
                channelName, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        if (active != null) {
            result.addAll(active);
        }
        return result;
    }
}
