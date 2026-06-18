package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryModuleCompatReportLogSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class CompatReportLogSyncProcessorTest {

    @Test
    void shouldRejectContextWithoutPollBatchNo() {
        CompatReportLogSyncProcessor processor = processor(Mockito.mock(BatteryModuleCompatReportLogSyncService.class));

        Assertions.assertFalse(processor.shouldProcess(null));
        Assertions.assertFalse(processor.shouldProcess(context(null, "batch-1", "batch-1")));
        Assertions.assertFalse(processor.shouldProcess(context(" ", "batch-1", "batch-1")));
    }

    @Test
    void shouldRejectMismatchedGroupOrCellBatch() {
        CompatReportLogSyncProcessor processor = processor(Mockito.mock(BatteryModuleCompatReportLogSyncService.class));

        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-2", "batch-1")));
        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", "batch-2")));
        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", null)));
    }

    @Test
    void shouldProcessAndSyncOnlyCurrentBatch() {
        BatteryModuleCompatReportLogSyncService syncService = Mockito.mock(BatteryModuleCompatReportLogSyncService.class);
        CompatReportLogSyncProcessor processor = processor(syncService);
        BatteryRealtimePostProcessContext context = context("batch-1", "batch-1", "batch-1");

        Assertions.assertTrue(processor.shouldProcess(context));
        processor.process(context);

        Mockito.verify(syncService).sync(context.getChannelConfig(), context.getGroup(), context.getCells());
    }

    @Test
    void shouldRejectWhenCompatReportLogDisabled() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setCompatReportLogEnabled(Boolean.FALSE);
        CompatReportLogSyncProcessor processor = new CompatReportLogSyncProcessor();
        ReflectionTestUtils.setField(processor, "properties", properties);
        ReflectionTestUtils.setField(processor, "compatReportLogSyncService",
                Mockito.mock(BatteryModuleCompatReportLogSyncService.class));

        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", "batch-1")));
    }

    private CompatReportLogSyncProcessor processor(BatteryModuleCompatReportLogSyncService syncService) {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setCompatReportLogEnabled(Boolean.TRUE);
        CompatReportLogSyncProcessor processor = new CompatReportLogSyncProcessor();
        ReflectionTestUtils.setField(processor, "properties", properties);
        ReflectionTestUtils.setField(processor, "compatReportLogSyncService", syncService);
        return processor;
    }

    private BatteryRealtimePostProcessContext context(String contextBatchNo,
                                                      String groupBatchNo,
                                                      String cellBatchNo) {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPollBatchNo(groupBatchNo);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(1);
        cell.setPollBatchNo(cellBatchNo);
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(contextBatchNo)
                .channelConfig(channelConfig)
                .group(group)
                .cells(Collections.singletonList(cell))
                .build();
    }
}
