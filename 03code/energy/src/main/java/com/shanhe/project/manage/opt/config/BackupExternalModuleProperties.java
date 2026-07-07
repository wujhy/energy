package com.shanhe.project.manage.opt.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * `_5` 备电时长外部模块直控配置。
 *
 * @author wjh
 * @since 2026-07-07
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "battery-opt.backup-external-module")
public class BackupExternalModuleProperties {

    /** 是否启用 energy 直控外部备电模块。 */
    private Boolean enabled = Boolean.FALSE;

    /** 串口名称。 */
    private String portName;

    /** 波特率。 */
    private Integer baudRate = 9600;

    /** 数据位。 */
    private Integer dataBits = 8;

    /** 停止位。 */
    private Integer stopBits = 1;

    /** 校验位：0=无校验, 1=奇校验, 2=偶校验。 */
    private Integer parity = 0;

    /** 读写超时，单位毫秒；默认对齐 M460 约 2 秒响应窗口。 */
    private Integer timeoutMs = 2000;

    /** 重试次数。 */
    private Integer retryTimes = 0;

    /** 是否按 M460 旧逻辑用 0x6E + packNum - 1 计算站号。 */
    private Boolean useM460StationMapping = Boolean.TRUE;

    /** 固定站号；useM460StationMapping=false 时使用。 */
    private Integer stationAddress = 0x6E;

    /** 备电运行对应的远程断开寄存器地址，默认采用 M460 E_REMOTE_TURN_OFF_ADDRESS。 */
    private Integer backupRunRegisterAddress = 0x1142;

    /** 停止/空闲对应的远程合上寄存器地址，默认采用 M460 E_REMOTE_TURN_ON_ADDRESS。 */
    private Integer backupStopRegisterAddress = 0x1141;

    /** 写单寄存器值。 */
    private Integer writeValue = 0x00FF;
}
