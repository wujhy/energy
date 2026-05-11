package com.shanhe.project.scheduled;

import com.shanhe.framework.enums.ConnectionStatusEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
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
    IConfigService configService;
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

    public void initHost() {
        try {
            // 初始化主机未下线且imei为空
            Host host = hostService.getDetail();
            host.setHostId(1L);
            host.setStatus(ConnectionStatusEnum._0.getDictValue());
            SystemService.getIp(host);
            hostService.updateHost(host);
        } catch (Exception e) {
            log.error("初始化主机缓存异常：{}", e.getMessage());
        }
    }

    public void initConfig() {
        try {
            configService.updateCache();
        } catch (Exception e) {
            log.error("初始化设备配置缓存异常：{}", e.getMessage());
        }
    }

    public void initAlarm() {
        try {
            alarmLogService.updateCache();
        } catch (Exception e) {
            log.error("初始化告警缓存异常：{}", e.getMessage());
        }
    }

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
