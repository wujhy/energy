package com.shanhe.project.collector.battery.model;

/**
 * 采集通道运行状态。
 *
 * @author wjh
 * @since 2026-04-28
 */
public enum BatteryCollectorRunState {

    READ,

    WAIT_RESPONSE,

    COMMAND,

    WAIT_COMMAND_RESPONSE
}
