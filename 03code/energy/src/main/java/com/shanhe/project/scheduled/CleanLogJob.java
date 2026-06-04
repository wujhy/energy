package com.shanhe.project.scheduled;

import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.monitor.operlog.service.IOperLogService;
import com.shanhe.project.monitor.server.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 清理日志
 *
 * @author wjh
 * @since 2025/3/18
 */
@Slf4j
@Component
@EnableScheduling
public class CleanLogJob {

    @Value("${job.cleanBatteryReportDays:3}")
    private Integer cleanBatteryReportDays;
    @Value("${job.cleanSysLogMonth:2}")
    private Integer cleanSysLogMonth;

    @Resource
    private IHostService hostService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IOperLogService operLogService;
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    @Resource
    private com.shanhe.project.collector.battery.mapper.BatteryModuleFrameLogMapper frameLogMapper;

    @Scheduled(cron = "${job.cleanLog}")
    public void logCleanJob() {
        try {
            // 耗时较大容易堵塞，先关闭看门狗
            SystemService.closeWatchDog();

            Host host = hostService.getDetail();
            Integer cleanBattery = host.getCleanLogDays() != null ? host.getCleanLogDays() : cleanBatteryReportDays;

            log.info("删除电池历史记录异常：{}天前", cleanBattery);
            try {
                batteryReportLogService.deleteByDays(cleanBattery);
            } catch (Exception e) {
                log.error("删除单体电池历史记录异常：{}", e.getMessage());
            }

            log.info("删除系统历史记录：{}个月前", cleanSysLogMonth);
            try {
                operLogService.deleteOperLog(cleanSysLogMonth);
            } catch (Exception e) {
                log.error("删除系统历史记录异常：{}", e.getMessage());
            }

            try {
                batteryDeviceStateService.deleteExpired();
            } catch (Exception e) {
                log.error("删除过期设备状态异常：{}", e.getMessage());
            }

            try {
                int frameLogDeleted = frameLogMapper.deleteByDays(7);
                log.info("删除7天前原始帧日志：{}条", frameLogDeleted);
            } catch (Exception e) {
                log.error("删除原始帧日志异常：{}", e.getMessage());
            }

            try {
                //程序休眠后5秒后再执行
                log.info("----------------- 数据库压缩开始 --------------------");
                operLogService.vacuum();
                log.info("----------------- 数据库压缩结束 --------------------");
            } catch (Exception e) {
                log.error("缩减数据空间：{}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("清理日志异常：{}", e.getMessage());
        } finally {
            // 打开看门狗
            SystemService.openWatchDog();
        }
    }
}
