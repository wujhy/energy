package com.shanhe.project.collector.battery.protocol;

import lombok.Getter;

/**
 * 980节 PC/网络端聚合命令定义。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Getter
public enum BatteryAggregateCommandDefinition {

    // 980 PC / 网络通用读取与基础控制
    GET_SINGLE_BATTERY_INFO(0x01, 0x81, "获取单体电池信息"),
    GET_BATTERY_GROUP_INFO(0x02, 0x82, "获取电池组信息"),
    SET_BATTERY_GROUP_ALARM_PARAM(0x03, 0x83, "设置电池组报警参数"),
    BATCH_SET_BATTERY_GROUP_ALARM_PARAM(0x04, 0x84, "批量设置电池组报警参数"),
    SET_SYSTEM_STATE(0x05, 0x85, "设置系统状态"),
    READ_BATTERY_GROUP_CONFIG(0x06, 0x86, "读取电池组配置信息"),
    GET_BATTERY_GROUP_ALARM_STATE(0x07, 0x87, "获取电池组报警状态"),
    SET_SUBMODULE_ID(0x08, 0x88, "设置子模块ID"),
    CONFIGURE_BATTERY_GROUP(0x09, 0x89, "配置电池组"),
    DISABLE_WARNING(0x0A, 0x8A, "屏蔽报警"),
    READ_BATTERY_GROUP_ALARM_PARAM(0x0B, 0x8B, "读取电池组报警参数"),
    SET_DEVICE_ID(0x0C, 0x8C, "设置模块ID/设备ID"),
    GET_DEVICE_FAULT_ALARM_STATE(0x0D, 0x8D, "获取设备故障类告警状态"),
    GET_DEVICE_VERSION(0x0E, 0x8E, "获取设备型号及软件版本号"),
    CONNECT_RESISTANCE_TEST(0x0F, 0x8F, "连接条电阻测试"),

    // 980 PC / 网络上层设置与状态
    AUTOMATIC_SET_SUBMODULE_ADDRESS(0x18, 0xA8, "自动设置子模块地址"),
    SETTING_INTERNAL_RESISTANCE_COEFFICIENT(0x19, 0x99, "设置内阻系数"),
    SET_SWOLLEN_VOLTAGE_REFERENCE(0x20, 0x2A, "设置电池鼓包电压基础值"),
    SET_FLOATING_RELAY_SWITCH(0x21, 0x2B, "设置浮充继电器开关"),
    SET_CAPACITY_MODULE_WORK_MODE(0x30, 0xE0, "设置核容模块工作模式"),
    FLOAT_CHARGE_MANAGEMENT_CONFIG(0x31, 0xE1, "浮充管理配置"),
    INTERNAL_RESISTANCE_TEST_CONFIG(0x32, 0xE2, "内阻测试配置"),
    CONNECT_RESISTANCE_TEST_CONFIG(0x33, 0xE3, "连接条电阻测试配置"),
    CAPACITY_TEST_CONFIG(0x34, 0xE4, "核容测试配置"),
    BACKUP_DURATION_TEST_CONFIG(0x35, 0xE5, "备电时长测试配置"),
    SINGLE_INTERNAL_RESISTANCE_TEST(0x36, 0xE6, "单个电池内阻测试"),
    UPDATE_TIME_ALL(0x37, 0xE7, "修改日期时间"),
    BATTERY_EQUALIZATION_SET(0x38, 0xE8, "均衡设置"),
    DISABLE_WARNING_CONFIGURE(0x39, 0xE9, "获取屏蔽报警参数"),
    SET_SERVER_CLIENT_MODE(0x3A, 0xEA, "设置服务端客户端模式"),
    TEST_STATE(0x3B, 0xEB, "获取自动编号、连接条电阻、内阻测试状态"),
    ALARM_RELEASE(0x3C, 0xEC, "解除告警"),

    // 980 网络端读回与网络参数
    READ_SERVER_CLIENT_MODE(0x3E, 0xEE, "读取服务端客户端模式"),
    READ_MAC_ADDRESS(0x3F, 0xEF, "读取网卡MAC地址"),
    READ_INTERNAL_RESISTANCE_TEST_CONFIG(0x40, 0xC0, "读取内阻测试配置"),
    READ_CONNECT_RESISTANCE_TEST_CONFIG(0x41, 0xC1, "读取连接条电阻测试配置"),
    READ_CAPACITY_TEST_CONFIG(0x42, 0xC2, "读取核容测试配置"),
    READ_BACKUP_DURATION_TEST_CONFIG(0x43, 0xC3, "读取备电时长测试配置"),
    BATTERY_DATA_CORRECTION(0x76, 0xF6, "电池数据校正"),
    RESTORE_FACTORY_DEFAULTS(0x75, 0xF5, "恢复出厂设置"),
    OTA_UPGRADE(0x77, 0xF7, "系统程序升级OTA"),
    CLEAR_INDIVIDUAL_DEBUGGING_DATA(0x78, 0xF8, "清除电池组单体调试数据"),
    CLEAR_HOST_DEBUGGING_DATA(0x79, 0xF9, "清除主机调试数据"),
    SET_DEVICE_IP_ADDRESS(0x5E, 0xDE, "设置设备IP地址"),
    SET_CLOUD_SERVER_IP_ADDRESS(0x5F, 0xDF, "设置云服务器IP地址"),
    READ_DEVICE_IP_ADDRESS(0x61, 0xB1, "读取设备IP地址"),
    READ_CLOUD_SERVER_IP_ADDRESS(0x62, 0xB2, "读取云服务器IP地址");

    /**
     * 980 聚合请求命令码。
     */
    private final int requestCode;

    /**
     * 980 聚合响应命令码。
     */
    private final int responseCode;

    /**
     * 命令说明。
     */
    private final String description;

    BatteryAggregateCommandDefinition(int requestCode, int responseCode, String description) {
        this.requestCode = requestCode;
        this.responseCode = responseCode;
        this.description = description;
    }
}
