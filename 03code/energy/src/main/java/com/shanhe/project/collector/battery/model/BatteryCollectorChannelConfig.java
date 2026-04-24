package com.shanhe.project.collector.battery.model;

import lombok.Data;

/**
 * 独立采集通道配置。
 */
@Data
public class BatteryCollectorChannelConfig {

    private String name;

    private String portName;

    private Integer baudRate = 9600;

    private Integer dataBits = 8;

    private Integer stopBits = 1;

    private Integer parity = 0;

    private Integer timeoutMs = 1000;

    /**
     * 轮询周期，毫秒。
     */
    private Long pollIntervalMs = 3000L;

    /**
     * 指令响应超时，毫秒。
     */
    private Long responseTimeoutMs = 1500L;

    /**
     * 单次读取缓冲区大小。
     */
    private Integer readBufferSize = 2048;

    /**
     * 累积接收缓冲上限。
     */
    private Integer receiveBufferLimit = 8192;

    /**
     * 单条命令最大重试次数。
     */
    private Integer maxRetryCount = 2;

    private Integer deviceAddress = 1;

    private Integer batteryPort;

    private Integer batteryChannel;

    /**
     * 电池组编号。
     */
    private Integer batteryGroup;

    private Boolean enabled = Boolean.TRUE;
}
