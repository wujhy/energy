package com.shanhe.project.collector.battery.model;

/**
 * 600节模块端解析数据类型。
 *
 * @author wjh
 * @since 2026-04-28
 */
public enum BatteryModuleDataType {

    /** 单模块信息 */
    SINGLE_MODULE_INFO,
    /** 数组模块信息 */
    ARRAY_MODULE_INFO,
    /** 连接条电阻电压 */
    CONNECT_RESISTANCE_VOLTAGE,
    /** 状态响应 */
    STATUS_RESPONSE,
    /** 自动设置地址响应 */
    AUTO_SET_ADDRESS_RESPONSE
}
