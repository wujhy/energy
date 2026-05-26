package com.shanhe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动程序
 *
 * @author wjh
 * @since 2024/12/17
 */
@Slf4j
@EnableCaching
@EnableAsync
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class EnergyApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnergyApplication.class, args);
        log.info("====================================================================启动成功");
    }
}