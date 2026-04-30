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
}
