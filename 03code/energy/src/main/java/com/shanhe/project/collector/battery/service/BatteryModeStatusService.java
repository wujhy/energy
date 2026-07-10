package com.shanhe.project.collector.battery.service;

import cn.hutool.core.util.ObjUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 蓄电池测试/维护工作模式缓存服务。
 *
 * @author wjh
 * @since 2026/5/13
 */
@Slf4j
@Service
public class BatteryModeStatusService {

    /** 工作模式对应的设备状态编码。 */
    private static final String STATE_CODE_WORK_MODE = BatteryDeviceStateConstants.StateCode.WORK_MODE;

    /** 电池设备状态服务。 */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /** 无测试。 */
    public static final int MODE_IDLE = 0;
    /** 自动编号。 */
    public static final int MODE_AUTO_MODEL_NUM = 1;
    /** 内阻测试。 */
    public static final int MODE_INTERNAL_RESISTANCE = 6;
    /** 连接条电阻测试。 */
    public static final int MODE_CONNECT_RESISTANCE = 10;
    /** 单体均衡。 */
    public static final int MODE_BALANCE = 11;

    /** 工作模式状态值：停止。 */
    private static final int STATUS_STOP = 0;
    /** 工作模式状态值：运行中。 */
    private static final int STATUS_RUNNING = 1;

    /** 缓存键枚举。 */
    public final CacheKeyEnum cacheKeyEnum = CacheKeyEnum.MODE_STATUS;

    private CacheAccessor cacheAccessor = new CacheUtilsAccessor();

    /** 获取当前蓄电池测试/维护工作模式状态。 */
    public BatteryModeInfo get(Integer packNum) {
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), cacheKeyEnum.getKey());
        if (result instanceof BatteryModeInfo) {
            return (BatteryModeInfo) result;
        }
        return idle(packNum);
    }

    /** 清除指定电池组的工作模式缓存。 */
    public void clear(Integer packNum) {
        if (packNum == null) {
            cacheAccessor.remove(cacheKeyEnum.getCache(), cacheKeyEnum.getKey());
            return;
        }
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), cacheKeyEnum.getKey());
        if (result instanceof BatteryModeInfo) {
            BatteryModeInfo batteryModeInfo = (BatteryModeInfo) result;
            if (ObjUtil.equals(packNum, batteryModeInfo.getPackNum())) {
                cacheAccessor.remove(cacheKeyEnum.getCache(), cacheKeyEnum.getKey());
            }
        }
    }

    /** 标记指定电池组进入测试/维护运行状态。 */
    public void markRunning(Integer packNum, int mode, Integer address) {
        markRunning(packNum, mode, address, null);
    }

    /** 标记指定电池组进入测试/维护运行状态，并关联操作日志。 */
    public void markRunning(Integer packNum, int mode, Integer address, Long optLogId) {
        BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
        batteryModeInfo.setPackNum(packNum);
        batteryModeInfo.setResult(0);
        batteryModeInfo.setMode(mode);
        batteryModeInfo.setStatus(STATUS_RUNNING);
        batteryModeInfo.setAddress(address);
        cacheAccessor.put(cacheKeyEnum.getCache(), cacheKeyEnum.getKey(), batteryModeInfo);
        persistModeState(packNum, mode, address, String.valueOf(mode),
                BatteryDeviceStateConstants.StateLevel.RUNNING, optLogId);
    }

    /** 标记指定电池组测试/维护已停止。 */
    public void markStopped(Integer packNum, int mode, Integer address, boolean success) {
        markStopped(packNum, mode, address, success, null);
    }

    /** 标记指定电池组测试/维护已停止，并关联操作日志。 */
    public void markStopped(Integer packNum, int mode, Integer address, boolean success, Long optLogId) {
        BatteryModeInfo previous = getStored();
        BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
        batteryModeInfo.setPackNum(packNum);
        batteryModeInfo.setResult(success ? 0 : 1);
        batteryModeInfo.setMode(MODE_IDLE);
        batteryModeInfo.setStatus(STATUS_STOP);
        batteryModeInfo.setAddress(address);
        if (previous != null) {
            batteryModeInfo.setLastPackNum(previous.getLastPackNum());
            batteryModeInfo.setLastMode(previous.getLastMode());
            batteryModeInfo.setLastAddress(previous.getAddress());
        }
        if (batteryModeInfo.getLastPackNum() == null) {
            batteryModeInfo.setLastPackNum(packNum);
        }
        if (batteryModeInfo.getLastMode() == null) {
            batteryModeInfo.setLastMode(mode);
        }
        cacheAccessor.put(cacheKeyEnum.getCache(), cacheKeyEnum.getKey(), batteryModeInfo);
        persistModeState(packNum, MODE_IDLE, address, String.valueOf(MODE_IDLE),
                success ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.WARN,
                optLogId);
    }


    /** 将工作模式状态持久化到 battery_device_state。 */
    private void persistModeState(Integer packNum, int mode, Integer address, String stateValue,
                                  String stateLevel, Long optLogId) {
        if (batteryDeviceStateService == null) {
            return;
        }
        try {
            BatteryDeviceState state = new BatteryDeviceState();
            state.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
            state.setScopeKey(String.valueOf(packNum));
            state.setPackNum(packNum);
            state.setStateCode(STATE_CODE_WORK_MODE);
            state.setStateValue(stateValue);
            state.setStateLevel(stateLevel);
            state.setSource(BatteryDeviceStateConstants.Source.MODE_STATUS);
            state.setSourceRefId(address == null ? null : String.valueOf(address));
            state.setMode(mode);
            state.setOptLogId(optLogId);
            state.setFirstSeenTime(new Date());
            state.setLastChangeTime(new Date());
            batteryDeviceStateService.upsert(state);
        } catch (Exception e) {
            log.warn("持久化工作模式状态失败, 电池组={}, 模式={}, 原因={}", packNum, mode, e.getMessage());
        }
    }

    /** 从缓存获取已存储的工作模式信息。 */
    private BatteryModeInfo getStored() {
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), cacheKeyEnum.getKey());
        return result instanceof BatteryModeInfo ? (BatteryModeInfo) result : null;
    }

    /** 构造空闲状态的工作模式信息。 */
    private BatteryModeInfo idle(Integer packNum) {
        BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
        batteryModeInfo.setPackNum(packNum);
        batteryModeInfo.setMode(MODE_IDLE);
        batteryModeInfo.setResult(0);
        batteryModeInfo.setStatus(STATUS_STOP);
        return batteryModeInfo;
    }

    interface CacheAccessor {
        Object get(String cacheName, String key);

        void put(String cacheName, String key, Object value);

        void remove(String cacheName, String key);
    }

    private static class CacheUtilsAccessor implements CacheAccessor {
        @Override
        public Object get(String cacheName, String key) {
            return CacheUtils.get(cacheName, key);
        }

        @Override
        public void put(String cacheName, String key, Object value) {
            CacheUtils.put(cacheName, key, value);
        }

        @Override
        public void remove(String cacheName, String key) {
            CacheUtils.remove(cacheName, key);
        }
    }
}
