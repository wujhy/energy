package com.shanhe.project.scheduled;

import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.service.*;
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
    private IConfigAttributeService configAttributeService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IBatteryPackService batteryPackService;

    /** 定时更新设备配置缓存 */
    @Scheduled(cron = "${job.configCache}")
    public void configJob() {
        try {
            log.debug("更新设备缓存！");
            configAttributeService.updateCache();
        } catch (Exception e) {
            log.error("更新设备缓存异常", e);
        }
    }

    /** 定时更新设备告警缓存 */
    @Scheduled(cron = "${job.alarmCache}")
    public void alarmJob() {
        try {
            log.debug("更新设备告警缓存！");
            alarmLogService.updateCache();
        } catch (Exception e) {
            log.error("更新设备告警缓存异常", e);
        }
    }

    /** 定时更新电池组缓存 */
    @Scheduled(cron = "${job.batteryCache}")
    public void batteryJob() {
        try {
            log.debug("更新电池组缓存！");
            batteryPackService.updateCache();
        } catch (Exception e) {
            log.error("更新电池组缓存异常", e);
        }
    }

}
