package com.shanhe.project.scheduled;

import com.shanhe.project.monitor.server.service.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 重启浏览器
 *
 * @author xuxw
 * @since 2025/9/11
 */
@Component
@EnableScheduling
public class RestartAppJob {
    protected static Logger logger = LoggerFactory.getLogger(RestartAppJob.class);
    @Scheduled(cron = "${job.restartAppCron}")
    public void restartApp() {
        try {
            SystemService.resChromiumApp();
        } catch (Exception e) {
            logger.error("重新启动客户端：{}", e.getMessage());
        }

    }
}
