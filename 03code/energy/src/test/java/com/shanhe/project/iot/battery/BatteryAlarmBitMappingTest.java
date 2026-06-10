package com.shanhe.project.iot.battery;

import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BatteryAlarmBitMappingTest {

    @Test
    void shouldMapBinaryStringIndexToPhysicalBit() {
        assertEquals("00000001", CodingUtil.hexString2binaryString("01"));
        assertEquals(7, BatteryAlarmBitMapping.binaryStringIndexToPhysicalBit(0));
        assertEquals(0, BatteryAlarmBitMapping.binaryStringIndexToPhysicalBit(7));
        assertEquals(7, BatteryAlarmBitMapping.physicalBitToBinaryStringIndex(0));
        assertEquals(0, BatteryAlarmBitMapping.physicalBitToBinaryStringIndex(7));
    }

    @Test
    void shouldBuildGroup87EffectiveStatusByRemovingLevelBits() {
        assertEquals(
                "10000000000001",
                BatteryAlarmBitMapping.group87EffectiveStatus("00100000", "00000001")
        );
    }

    @Test
    void shouldMapGroup87EffectiveIndexToSourceByteAndPhysicalBit() {
        BatteryAlarmBitMapping.Group87Bit firstEffectiveBit =
                BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(0);
        assertEquals(1, firstEffectiveBit.getStatusByteNo());
        assertEquals(5, firstEffectiveBit.getPhysicalBit());

        BatteryAlarmBitMapping.Group87Bit lastFirstByteBit =
                BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(5);
        assertEquals(1, lastFirstByteBit.getStatusByteNo());
        assertEquals(0, lastFirstByteBit.getPhysicalBit());

        BatteryAlarmBitMapping.Group87Bit firstSecondByteBit =
                BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(6);
        assertEquals(2, firstSecondByteBit.getStatusByteNo());
        assertEquals(7, firstSecondByteBit.getPhysicalBit());

        BatteryAlarmBitMapping.Group87Bit lastEffectiveBit =
                BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(13);
        assertEquals(2, lastEffectiveBit.getStatusByteNo());
        assertEquals(0, lastEffectiveBit.getPhysicalBit());
    }

    @Test
    void shouldMapAllGroup87EffectiveBitsInOrder() {
        int[][] expected = new int[][]{
                {1, 5},
                {1, 4},
                {1, 3},
                {1, 2},
                {1, 1},
                {1, 0},
                {2, 7},
                {2, 6},
                {2, 5},
                {2, 4},
                {2, 3},
                {2, 2},
                {2, 1},
                {2, 0}
        };

        for (int i = 0; i < expected.length; i++) {
            BatteryAlarmBitMapping.Group87Bit bit =
                    BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(i);
            assertEquals(expected[i][0], bit.getStatusByteNo());
            assertEquals(expected[i][1], bit.getPhysicalBit());
        }
    }

    @Test
    void shouldRejectInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> BatteryAlarmBitMapping.binaryStringIndexToPhysicalBit(8));
        assertThrows(IllegalArgumentException.class,
                () -> BatteryAlarmBitMapping.group87EffectiveStatus("1000000", "00000001"));
        assertThrows(IllegalArgumentException.class,
                () -> BatteryAlarmBitMapping.group87EffectiveIndexToPhysicalBit(14));
    }
}
