package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BatteryModuleFrameSummaryServiceTest {

    private final BatteryCollectorFrameCodec codec = new BatteryCollectorFrameCodec();
    private final BatteryModuleFrameSummaryService service = new BatteryModuleFrameSummaryService();

    @Test
    void shouldSummarizeStatusFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x82, new byte[]{0x00, 0x03, 0x12, 0x34});

        BatteryModuleFrameSummary summary = service.summarize(frame);

        Assertions.assertNotNull(summary);
        Assertions.assertTrue(summary.isKnown());
        Assertions.assertTrue(summary.isSuccess());
        Assertions.assertEquals(1, summary.getModuleAddress());
        Assertions.assertEquals(4, summary.getPayloadLength());
    }

    @Test
    void shouldTreatModuleInfoAsDataFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x81, new byte[]{0x01, 0x02, 0x03});

        BatteryModuleFrameSummary summary = service.summarize(frame);

        Assertions.assertNotNull(summary);
        Assertions.assertTrue(summary.isKnown());
        Assertions.assertTrue(summary.isSuccess());
        Assertions.assertEquals(3, summary.getPayloadLength());
    }

    @Test
    void shouldSummarizeDataFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x02, 0x91, new byte[]{0x12, 0x34, 0x56, 0x78});

        BatteryModuleFrameSummary summary = service.summarize(frame);

        Assertions.assertNotNull(summary);
        Assertions.assertTrue(summary.isKnown());
        Assertions.assertTrue(summary.isSuccess());
        Assertions.assertEquals(2, summary.getModuleAddress());
        Assertions.assertEquals(4, summary.getPayloadLength());
    }

    @Test
    void shouldReturnUnknownSummaryForUnsupportedCode() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x55, new byte[]{0x00});

        BatteryModuleFrameSummary summary = service.summarize(frame);

        Assertions.assertNotNull(summary);
        Assertions.assertFalse(summary.isKnown());
        Assertions.assertFalse(summary.isSuccess());
    }
}
