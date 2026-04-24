package com.shanhe.project.scheduled;

import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 设备任务
 *
 * @author wjh
 * @since 2025/4/22
 */
@Component
@EnableScheduling
public class ProjectJob {
    protected static Logger logger = LoggerFactory.getLogger(ProjectJob.class);
    @Resource
    private IHostService hostService;

    @Scheduled(cron = "${job.device}")
    public void cmdDevice() {
        try {
            hostService.syncServerTime(null);
        } catch (Exception e) {
            logger.error("同步服务器时间异常：{}", e.getMessage());
        }
    }

    @Scheduled(cron = "${job.watchDog}")
    public void feedWatchDog() {
        try {
            SystemService.feedWatchDog();
        } catch (Exception e) {
            logger.error("喂狗异常：{}", e.getMessage());
        }
    }
}
