package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.iot.service.DataService;
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
    void shouldProcessAndSyncOnlyCurrentBatch() {
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        DataService dataService = Mockito.mock(DataService.class);

        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setPackParam(Collections.singletonMap("packVoltage", "220.1"));
        reportLog.setBatteryList(Collections.singletonList(new BatteryMonitor()));
        Mockito.when(adapterService.buildReportLog(Mockito.eq(1), Mockito.any(), Mockito.any()))
                .thenReturn(reportLog);
        Mockito.when(dataService.isInsert("1")).thenReturn(true);

        CompatReportLogSyncProcessor processor = processor(adapterService, reportLogService, dataService);
        BatteryRealtimePostProcessContext context = context("batch-1", "batch-1", "batch-1");

        Assertions.assertTrue(processor.shouldProcess(context));
        processor.process(context);

        ArgumentCaptor<Map> packParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(reportLogService).insert(Mockito.eq(1), packParamCaptor.capture(), Mockito.any(), Mockito.eq(true));
        Assertions.assertEquals("220.1", packParamCaptor.getValue().get("packVoltage"));
    }

    @Test
    void shouldRejectWhenCompatReportLogDisabled() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setCompatReportLogEnabled(Boolean.FALSE);
        CompatReportLogSyncProcessor processor = new CompatReportLogSyncProcessor();
        ReflectionTestUtils.setField(processor, "properties", properties);

        Assertions.assertFalse(processor.shouldProcess(context("batch-1", "batch-1", "batch-1")));
    }

    private CompatReportLogSyncProcessor processor() {
        return processor(
                Mockito.mock(BatteryModuleReportLogAdapterService.class),
                Mockito.mock(BatteryReportLogService.class),
                Mockito.mock(DataService.class));
    }

    private CompatReportLogSyncProcessor processor(BatteryModuleReportLogAdapterService adapterService,
                                                    BatteryReportLogService reportLogService,
                                                    DataService dataService) {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setCompatReportLogEnabled(Boolean.TRUE);
        CompatReportLogSyncProcessor processor = new CompatReportLogSyncProcessor();
        ReflectionTestUtils.setField(processor, "properties", properties);
        ReflectionTestUtils.setField(processor, "adapterService", adapterService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(processor, "dataService", dataService);
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
