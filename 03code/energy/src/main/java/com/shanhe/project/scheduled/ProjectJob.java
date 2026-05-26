package com.shanhe.project.scheduled;

import com.shanhe.project.monitor.server.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 设备任务
 *
 * @author wjh
 * @since 2025/4/22
 */
@Slf4j
@Component
@EnableScheduling
public class ProjectJob {

    @Scheduled(cron = "${job.watchDog}")
    public void feedWatchDog() {
        try {
            SystemService.feedWatchDog();
        } catch (Exception e) {
            log.error("喂狗异常：{}", e.getMessage());
        }
    }
}
