package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.Builder;
import lombok.Data;

/**
 * 600节模块端显式控制命令。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Data
@Builder
public class BatteryModuleControlCommand {

    /**
     * 600节模块端协议定义。
     */
    private BatteryDeviceProtocolCode protocolCode;

    /**
     * 目标模块地址，广播命令使用0。
     */
    private int address;

    /**
     * 请求负载。
     */
    private byte[] payload;

    /**
     * 请求命令码。
     */
    private int requestCode;

    /**
     * 期望响应命令码；无明确响应时为空。
     */
    private Integer responseCode;

    /**
     * 命令说明。
     */
    private String description;

    /**
     * 旧业务设备ID，用于兼容 BatteryModeInfo 状态缓存。
     */
    private Long configId;

    /**
     * 电池组编号，用于兼容 BatteryModeInfo 状态缓存。
     */
    private Integer batteryGroup;

    /**
     * 旧 BatteryModeInfo 工作模式。
     */
    private Integer mode;

    /**
     * 自动编号目标单体数量。
     */
    private Integer autoAddressBatteryCount;

    /**
     * 自动编号电池规格。
     */
    private Integer autoAddressBatterySpecification;
}
