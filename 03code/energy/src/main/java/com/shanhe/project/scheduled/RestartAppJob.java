package com.shanhe.project.scheduled;

import com.shanhe.project.monitor.server.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 重启浏览器
 *
 * @author wjh
 * @since 2025/9/11
 */
@Slf4j
@Component
@EnableScheduling
public class RestartAppJob {
    @Scheduled(cron = "${job.restartAppCron}")
    public void restartApp() {
        try {
            SystemService.resChromiumApp();
        } catch (Exception e) {
            log.error("重新启动客户端：{}", e.getMessage());
        }

    }
}
