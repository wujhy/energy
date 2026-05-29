package com.shanhe.project.collector.battery.protocol;

/**
 * 600 节采集模块端协议常量。
 *
 * @author wjh
 * @since 2026-05-27
 */
public final class BatteryModuleProtocolConstants {

    private BatteryModuleProtocolConstants() {
    }

    /**
     * 单体模块最大地址（1..245）。
     */
    public static final int MAX_CELL_ADDRESS = 245;

    /**
     * 组模块地址（246），提供组电流、外组电压、环境温度等原始组数据。
     */
    public static final int GROUP_MODULE_ADDRESS = 246;

    /**
     * 无符号字节最大值（0xFF）。
     */
    public static final int UNSIGNED_BYTE_MAX = 255;

    /**
     * 无符号短整型最大值（0xFFFF）。
     */
    public static final int UNSIGNED_SHORT_MAX = 65535;

    /**
     * 2V 电池规格编码。
     */
    public static final int BATTERY_SPEC_2V = 2;

    /**
     * 12V 电池规格编码（6 节串联）。
     */
    public static final int BATTERY_SPEC_12V = 8;

    /**
     * 2V 规格对应的标称电压。
     */
    public static final int VOLTAGE_2V = 2;

    /**
     * 12V 规格对应的标称电压。
     */
    public static final int VOLTAGE_12V = 12;
}
