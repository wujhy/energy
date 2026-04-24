package com.shanhe.project.scheduled;

import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.history.service.IHistoryLogService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.monitor.operlog.service.IOperLogService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
@EnableScheduling
public class CleanLogJob {

    protected static Logger logger = LoggerFactory.getLogger(CleanLogJob.class);

    @Value("${job.cleanHistoryLogDays:3}")
    private Integer cleanHistoryLogDays;
    @Value("${job.cleanBatteryMonitorDays:3}")
    private Integer cleanBatteryMonitorDays;
    @Value("${job.cleanSysLogMonth:2}")
    private Integer cleanSysLogMonth;

    @Resource
    private IHostService hostService;
    @Resource
    private IHistoryLogService historyLogService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IOperLogService operLogService;

    @Scheduled(cron = "${job.cleanLog}")
    public void logCleanJob() {
        try {
            // 耗时较大容易堵塞，先关闭看门狗
            SystemService.closeWatchDog();

            Host host = hostService.getDetail();
            Integer cleanHistory = host.getCleanLogDays() != null ? host.getCleanLogDays() : cleanHistoryLogDays;
            Integer cleanBattery = host.getCleanLogDays() != null ? host.getCleanLogDays() : cleanBatteryMonitorDays;

            logger.info("删除设备历史记录：{}天前", cleanHistory);
            try {
                historyLogService.deleteHistoryLog(cleanHistory);
            } catch (Exception e) {
                logger.error("删除设备历史记录异常：{}", e.getMessage());
            }

            logger.info("删除电池历史记录异常：{}天前", cleanBattery);
            try {
                batteryReportLogService.deleteByDays(cleanBattery);
            } catch (Exception e) {
                logger.error("删除单体电池历史记录异常：{}", e.getMessage());
            }

            logger.info("删除系统历史记录：{}个月前", cleanSysLogMonth);
            try {
                operLogService.deleteOperLog(cleanSysLogMonth);
            } catch (Exception e) {
                logger.error("删除系统历史记录异常：{}", e.getMessage());
            }

            try {
                //程序休眠后5秒后再执行
                logger.info("----------------- vacuum run--------------------");
                operLogService.vacuum();
                logger.info("----------------- vacuum end--------------------");
            } catch (Exception e) {
                logger.error("缩减数据空间：{}", e.getMessage());
            }
        } catch (Exception e) {
            logger.error("清理日志异常：{}", e.getMessage());
        } finally {
            // 打开看门狗
            SystemService.openWatchDog();
        }
    }
}
