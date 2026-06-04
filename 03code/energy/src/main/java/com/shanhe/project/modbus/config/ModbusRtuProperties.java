package com.shanhe.project.modbus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Modbus RTU 从站配置。
 *
 * @author wjh
 * @since 2026-06-03
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "battery-collector.modbus-rtu")
public class ModbusRtuProperties {

    /** 是否启用 Modbus RTU 从站。 */
    private Boolean enabled = Boolean.FALSE;

    /** 串口名称。 */
    private String portName;

    /** 站号（从机地址）。 */
    private Integer stationAddress = 1;

    /** 波特率。 */
    private Integer baudRate = 115200;

    /** 数据位。 */
    private Integer dataBits = 8;

    /** 停止位。 */
    private Integer stopBits = 1;

    /** 校验位：0=无校验, 1=奇校验, 2=偶校验。 */
    private Integer parity = 0;

    /** 读超时(ms)。 */
    private Integer readTimeoutMs = 1000;
}
