package com.shanhe.project.iot.battery;

import com.alibaba.fastjson.JSONObject;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.AlarmLevelEnum;
import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.iot.model.BatteryWarnInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

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

    @Test
    void shouldUploadSingleBatteryWarnUsingGroup87EffectiveBitMapping() {
        BatteryAlarmHandler handler = new BatteryAlarmHandler();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        Mockito.when(batteryReportLogService.lastCache(1)).thenReturn(batteryReportLog);
        ReflectionTestUtils.setField(handler, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", batteryReportLogService);

        String payload = "0101" +
                "4000" +
                "8000" +
                "C000" +
                "024040" +
                "028000" +
                "02C000";
        DeviceData deviceData = new DeviceData();
        deviceData.setInfo(buildFrame("87", payload));

        handler.uploadBatteryWarnData(new Config(), deviceData);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBattery(
                Mockito.any(Config.class),
                Mockito.eq(1),
                Mockito.eq(2),
                warnParamCaptor.capture(),
                Mockito.same(batteryReportLog));
        Assertions.assertEquals(
                AlarmLevelEnum._1.getDictValue(),
                warnParamCaptor.getValue().get(ItemCode.DTLJTGJ.getCode()));
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
