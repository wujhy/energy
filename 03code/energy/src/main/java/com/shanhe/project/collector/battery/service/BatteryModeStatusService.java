package com.shanhe.project.collector.battery.service;

import cn.hutool.core.util.ObjUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 蓄电池测试/维护工作模式缓存服务。
 *
 * @author wjh
 * @since 2026/5/13
 */
@Service
public class BatteryModeStatusService {

    /**
     * 无测试。
     */
    public static final int MODE_IDLE = 0;
    /**
     * 自动编号。
     */
    public static final int MODE_AUTO_MODEL_NUM = 1;
    /**
     * 内阻测试。
     */
    public static final int MODE_INTERNAL_RESISTANCE = 6;
    /**
     * 连接条电阻测试。
     */
    public static final int MODE_CONNECT_RESISTANCE = 10;

    private static final int STATUS_STOP = 0;
    private static final int STATUS_RUNNING = 1;
    private static final String MODE_STATUS_KEY = "battery:mode:status:EB";

    private final CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    private CacheAccessor cacheAccessor = new CacheUtilsAccessor();

    public BatteryModeInfo get(Integer packNum) {
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), key());
        if (result instanceof BatteryModeInfo) {
            return (BatteryModeInfo) result;
        }
        return idle(packNum);
    }

    public void clear(Integer packNum) {
        String key = key();
        if (packNum == null) {
            cacheAccessor.remove(cacheKeyEnum.getCache(), key);
            return;
        }
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), key);
        if (result instanceof BatteryModeInfo) {
            BatteryModeInfo batteryModeInfo = (BatteryModeInfo) result;
            if (ObjUtil.equals(packNum, batteryModeInfo.getPackNum())) {
                cacheAccessor.remove(cacheKeyEnum.getCache(), key);
            }
        }
    }

    public void markRunning(Integer packNum, int mode, Integer address) {
        BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
        batteryModeInfo.setPackNum(packNum);
        batteryModeInfo.setResult(0);
        batteryModeInfo.setMode(mode);
        batteryModeInfo.setStatus(STATUS_RUNNING);
        batteryModeInfo.setAddress(address);
        cacheAccessor.put(cacheKeyEnum.getCache(), key(), batteryModeInfo);
    }

    public void markStopped(Integer packNum, int mode, Integer address, boolean success) {
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
        cacheAccessor.put(cacheKeyEnum.getCache(), key(), batteryModeInfo);
    }

    public void putFromM460(BatteryModeInfo batteryModeInfo) {
        if (batteryModeInfo == null) {
            return;
        }
        String key = key();
        if (Objects.equals(batteryModeInfo.getStatus(), STATUS_STOP)) {
            BatteryModeInfo oldBatteryModeInfo = getStored();
            if (oldBatteryModeInfo != null) {
                batteryModeInfo.setLastPackNum(oldBatteryModeInfo.getLastPackNum());
                batteryModeInfo.setLastMode(oldBatteryModeInfo.getLastMode());
                batteryModeInfo.setLastAddress(oldBatteryModeInfo.getAddress());
                // 旧 M460 内阻测试启动后，短时间可能回无测试，页面仍沿用上一轮进行中状态。
                if (Objects.equals(oldBatteryModeInfo.getAddress(), 1)
                        && Objects.equals(batteryModeInfo.getMode(), MODE_IDLE)) {
                    batteryModeInfo.setResult(oldBatteryModeInfo.getResult());
                    batteryModeInfo.setStatus(oldBatteryModeInfo.getStatus());
                    batteryModeInfo.setAddress(oldBatteryModeInfo.getAddress());
                }
            }
        }
        cacheAccessor.put(cacheKeyEnum.getCache(), key, batteryModeInfo);
    }

    public String key() {
        return MODE_STATUS_KEY;
    }

    private BatteryModeInfo getStored() {
        Object result = cacheAccessor.get(cacheKeyEnum.getCache(), key());
        return result instanceof BatteryModeInfo ? (BatteryModeInfo) result : null;
    }

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
