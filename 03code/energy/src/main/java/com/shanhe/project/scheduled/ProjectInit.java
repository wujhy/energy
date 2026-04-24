package com.shanhe.project.scheduled;

import com.shanhe.project.monitor.server.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 初始化
 *
 * @author wjh
 * @since 2025/3/18
 */
@Slf4j
@Order(1)
@Component
public class ProjectInit implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            SystemService.chromiumApp();
        } catch (Exception e) {
            log.error("初始化浏览器异常：{}", e.getMessage());
        }

        try {
            SystemService.openWatchDog();
        } catch (Exception e) {
            log.error("开启看门狗异常：{}", e.getMessage());
        }
    }
}
