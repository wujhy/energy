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

    /** 通道名称。 */
    private String name;

    /** 串口名称。 */
    private String portName;

    /** 波特率。 */
    private Integer baudRate = 9600;

    /** 数据位。 */
    private Integer dataBits = 8;

    /** 停止位。 */
    private Integer stopBits = 1;

    /** 校验位。 */
    private Integer parity = 0;

    /** 超时时间（毫秒）。 */
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
    private Integer maxRetryCount = 1;

    /** 设备地址。 */
    private Integer deviceAddress = 1;

    /**
     * 600节模块端轮询起始地址，默认单体1。
     */
    private Integer moduleAddressStart = 1;

    /**
     * 600节模块端轮询结束地址，默认包含电流温度模块246。
     */
    private Integer moduleAddressEnd = 246;

    /**
     * 期望单体数量；为空时优先按 batteryGroup 读取 BatteryPack.batSinSize。
     */
    private Integer expectedCellCount;

    /** 电池端口号。 */
    private Integer batteryPort;

    /** 电池通道号。 */
    private Integer batteryChannel;

    /**
     * 兼容旧配置字段，内部通道定位不再依赖该值。
     */
    private Long configId;

    /**
     * 对应电池组编号。
     */
    private Integer batteryGroup;

    /** 是否启用。 */
    private Boolean enabled = Boolean.TRUE;
}
