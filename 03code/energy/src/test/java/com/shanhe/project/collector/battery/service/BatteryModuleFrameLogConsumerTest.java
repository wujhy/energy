package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameLog;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleFrameLogConsumerTest {

    private final BatteryCollectorFrameCodec codec = new BatteryCollectorFrameCodec();
    private final BatteryModuleFrameLogConsumer consumer = new BatteryModuleFrameLogConsumer();

    @Test
    void shouldBuildFrameLogWithParsedFields() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setPortName("ttyS9");
        channelConfig.setBatteryGroup(1);
        BatteryCollectorFrame frame = codec.buildRequest(0x05, 0x81,
                new byte[]{0x00, 0x09, (byte) 0xC4, 0x00, 0x64, 0x00, (byte) 0xFB, 0x01, 0x04, (byte) 0xD2});
        BatteryModuleFrameSummary summary = BatteryModuleFrameSummary.builder()
                .protocolCode(BatteryDeviceProtocolCode.MODULE_INFO)
                .known(true)
                .success(true)
                .responseFlag(0)
                .payloadLength(10)
                .payloadHex("0009C4006400FB0104D2")
                .build();
        BatteryModuleFrameData data = BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.SINGLE_MODULE_INFO)
                .moduleAddress(5)
                .success(true)
                .cellVoltage(2.5d)
                .internalResistance(100)
                .build();

        BatteryModuleFrameLog log = consumer.buildLog(channelConfig, frame, summary, data);

        Assertions.assertEquals("battery-group-1", log.getChannelName());
        Assertions.assertEquals("ttyS9", log.getPortName());
        Assertions.assertEquals(1, log.getBatteryGroup());
        Assertions.assertEquals(5, log.getModuleAddress());
        Assertions.assertEquals("81", log.getCommandCode());
        Assertions.assertEquals("SINGLE_MODULE_INFO", log.getParsedType());
        Assertions.assertEquals(2.5d, log.getCellVoltage(), 0.0001d);
        Assertions.assertEquals(100, log.getInternalResistance());
        Assertions.assertTrue(log.getKnown());
        Assertions.assertTrue(log.getSuccess());
    }
}
