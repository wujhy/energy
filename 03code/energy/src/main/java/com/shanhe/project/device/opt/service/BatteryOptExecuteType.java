package com.shanhe.project.device.opt.service;

/**
 * 蓄电池测试执行来源。
 */
public enum BatteryOptExecuteType {
    /** 页面立即执行。 */
    MANUAL,

    /** 测试计划定时触发。 */
    SCHEDULED,

    /** 平台同步触发立即执行。 */
    SYNC
}
