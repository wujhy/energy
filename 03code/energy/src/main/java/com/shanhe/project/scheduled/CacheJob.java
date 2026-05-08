package com.shanhe.project.scheduled;

import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 缓存任务
 *
 * @author wjh
 * @since 2025/3/18
 */
@Slf4j
@Component
@EnableScheduling
public class CacheJob {

    @Resource
    private IConfigService configService;
    @Resource
    private IAlarmLogService alarmLogService;
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IBatteryPackService batteryPackService;

    @Scheduled(cron = "${job.configCache}")
    public void configJob() {
        try {
            log.debug("更新设备缓存！");
            configService.updateCache();
        } catch (Exception e) {
            log.error("更新设备缓存异常", e);
        }
    }

    @Scheduled(cron = "${job.alarmCache}")
    public void alarmJob() {
        try {
            log.debug("更新设备告警缓存！");
            alarmLogService.updateCache();
        } catch (Exception e) {
            log.error("更新设备告警缓存异常", e);
        }
    }

    @Scheduled(cron = "${job.batteryCache}")
    public void batteryJob() {
//        try {
//            log.debug("更新电池历史缓存！");
//            batteryMonitorService.updateCache();
//        } catch (Exception e) {
//            log.error("更新电池历史缓存异常：{}", e.getMessage());
//        }

        try {
            log.debug("更新电池组缓存！");
            batteryPackService.updateCache();
        } catch (Exception e) {
            log.error("更新电池组缓存异常", e);
        }

        try {
            log.debug("更新电池组历史缓存！");
            batteryReportLogService.updateCache();
        } catch (Exception e) {
            log.error("更新电池组历史缓存异常", e);
        }
    }

}
