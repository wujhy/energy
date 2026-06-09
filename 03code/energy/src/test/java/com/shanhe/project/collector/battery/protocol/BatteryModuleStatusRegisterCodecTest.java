package com.shanhe.project.collector.battery.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleStatusRegisterCodecTest {

    @Test
    void shouldComposeAndSplitM460BatteryStateRegister() {
        int register = BatteryModuleStatusRegisterCodec.compose(0x35, 0x102);

        Assertions.assertEquals(0x0502, register);
        Assertions.assertEquals(0x05, BatteryModuleStatusRegisterCodec.batteryPackStatus(register));
        Assertions.assertEquals(0x02, BatteryModuleStatusRegisterCodec.resistanceTestStatus(register));
    }

    @Test
    void shouldUseZeroForMissingStatusFields() {
        int register = BatteryModuleStatusRegisterCodec.compose(null, null);

        Assertions.assertEquals(0, register);
        Assertions.assertEquals(0, BatteryModuleStatusRegisterCodec.batteryPackStatus(register));
        Assertions.assertEquals(0, BatteryModuleStatusRegisterCodec.resistanceTestStatus(register));
    }
}
