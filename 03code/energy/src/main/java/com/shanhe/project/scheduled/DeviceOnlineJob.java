package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryAlarmEvaluationService;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Date;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 设备在线检测任务
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Component
@EnableScheduling
public class DeviceOnlineJob {

    @Value("${job.offlineNum:10}")
    private int maxOffline;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryAlarmEvaluationService alarmEvaluationService;
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /** 电池组在线状态码。 */
    private static final String STATE_CODE_ONLINE = BatteryDeviceStateConstants.StateCode.ONLINE;

    private final Map<Integer, Integer> offlineBatteryPackNumMap = new HashMap<>();

    private boolean isStart = true;
    /** 服务启动时间戳（毫秒）。 */
    private static final long SERVER_START_TIME = System.currentTimeMillis();
    /** 启动后延迟检查的时间窗口（2分钟），避免启动初期误判离线。 */
    private static final long STARTUP_CHECK_DELAY = TimeUnit.MINUTES.toMillis(2);

    @Scheduled(cron = "${job.deviceOnline}")
    public void cmdDevice() {
        try {
            log.debug("同步电池组在线状态开始");
            if (isStart) {
                long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - SERVER_START_TIME) <= STARTUP_CHECK_DELAY) {
                    return;
                }
                isStart = false;
            }

            List<BatteryPack> packList = batteryPackService.selectBatteryPackListCache(null);
            if (packList == null || packList.isEmpty()) {
                log.debug("同步电池组在线状态跳过，无电池组配置");
                return;
            }

            Date nowDate = new Date();
            for (BatteryPack batteryPack : packList) {
                if (batteryPack == null || batteryPack.getPackNum() == null) {
                    continue;
                }
                if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
                    handleDisabledBatteryPack(batteryPack.getPackNum());
                    continue;
                }

                Integer packNum = batteryPack.getPackNum();
                String key = String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), packNum);
                Object object = CacheUtils.get(CacheKeyEnum.BATTERY_ONLINE.getCache(), key);
                log.info("同步电池组在线状态, 电池组={}, 缓存键={}, 最后上报时间={}",
                        packNum, key, object);

                if (object == null) {
                    handleMissingOnlineCache(packNum);
                    continue;
                }

                Date lastDate = (Date) object;
                int num = DateUtils.differentMillsByMillisecond(lastDate, nowDate);
                if (num > maxOffline) {
                    syncBatteryOfflineAlarm(packNum, true);
                    log.info("同步电池组在线状态, 电池组={} 离线, 最后上报分钟数={}, 最大离线分钟数={}",
                            packNum, num, maxOffline);
                    CacheUtils.remove(CacheKeyEnum.BATTERY_ONLINE.getCache(), key);
                    continue;
                }

                offlineBatteryPackNumMap.put(packNum, 0);
                syncBatteryOfflineAlarm(packNum, false);
            }
        } catch (Exception e) {
            log.error("同步电池组在线状态失败: {}", e.getMessage(), e);
        } finally {
            log.debug("同步电池组在线状态完成");
        }
    }

    private void handleMissingOnlineCache(Integer packNum) {
        int offlineNum = offlineBatteryPackNumMap.getOrDefault(packNum, 0);
        if (offlineNum > maxOffline) {
            log.info("同步电池组在线状态, 电池组={} 离线, 缺失次数={}", packNum, offlineNum);
            syncBatteryOfflineAlarm(packNum, true);
            offlineBatteryPackNumMap.put(packNum, 0);
            return;
        }
        offlineBatteryPackNumMap.put(packNum, offlineNum + 1);
    }

    private void handleDisabledBatteryPack(Integer packNum) {
        offlineBatteryPackNumMap.remove(packNum);
        alarmEvaluationService.recoverPackWarnings(packNum, Collections.singletonList(ItemCode.TXZT.getCode()));
    }

    private void syncBatteryOfflineAlarm(Integer packNum, boolean offline) {
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService == null
                ? null : realtimeSnapshotService.getCachedSnapshot(packNum);
        persistOnlineState(packNum, offline);
        if (offline) {
            alarmEvaluationService.submitPackWarnings(packNum,
                    Collections.singletonMap(ItemCode.TXZT.getCode(), "1"));
            if (snapshot == null || !snapshot.isDataReady() || !snapshot.isFresh()) {
                log.debug("电池组离线状态已按 energy 告警上下文处理, packNum={}, snapshotReady={}",
                        packNum, snapshot != null && snapshot.isDataReady());
            }
            return;
        }
        alarmEvaluationService.recoverPackWarnings(packNum, Collections.singletonList(ItemCode.TXZT.getCode()));
    }

    /** 持久化电池组在线/离线状态到 battery_device_state。 */
    private void persistOnlineState(Integer packNum, boolean offline) {
        try {
            BatteryDeviceState state = new BatteryDeviceState();
            state.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
            state.setScopeKey(String.valueOf(packNum));
            state.setPackNum(packNum);
            state.setStateCode(STATE_CODE_ONLINE);
            state.setStateValue(offline ? "offline" : "online");
            state.setStateLevel(offline ? BatteryDeviceStateConstants.StateLevel.WARN : BatteryDeviceStateConstants.StateLevel.NORMAL);
            state.setSource(BatteryDeviceStateConstants.Source.DEVICE_ONLINE_JOB);
            state.setFirstSeenTime(new Date());
            state.setLastChangeTime(new Date());
            if (offline) {
                state.setExpireTime(new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(7)));
            }
            batteryDeviceStateService.upsert(state);
        } catch (Exception e) {
            log.warn("持久化电池组在线状态失败, 电池组={}, 原因={}", packNum, e.getMessage());
        }
    }
}
