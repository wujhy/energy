package com.shanhe.project.iot.battery;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.project.iot.model.BatteryWarnInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryAlarmHandlerTest {

    @Test
    void shouldDecodeBatteryWarnFrame() {
        String payload = "0101" +
                "4000" +
                "8000" +
                "C000" +
                "024000" +
                "028000" +
                "02C000";

        BatteryWarnInfo warnInfo = BatteryAlarmHandler.toWarnDecoder(buildFrame("87", payload));

        Assertions.assertNotNull(warnInfo);
        Assertions.assertEquals(1, warnInfo.getBatteryPackNumber());
        Assertions.assertEquals(1, warnInfo.getAlarmBatterySum());
        Assertions.assertNotNull(warnInfo.getPackStatus());
        Assertions.assertNotNull(warnInfo.getPackStatus().getJSONObject("commonly"));
        Assertions.assertNotNull(warnInfo.getPackStatus().getJSONObject("abnormal"));
        Assertions.assertNotNull(warnInfo.getPackStatus().getJSONObject("serious"));
        Assertions.assertEquals(1, warnInfo.getPackBatteryStatus().size());

        JSONObject batteryStatus = warnInfo.getPackBatteryStatus().getJSONObject(0);
        Assertions.assertEquals(2, batteryStatus.getInteger("batteryNumber"));
        Assertions.assertNotNull(batteryStatus.getJSONObject("commonly"));
        Assertions.assertNotNull(batteryStatus.getJSONObject("abnormal"));
        Assertions.assertNotNull(batteryStatus.getJSONObject("serious"));
    }

    @Test
    void shouldDecodeDeviceFaultFrame() {
        String payload = "0101" +
                "02A0" +
                "04";

        BatteryWarnInfo warnInfo = BatteryAlarmHandler.toFailDecoder(buildFrame("8D", payload));

        Assertions.assertNotNull(warnInfo);
        Assertions.assertEquals(1, warnInfo.getBatteryPackNumber());
        Assertions.assertEquals(1, warnInfo.getAlarmBatterySum());
        Assertions.assertEquals("00000100", warnInfo.getDeviceFaultStatus());
        Assertions.assertEquals(1, warnInfo.getDeviceFaultBatteryStatus().size());
        JSONObject batteryStatus = warnInfo.getDeviceFaultBatteryStatus().getJSONObject(0);
        Assertions.assertEquals(2, batteryStatus.getInteger("batteryNumber"));
        Assertions.assertEquals("10100000", batteryStatus.getString("status"));
    }

    private String buildFrame(String command, String payloadHex) {
        int payloadLength = payloadHex.length() / 2;
        return "5354415254" +
                "01" +
                command +
                String.format("%02X", payloadLength) +
                payloadHex +
                "00" +
                "0D";
    }
}
