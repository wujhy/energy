package com.shanhe.project.manage.opt.service;

/**
 * 蓄电池测试执行来源。
 *
 * @author wjh
 * @since 2026/06/26
 */
public enum BatteryOptExecuteType {
    /** 页面立即执行。 */
    MANUAL,

    /** 测试计划定时触发。 */
    SCHEDULED,

    /** 平台同步触发立即执行。 */
    SYNC
}
