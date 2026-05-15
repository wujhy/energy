package com.shanhe.project.scheduled;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
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

    private final Map<Integer, Integer> offlineBatteryPackNumMap = new HashMap<>();

    private boolean isStart = true;
    private static final long SERVER_START_TIME = System.currentTimeMillis();
    private static final long STARTUP_CHECK_DELAY = TimeUnit.MINUTES.toMillis(2);

    @Scheduled(cron = "${job.deviceOnline}")
    public void cmdDevice() {
        try {
            log.debug("sync battery pack online status start");
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
                log.debug("sync battery pack online status skipped, no battery pack");
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
                log.info("sync battery pack online status, packNum={}, key={}, lastReportTime={}",
                        packNum, key, object);

                if (object == null) {
                    handleMissingOnlineCache(config, packNum);
                    continue;
                }

                Date lastDate = (Date) object;
                int num = DateUtils.differentMillsByMillisecond(lastDate, nowDate);
                if (num > maxOffline) {
                    syncBatteryOfflineAlarm(config, packNum, true);
                    log.info("sync battery pack online status, packNum={} offline, lastReportMinutes={}, maxOffline={}",
                            packNum, num, maxOffline);
                    CacheUtils.remove(CacheKeyEnum.BATTERY_ONLINE.getCache(), key);
                    continue;
                }

                offlineBatteryPackNumMap.put(packNum, 0);
                syncBatteryOfflineAlarm(config, packNum, false);
            }
        } catch (Exception e) {
            log.error("sync battery pack online status failed: {}", e.getMessage(), e);
        } finally {
            log.debug("sync battery pack online status finished");
        }
    }

    private void handleMissingOnlineCache(Config config, Integer packNum) {
        int offlineNum = offlineBatteryPackNumMap.getOrDefault(packNum, 0);
        if (offlineNum > maxOffline) {
            log.info("sync battery pack online status, packNum={} offline, missCount={}", packNum, offlineNum);
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
        BatteryReportLog batteryReportLog = batteryReportLogService.lastCache(packNum);
        alarmLogService.alarmBattery(config, packNum, null, warnParam, batteryReportLog);
    }
}
