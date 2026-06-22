package com.shanhe.project.collector.battery.protocol;

/**
 * M460 电池组状态寄存器编码工具。
 *
 * @author wjh
 * @since 2026-06-18
 */
public final class BatteryModuleStatusRegisterCodec {

    private static final int BATTERY_STATUS_MASK = 0x0F;
    private static final int TEST_STATUS_MASK = 0xFF;

    private BatteryModuleStatusRegisterCodec() {
    }

    /**
     * M460 Battery_State_Register：高字节低 4 位为电池组状态，低字节为内阻测试状态。
     */
    public static int compose(Integer batteryPackStatus, Integer resistanceTestStatus) {
        int batteryStatus = batteryPackStatus == null ? 0 : batteryPackStatus & BATTERY_STATUS_MASK;
        int testStatus = resistanceTestStatus == null ? 0 : resistanceTestStatus & TEST_STATUS_MASK;
        return (batteryStatus << 8) | testStatus;
    }

    public static int batteryPackStatus(int registerValue) {
        return (registerValue >> 8) & BATTERY_STATUS_MASK;
    }

    public static int resistanceTestStatus(int registerValue) {
        return registerValue & TEST_STATUS_MASK;
    }
}
