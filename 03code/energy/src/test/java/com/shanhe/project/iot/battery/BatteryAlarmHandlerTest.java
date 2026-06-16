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

import java.util.List;
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
    void shouldDecodeBatteryWarnPayloadInM460StructCopyOrder() {
        String payload = "0101" +
                "5001" +
                "A002" +
                "C408" +
                "036010" +
                "03A020" +
                "03C340";

        BatteryWarnInfo warnInfo = BatteryAlarmHandler.toWarnDecoder(buildFrame("87", payload));

        Assertions.assertNotNull(warnInfo);
        Assertions.assertEquals("01000000000001", BatteryAlarmBitMapping.group87EffectiveStatus(
                warnInfo.getPackStatus().getJSONObject("commonly").getString("status1"),
                warnInfo.getPackStatus().getJSONObject("commonly").getString("status2")));
        Assertions.assertEquals("10000000000010", BatteryAlarmBitMapping.group87EffectiveStatus(
                warnInfo.getPackStatus().getJSONObject("abnormal").getString("status1"),
                warnInfo.getPackStatus().getJSONObject("abnormal").getString("status2")));
        Assertions.assertEquals("00010000001000", BatteryAlarmBitMapping.group87EffectiveStatus(
                warnInfo.getPackStatus().getJSONObject("serious").getString("status1"),
                warnInfo.getPackStatus().getJSONObject("serious").getString("status2")));

        JSONObject batteryStatus = warnInfo.getPackBatteryStatus().getJSONObject(0);
        Assertions.assertEquals(3, batteryStatus.getInteger("batteryNumber"));
        Assertions.assertEquals("10000000010000", BatteryAlarmBitMapping.group87EffectiveStatus(
                batteryStatus.getJSONObject("commonly").getString("status1"),
                batteryStatus.getJSONObject("commonly").getString("status2")));
        Assertions.assertEquals("10000000100000", BatteryAlarmBitMapping.group87EffectiveStatus(
                batteryStatus.getJSONObject("abnormal").getString("status1"),
                batteryStatus.getJSONObject("abnormal").getString("status2")));
        Assertions.assertEquals("00001101000000", BatteryAlarmBitMapping.group87EffectiveStatus(
                batteryStatus.getJSONObject("serious").getString("status1"),
                batteryStatus.getJSONObject("serious").getString("status2")));
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
    @Test
    void shouldUploadBatteryGroupWarnWithCorrectMappings() {
        BatteryAlarmHandler handler = new BatteryAlarmHandler();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        Mockito.when(batteryReportLogService.lastCache(1)).thenReturn(batteryReportLog);
        ReflectionTestUtils.setField(handler, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", batteryReportLogService);

        // We want to test all commonly group alarm mappings:
        // status1 = hex "50" (index 1 = ZWDG)
        // status2 = hex "01" (index 13 = ZFCDYGG)
        // commonly = "5001"
        // abnormal = "6002" (status1 = "60" -> index 0 = ZWDD; status2 = "02" -> index 12 = ZFCDYGD)
        // serious = "4208" (status1 = "42" -> index 4 = ZDYGF; status2 = "08" -> index 10 = ZSOCDGJ)
        String payload = "0100" +
                "5001" +
                "A002" +
                "C208";
        DeviceData deviceData = new DeviceData();
        deviceData.setInfo(buildFrame("87", payload));

        handler.uploadBatteryWarnData(new Config(), deviceData);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        // Verify group alarm (modelNum is null)
        Mockito.verify(alarmLogService, Mockito.times(1)).alarmBattery(
                Mockito.any(Config.class),
                Mockito.eq(1),
                Mockito.isNull(),
                warnParamCaptor.capture(),
                Mockito.same(batteryReportLog));

        Map<String, String> warnParams = warnParamCaptor.getValue();

        // Verify commonlyParams maps correctly
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZWDG.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZFCDYGG.getCode()));

        // Verify abnormalParams maps correctly
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZWDD.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZFCDYGD.getCode()));

        // Verify seriousParams maps correctly
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZDYGF.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.ZSOCDGJ.getCode()));
    }

    @Test
    void shouldUploadSingleBatteryWarnWithCorrectMappings() {
        BatteryAlarmHandler handler = new BatteryAlarmHandler();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        Mockito.when(batteryReportLogService.lastCache(1)).thenReturn(batteryReportLog);
        ReflectionTestUtils.setField(handler, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", batteryReportLogService);

        // Test single battery commonly alarm mappings:
        // Commonly status1 = hex "60" -> index 0 = DTDCWDD
        // Commonly status2 = hex "01" -> index 13 = DTDYBJ
        // Commonly = "6001"
        // Abnormal = "5002" -> status1 = "50" -> index 1 = DTDCWDG; status2 = "02" -> index 12 = DTDCWDBJ
        // Serious = "4204" -> status1 = "42" -> index 4 = DTDYGF; status2 = "04" -> index 11 = DTNZBJ
        String payload = "0101" +
                "4000" +
                "8000" +
                "C000" +
                "026001" +
                "029002" +
                "02C204";
        DeviceData deviceData = new DeviceData();
        deviceData.setInfo(buildFrame("87", payload));

        handler.uploadBatteryWarnData(new Config(), deviceData);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService, Mockito.times(1)).alarmBattery(
                Mockito.any(Config.class),
                Mockito.eq(1),
                Mockito.eq(2),
                warnParamCaptor.capture(),
                Mockito.same(batteryReportLog));

        Map<String, String> warnParams = warnParamCaptor.getValue();

        // Verify commonlyParams
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTDCWDD.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTDYBJ.getCode()));

        // Verify abnormalParams
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTDCWDG.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTDCWDBJ.getCode()));

        // Verify seriousParams
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals(AlarmLevelEnum._1.getDictValue(), warnParams.get(ItemCode.DTNZBJ.getCode()));
    }

    @Test
    void shouldUploadDeviceFaultWithCorrectMappings() {
        BatteryAlarmHandler handler = new BatteryAlarmHandler();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        Mockito.when(batteryReportLogService.lastCache(1)).thenReturn(batteryReportLog);
        ReflectionTestUtils.setField(handler, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", batteryReportLogService);

        // dfs = hex "AC" (binary "10101100")
        // Index 0: ZWLGZ (1)
        // Index 2: ZTDGJ (1)
        // Index 3: ZWDCGQ2GZ (0)
        // Index 4: ZWDCGQ1GZ (1)
        // Index 5: TXZT (1)
        String payload = "0100" +
                "AC";
        DeviceData deviceData = new DeviceData();
        deviceData.setInfo(buildFrame("8D", payload));

        handler.deviceFaultAlarmUpload(new Config(), deviceData);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBattery(
                Mockito.any(Config.class),
                Mockito.eq(1),
                Mockito.isNull(),
                warnParamCaptor.capture(),
                Mockito.same(batteryReportLog));

        Map<String, String> warnParam = warnParamCaptor.getValue();
        Assertions.assertEquals("1", warnParam.get(ItemCode.ZWLGZ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.ZTDGJ.getCode()));
        Assertions.assertEquals("0", warnParam.get(ItemCode.ZWDCGQ2GZ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.ZWDCGQ1GZ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldUploadSingleBatteryFaultWithCorrectMappings() {
        BatteryAlarmHandler handler = new BatteryAlarmHandler();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog batteryReportLog = new BatteryReportLog();
        Mockito.when(batteryReportLogService.lastCache(1)).thenReturn(batteryReportLog);
        ReflectionTestUtils.setField(handler, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(handler, "batteryReportLogService", batteryReportLogService);

        // batteryNumber = 2
        // status = hex "D8" (binary "11011000")
        // Index 0: DTLJTGJ (1)
        // Index 1: DTGB (1)
        // Index 2: DTLYGJ (0)
        // Index 3: DTWDCGQGZ (1)
        // Index 4: DTTXZT (1)
        String payload = "0101" +
                "02D8" +
                "00";
        DeviceData deviceData = new DeviceData();
        deviceData.setInfo(buildFrame("8D", payload));

        handler.deviceFaultAlarmUpload(new Config(), deviceData);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        // Verify battery 2 alarm
        Mockito.verify(alarmLogService).alarmBattery(
                Mockito.any(Config.class),
                Mockito.eq(1),
                Mockito.eq(2),
                warnParamCaptor.capture(),
                Mockito.same(batteryReportLog));

        Map<String, String> warnParam = warnParamCaptor.getValue();
        Assertions.assertEquals("1", warnParam.get(ItemCode.DTLJTGJ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.DTGB.getCode()));
        Assertions.assertEquals("0", warnParam.get(ItemCode.DTLYGJ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.DTWDCGQGZ.getCode()));
        Assertions.assertEquals("1", warnParam.get(ItemCode.DTTXZT.getCode()));
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
