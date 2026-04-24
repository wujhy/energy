package com.shanhe.framework.config;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Flyway
 *
 * @author wjh
 * @since 2025/11/11
 */
@Slf4j
@Order(1)
@Lazy(false)
@Configuration
public class FlywayConfig {

    @Resource
    private DataSource dataSource;

    @PostConstruct
    public void migrate() {
        Flyway flyway = Flyway.configure()
                // 复用数据库连接
                .dataSource(dataSource)
                // 是否禁止清除已有数据
                .cleanDisabled(true)
                // 脚本文件路径
                .locations("db/migration")
                // 是否允许无序
                .outOfOrder(false)
                .encoding("UTF-8")
                // 是否允许重复执行
                .validateOnMigrate(false)
                // 版本管理表、初始版本号、自动创建
                .table("flyway_schema_history")
                .baselineVersion("1")
                .baselineOnMigrate(true)
                // 是否在验证失败时清除数据库
                .cleanOnValidationError(false)
                .load();
        try {
            // 执行迁移记录
            flyway.migrate();
        } catch (FlywayException e) {
            log.error("flyway init error", e);
            try {
                // 修复迁移失败记录
                flyway.repair();
                log.info("flyway repair success!");
                // 重新执行迁移记录
                flyway.migrate();
                log.info("flyway again init success!");
            } catch (Exception ex) {
                log.error("flyway again init error:{}", ex.getMessage());
            }
        }
    }
}
