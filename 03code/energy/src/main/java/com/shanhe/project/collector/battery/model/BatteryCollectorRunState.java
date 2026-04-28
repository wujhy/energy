package com.shanhe.project.collector.battery.model;

/**
 * 采集通道运行态。
 */
public enum BatteryCollectorRunState {

    READ,

    WAIT_RESPONSE,

    COMMAND,

    WAIT_COMMAND_RESPONSE
}
