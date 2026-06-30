package com.shanhe.project.scheduled;

import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
import com.shanhe.project.manage.host.domain.Host;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.manage.opt.service.OptLogService;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.monitor.server.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 初始化缓存
 *
 * @author wjh
 * @since 2025/3/18
 */
@Slf4j
@Order(2)
@Component
public class CacheInit implements ApplicationRunner {
    @Resource
    IConfigAttributeService configAttributeService;
    @Resource
    OptLogService optLogService;
    @Resource
    IHostService hostService;
    @Resource
    IAlarmLogService alarmLogService;
    @Resource
    BatteryReportLogService batteryReportLogService;
    @Resource
    IBatteryPackService batteryPackService;
    @Resource
    PreBatteryGroupService preBatteryGroupService;

    /**
     * 应用启动时初始化所有缓存
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // @PostConstruct 注解初始化缓存
        log.info("-----------------初始化缓存开始------------");
        initHost();
        initConfig();
        initAlarm();
        initBattery();
        log.info("-----------------初始化缓存结束------------");
    }

    /**
     * 初始化主机信息缓存
     */
    public void initHost() {
        try {
            // 初始化主机未下线且imei为空
            Host host = hostService.getDetail();
            host.setHostId(1L);
            SystemService.getIp(host);
            hostService.updateHost(host);
        } catch (Exception e) {
            log.error("初始化主机缓存异常：{}", e.getMessage());
        }
    }

    /**
     * 初始化设备配置缓存
     */
    public void initConfig() {
        try {
            configAttributeService.updateCache();
            optLogService.updateCache();
        } catch (Exception e) {
            log.error("初始化设备配置缓存异常：{}", e.getMessage());
        }
    }

    /**
     * 初始化告警缓存
     */
    public void initAlarm() {
        try {
            alarmLogService.updateCache();
        } catch (Exception e) {
            log.error("初始化告警缓存异常：{}", e.getMessage());
        }
    }

    /**
     * 初始化电池组相关缓存
     */
    public void initBattery() {
        try {
            batteryPackService.updateCache();
        } catch (Exception e) {
            log.error("初始化电池组缓存异常：{}", e.getMessage());
        }
        try {
            batteryReportLogService.updateCache();
        } catch (Exception e) {
            log.error("初始化电池组记录缓存异常：{}", e.getMessage());
        }
        try {
            preBatteryGroupService.updateCache();
        } catch (Exception e) {
            log.error("初始化预电池组缓存异常：{}", e.getMessage());
        }
    }

}
