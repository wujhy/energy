package com.shanhe.project.iot.battery;

/**
 * M460 87/8D alarm bit mapping helpers（旧兼容工具类）
 *
 * <p>本类为旧 JSON/TCP 87/8D 告警 bit 映射工具，
 * 仅服务于旧 {@code BatteryAlarmHandler} 的兼容解析链路。
 * 新告警能力一律进入 {@code com.shanhe.project.collector.battery}。</p>
 *
 * <p>energy parses one status byte with {@code CodingUtil.hexString2binaryString},
 * so index 0 points to physical bit7 and index 7 points to physical bit0.</p>
 *
 * @author wjh
 * @since 2026/06/26
 */
public final class BatteryAlarmBitMapping {

    public static final int BITS_PER_BYTE = 8;
    public static final int GROUP_87_LEVEL_BITS = 2;

    private BatteryAlarmBitMapping() {
    }

    public static int binaryStringIndexToPhysicalBit(int index) {
        validateBitIndex(index, "index");
        return BITS_PER_BYTE - 1 - index;
    }

    public static int physicalBitToBinaryStringIndex(int physicalBit) {
        validateBitIndex(physicalBit, "physicalBit");
        return BITS_PER_BYTE - 1 - physicalBit;
    }

    public static String group87EffectiveStatus(String status1, String status2) {
        validateBinaryByte(status1, "status1");
        validateBinaryByte(status2, "status2");
        return status1.substring(GROUP_87_LEVEL_BITS) + status2;
    }

    public static Group87Bit group87EffectiveIndexToPhysicalBit(int effectiveIndex) {
        if (effectiveIndex < 0 || effectiveIndex >= 14) {
            throw new IllegalArgumentException("effectiveIndex must be in range 0..13");
        }
        if (effectiveIndex < BITS_PER_BYTE - GROUP_87_LEVEL_BITS) {
            int status1Index = effectiveIndex + GROUP_87_LEVEL_BITS;
            return new Group87Bit(1, binaryStringIndexToPhysicalBit(status1Index));
        }
        int status2Index = effectiveIndex - (BITS_PER_BYTE - GROUP_87_LEVEL_BITS);
        return new Group87Bit(2, binaryStringIndexToPhysicalBit(status2Index));
    }

    private static void validateBitIndex(int index, String name) {
        if (index < 0 || index >= BITS_PER_BYTE) {
            throw new IllegalArgumentException(name + " must be in range 0..7");
        }
    }

    private static void validateBinaryByte(String value, String name) {
        if (value == null || value.length() != BITS_PER_BYTE || !value.matches("[01]{8}")) {
            throw new IllegalArgumentException(name + " must be an 8-bit binary string");
        }
    }

    public static final class Group87Bit {

        private final int statusByteNo;
        private final int physicalBit;

        private Group87Bit(int statusByteNo, int physicalBit) {
            this.statusByteNo = statusByteNo;
            this.physicalBit = physicalBit;
        }

        public int getStatusByteNo() {
            return statusByteNo;
        }

        public int getPhysicalBit() {
            return physicalBit;
        }
    }
}
