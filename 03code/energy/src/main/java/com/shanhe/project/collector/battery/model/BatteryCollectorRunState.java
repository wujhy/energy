package com.shanhe.project.collector.battery.model;

/**
 * 采集通道运行状态。
 *
 * @author wjh
 * @since 2026-04-28
 */
public enum BatteryCollectorRunState {

    /** 读取状态 */
    READ,

    /** 等待响应 */
    WAIT_RESPONSE,

    /** 命令状态 */
    COMMAND,

    /** 等待命令响应 */
    WAIT_COMMAND_RESPONSE
}
