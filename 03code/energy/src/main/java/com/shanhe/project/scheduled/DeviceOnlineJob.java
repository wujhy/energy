package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigService;
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
    private IConfigService configService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private BatteryModuleReportLogAdapterService batteryModuleReportLogAdapterService;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    private static final String STATE_CODE_ONLINE = BatteryDeviceStateConstants.StateCode.ONLINE;

    private final Map<Integer, Integer> offlineBatteryPackNumMap = new HashMap<>();

    private boolean isStart = true;
    private static final long SERVER_START_TIME = System.currentTimeMillis();
    private static final long STARTUP_CHECK_DELAY = TimeUnit.MINUTES.toMillis(2);

    @Scheduled(cron = "${job.deviceOnline}")
    public void cmdDevice() {
        try {
            log.debug("同步电池组在线状态开始");
            if (isStart) {
                Long currentTime = System.currentTimeMillis();
                if (Math.abs(currentTime - SERVER_START_TIME) <= STARTUP_CHECK_DELAY) {
                    return;
                }
                isStart = false;
            }

            Config config = configService.selectDefaultConfig();
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
                    handleMissingOnlineCache(config, packNum);
                    continue;
                }

                Date lastDate = (Date) object;
                int num = DateUtils.differentMillsByMillisecond(lastDate, nowDate);
                if (num > maxOffline) {
                    syncBatteryOfflineAlarm(config, packNum, true);
                    log.info("同步电池组在线状态, 电池组={} 离线, 最后上报分钟数={}, 最大离线分钟数={}",
                            packNum, num, maxOffline);
                    CacheUtils.remove(CacheKeyEnum.BATTERY_ONLINE.getCache(), key);
                    continue;
                }

                offlineBatteryPackNumMap.put(packNum, 0);
                syncBatteryOfflineAlarm(config, packNum, false);
            }
        } catch (Exception e) {
            log.error("同步电池组在线状态失败: {}", e.getMessage(), e);
        } finally {
            log.debug("同步电池组在线状态完成");
        }
    }

    private void handleMissingOnlineCache(Config config, Integer packNum) {
        int offlineNum = offlineBatteryPackNumMap.getOrDefault(packNum, 0);
        if (offlineNum > maxOffline) {
            log.info("同步电池组在线状态, 电池组={} 离线, 缺失次数={}", packNum, offlineNum);
            syncBatteryOfflineAlarm(config, packNum, true);
            offlineBatteryPackNumMap.put(packNum, 0);
            return;
        }
        offlineBatteryPackNumMap.put(packNum, offlineNum + 1);
    }

    private void handleDisabledBatteryPack(Integer packNum) {
        offlineBatteryPackNumMap.remove(packNum);
        alarmLogService.alarmFix(packNum, false, null, Collections.singletonList(ItemCode.TXZT.getCode()));
    }

    private void syncBatteryOfflineAlarm(Config config, Integer packNum, boolean offline) {
        Map<String, String> warnParam = new HashMap<>(1);
        warnParam.put(ItemCode.TXZT.getCode(), offline ? "1" : "0");
        BatteryReportLog batteryReportLog = resolveBatteryReportLog(packNum);
        alarmLogService.alarmBattery(config, packNum, null, warnParam, batteryReportLog);
        persistOnlineState(packNum, offline);
    }

    BatteryReportLog resolveBatteryReportLog(Integer packNum) {
        if (batteryCollectorProperties != null
                && Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpRealtimeSourceEnabled())) {
            try {
                BatteryReportLog realtimeLog = batteryModuleReportLogAdapterService.buildReportLog(packNum);
                if (isUsableBatteryReportLog(realtimeLog)) {
                    return realtimeLog;
                }
            } catch (Exception e) {
                log.warn("标准实时数据构建设备在线告警上下文失败, packNum={}, fallback=oldCache", packNum, e);
            }
        }
        return batteryReportLogService.lastCache(packNum);
    }

    private boolean isUsableBatteryReportLog(BatteryReportLog log) {
        return log != null
                && log.getPackParam() != null
                && !log.getPackParam().isEmpty()
                && log.getBatteryList() != null
                && !log.getBatteryList().isEmpty();
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
