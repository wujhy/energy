package com.shanhe.project.collector.battery.service.impl;

import com.shanhe.project.collector.battery.mapper.BatteryDeviceStateMapper;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
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
        batteryDeviceStateMapper.upsert(state);
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
        batteryDeviceStateMapper.deleteByPackNum(packNum);
    }
}
