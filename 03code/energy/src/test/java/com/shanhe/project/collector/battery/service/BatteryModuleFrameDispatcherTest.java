package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class BatteryModuleFrameDispatcherTest {

    private final BatteryModuleFrameDispatcher dispatcher = new BatteryModuleFrameDispatcher();

    @Test
    void shouldDispatchToAllConsumers() {
        BatteryModuleFrameSummaryService summaryService = Mockito.mock(BatteryModuleFrameSummaryService.class);
        BatteryModuleFrameSummary summary = BatteryModuleFrameSummary.builder()
                .known(true).success(true).moduleAddress(8).payloadLength(10).build();
        Mockito.when(summaryService.summarize(Mockito.any())).thenReturn(summary);
        ReflectionTestUtils.setField(dispatcher, "summaryService", summaryService);

        BatteryModuleFrameConsumer consumer1 = Mockito.mock(BatteryModuleFrameConsumer.class);
        BatteryModuleFrameConsumer consumer2 = Mockito.mock(BatteryModuleFrameConsumer.class);
        ReflectionTestUtils.setField(dispatcher, "consumers", new ArrayList<>(Arrays.asList(consumer1, consumer2)));

        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("test-channel");
        BatteryCollectorFrame frame = BatteryCollectorFrame.builder().command(0x0F).build();

        dispatcher.dispatch(config, frame);

        Mockito.verify(consumer1).consume(config, frame, summary);
        Mockito.verify(consumer2).consume(config, frame, summary);
    }

    @Test
    void shouldHandleNullFrameWithoutException() {
        BatteryModuleFrameSummaryService summaryService = Mockito.mock(BatteryModuleFrameSummaryService.class);
        Mockito.when(summaryService.summarize(null)).thenReturn(null);
        ReflectionTestUtils.setField(dispatcher, "summaryService", summaryService);

        BatteryModuleFrameConsumer consumer = Mockito.mock(BatteryModuleFrameConsumer.class);
        ReflectionTestUtils.setField(dispatcher, "consumers", new ArrayList<>(Collections.singletonList(consumer)));

        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("test-channel");

        Assertions.assertDoesNotThrow(() -> dispatcher.dispatch(config, null));
        Mockito.verify(consumer).consume(config, null, null);
    }

    @Test
    void shouldHandleEmptyConsumerList() {
        BatteryModuleFrameSummaryService summaryService = Mockito.mock(BatteryModuleFrameSummaryService.class);
        Mockito.when(summaryService.summarize(Mockito.any())).thenReturn(
                BatteryModuleFrameSummary.builder().known(false).build());
        ReflectionTestUtils.setField(dispatcher, "summaryService", summaryService);
        ReflectionTestUtils.setField(dispatcher, "consumers", new ArrayList<>());

        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("test-channel");
        BatteryCollectorFrame frame = BatteryCollectorFrame.builder().command(0x02).build();

        Assertions.assertDoesNotThrow(() -> dispatcher.dispatch(config, frame));
    }

    @Test
    void shouldHandleNullChannelConfig() {
        BatteryModuleFrameSummaryService summaryService = Mockito.mock(BatteryModuleFrameSummaryService.class);
        BatteryModuleFrameSummary summary = BatteryModuleFrameSummary.builder()
                .known(true).success(true).build();
        Mockito.when(summaryService.summarize(Mockito.any())).thenReturn(summary);
        ReflectionTestUtils.setField(dispatcher, "summaryService", summaryService);

        BatteryModuleFrameConsumer consumer = Mockito.mock(BatteryModuleFrameConsumer.class);
        ReflectionTestUtils.setField(dispatcher, "consumers", new ArrayList<>(Collections.singletonList(consumer)));

        BatteryCollectorFrame frame = BatteryCollectorFrame.builder().command(0x82).build();

        Assertions.assertDoesNotThrow(() -> dispatcher.dispatch(null, frame));
        Mockito.verify(consumer).consume(null, frame, summary);
    }

    @Test
    void shouldPassNullSummaryWhenSummaryServiceReturnsNull() {
        BatteryModuleFrameSummaryService summaryService = Mockito.mock(BatteryModuleFrameSummaryService.class);
        Mockito.when(summaryService.summarize(Mockito.any())).thenReturn(null);
        ReflectionTestUtils.setField(dispatcher, "summaryService", summaryService);

        BatteryModuleFrameConsumer consumer = Mockito.mock(BatteryModuleFrameConsumer.class);
        ReflectionTestUtils.setField(dispatcher, "consumers", new ArrayList<>(Collections.singletonList(consumer)));

        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("test-channel");
        BatteryCollectorFrame frame = BatteryCollectorFrame.builder().command(0xFF).build();

        dispatcher.dispatch(config, frame);

        Mockito.verify(consumer).consume(config, frame, null);
    }
}
