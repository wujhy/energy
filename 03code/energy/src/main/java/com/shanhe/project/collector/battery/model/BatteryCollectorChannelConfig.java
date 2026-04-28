package com.shanhe.project.collector.battery.model;

import lombok.Data;

/**
 * 独立采集通道配置。
 *
 * @author wjh
 * @since 2026-04-28
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
     * 采集轮询间隔。
     */
    private Long pollIntervalMs = 3000L;

    /**
     * 单个请求等待响应超时时间。
     */
    private Long responseTimeoutMs = 1500L;

    /**
     * 串口单次读取缓冲大小。
     */
    private Integer readBufferSize = 2048;

    /**
     * 接收缓冲最大保留长度。
     */
    private Integer receiveBufferLimit = 8192;

    /**
     * 请求超时后的最大重试次数。
     */
    private Integer maxRetryCount = 2;

    private Integer deviceAddress = 1;

    /**
     * 600节模块端轮询起始地址，默认单体1。
     */
    private Integer moduleAddressStart = 1;

    /**
     * 600节模块端轮询结束地址，默认包含电流温度模块246。
     */
    private Integer moduleAddressEnd = 246;

    private Integer batteryPort;

    private Integer batteryChannel;

    /**
     * 对应电池组编号。
     */
    private Integer batteryGroup;

    private Boolean enabled = Boolean.TRUE;
}
