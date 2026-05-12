package com.shanhe.project.scheduled;

import com.shanhe.project.monitor.server.service.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(cron = "${job.watchDog}")
    public void feedWatchDog() {
        try {
            SystemService.feedWatchDog();
        } catch (Exception e) {
            logger.error("喂狗异常：{}", e.getMessage());
        }
    }
}
