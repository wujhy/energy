package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryStorageIntervalService;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;

class CompatReportLogSyncProcessorTest {

    @Test
    void shouldRejectContextWithoutPollBatchNo() {
        CompatReportLogSyncProcessor processor = processor();

        Assertions.assertFalse(processor.shouldProcess(null));
        Assertions.assertFalse(processor.shouldProcess(context(null, "batch-1", "batch-1")));
        Assertions.assertFalse(processor.shouldProcess(context(" ", "batch-1", "batch-1")));
    }

    @Test
    void shouldRejectMismatchedGroupOrCellBatch() {
        CompatReportLogSyncProcessor processor = processor();

        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-2", "batch-1")));
        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", "batch-2")));
        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", null)));
    }


    @Test
    void shouldInsertHistoryReportWhenStorageIntervalAllows() {
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryStorageIntervalService storageIntervalService = Mockito.mock(BatteryStorageIntervalService.class);
        Mockito.when(storageIntervalService.shouldInsert(1)).thenReturn(true);
        CompatReportLogSyncProcessor processor = processor(reportLogService, storageIntervalService);

        processor.process(context("batch-1", "batch-1", "batch-1"));

        ArgumentCaptor<Map<String, Object>> packParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(reportLogService).insert(Mockito.eq(1), packParamCaptor.capture(), Mockito.argThat(list ->
                list != null && list.size() == 1 && Integer.valueOf(1).equals(list.get(0).getBatNum())));
        Assertions.assertEquals(220.1d, packParamCaptor.getValue().get("packVoltage"));
    }
    private CompatReportLogSyncProcessor processor() {
        return processor(Mockito.mock(BatteryReportLogService.class), Mockito.mock(BatteryStorageIntervalService.class));
    }

    private CompatReportLogSyncProcessor processor(BatteryReportLogService reportLogService,
                                                   BatteryStorageIntervalService storageIntervalService) {
        CompatReportLogSyncProcessor processor = new CompatReportLogSyncProcessor();
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(processor, "storageIntervalService", storageIntervalService);
        return processor;
    }

    private BatteryRealtimePostProcessContext context(String contextBatchNo,
                                                      String groupBatchNo,
                                                      String cellBatchNo) {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPackVoltage(220.1d);
        group.setPollBatchNo(groupBatchNo);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(1);
        cell.setVoltage(2.1d);
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
