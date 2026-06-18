package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessor;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.postprocess.StatisticsProcessor;
import com.shanhe.project.collector.battery.postprocess.OnlineStatusProcessor;
import com.shanhe.project.collector.battery.postprocess.VoltageRangeProcessor;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessService;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.capacity.service.BatteryPredictorService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatteryRealtimePostProcessorsTest {

    private static final String POLL_BATCH_NO = "batch-1";

    @Test
    void statisticsProcessorShouldAdaptRealtimeModel() {
        StatisticsProcessor processor = new StatisticsProcessor();
        IStatBatteryPackService statBatteryPackService = Mockito.mock(IStatBatteryPackService.class);
        ReflectionTestUtils.setField(processor, "statBatteryPackService", statBatteryPackService);

        BatteryRealtimePostProcessContext context = context(group(6, null), cells());
        assertTrue(processor.shouldProcess(context));

        processor.process(context);

        ArgumentCaptor<Map<String, Object>> packMapCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(statBatteryPackService).insertList(Mockito.eq(1), packMapCaptor.capture(), Mockito.anyList());
        assertEquals(6, packMapCaptor.getValue().get("batteryPackStatus"));
        assertEquals(3, packMapCaptor.getValue().get("deviceWorkStatus"));
        assertEquals(1, packMapCaptor.getValue().get("deviceWorkIOStatus"));
        assertEquals(53.2, packMapCaptor.getValue().get("packVoltage"));
    }

    @Test
    void statisticsProcessorShouldRejectMissingOrMismatchedBatch() {
        StatisticsProcessor processor = new StatisticsProcessor();
        List<BatteryModuleCellRealtime> cells = cells();

        BatteryRealtimePostProcessContext missingBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group(6, null))
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(missingBatch));

        cells.get(1).setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group(6, null))
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(mismatchedBatch));
    }

    @Test
    void operationLogProcessorShouldSkipUnknownStatus() {
        OperationLogProcessor processor = new OperationLogProcessor();
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        ReflectionTestUtils.setField(processor, "optLogService", optLogService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        BatteryRealtimePostProcessContext context = context(group(99, null), cells());
        assertTrue(processor.shouldProcess(context));
        processor.process(context);

        Mockito.verifyNoInteractions(optLogService);
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void operationLogProcessorShouldCallOldServiceForKnownStatus() {
        OperationLogProcessor processor = new OperationLogProcessor();
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog oldInfo = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(oldInfo);
        ReflectionTestUtils.setField(processor, "optLogService", optLogService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        BatteryRealtimePostProcessContext context = context(group(6, null), cells());
        assertTrue(processor.shouldProcess(context));
        processor.process(context);

        Mockito.verify(reportLogService).lastCache(1);
        Mockito.verify(optLogService).insertBattery(Mockito.eq(1), Mockito.anyMap(), Mockito.same(oldInfo));
    }

    @Test
    void operationLogProcessorShouldIgnoreCompatInsertOldReportLogBoundary() {
        OperationLogProcessor processor = new OperationLogProcessor();
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog contextOldInfo = new BatteryReportLog();
        BatteryReportLog cachedOldInfo = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(cachedOldInfo);
        ReflectionTestUtils.setField(processor, "optLogService", optLogService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        BatteryRealtimePostProcessContext context = context(group(6, null), cells());
        context.setCompatInsert(true);
        context.setOldReportLog(contextOldInfo);

        assertTrue(processor.shouldProcess(context));
        processor.process(context);

        Mockito.verify(reportLogService).lastCache(1);
        Mockito.verify(optLogService).insertBattery(Mockito.eq(1), Mockito.anyMap(), Mockito.same(cachedOldInfo));
    }

    @Test
    void operationLogProcessorShouldRejectMissingOrMismatchedBatch() {
        OperationLogProcessor processor = new OperationLogProcessor();

        BatteryRealtimePostProcessContext missingBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group(6, null))
                .cells(cells())
                .build();
        assertFalse(processor.shouldProcess(missingBatch));

        BatteryModuleGroupRealtime mismatchedGroup = group(6, null);
        mismatchedGroup.setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(mismatchedGroup)
                .cells(cells())
                .build();
        assertFalse(processor.shouldProcess(mismatchedBatch));

        List<BatteryModuleCellRealtime> mismatchedCells = cells();
        mismatchedCells.get(0).setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedCellBatch = context(group(6, null), mismatchedCells);
        assertFalse(processor.shouldProcess(mismatchedCellBatch));
    }

    @Test
    void capacityPredictionProcessorShouldTriggerForKnownStatusInSameBatch() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryPredictorService predictorService = Mockito.mock(BatteryPredictorService.class);
        ReflectionTestUtils.setField(processor, "batteryPredictorService", predictorService);

        BatteryRealtimePostProcessContext backupContext = context(group(5, null), cells());
        BatteryRealtimePostProcessContext idleContext = context(group(6, null), cells());
        assertTrue(processor.shouldProcess(backupContext));
        assertTrue(processor.shouldProcess(idleContext));

        processor.process(backupContext);
        processor.process(idleContext);

        ArgumentCaptor<BatteryReportLog> oldInfoCaptor = ArgumentCaptor.forClass(BatteryReportLog.class);
        ArgumentCaptor<BatteryReportLog> currentInfoCaptor = ArgumentCaptor.forClass(BatteryReportLog.class);
        Mockito.verify(predictorService).doTotalBatteryStep(Mockito.eq(1), Mockito.eq("6"),
                oldInfoCaptor.capture(), currentInfoCaptor.capture());
        assertEquals(5, oldInfoCaptor.getValue().getPackParam().get("batteryPackStatus"));
        assertEquals(6, currentInfoCaptor.getValue().getPackParam().get("batteryPackStatus"));
        assertEquals(2, currentInfoCaptor.getValue().getBatteryList().size());
    }

    @Test
    void capacityPredictionProcessorShouldRejectMissingOrMismatchedBatch() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();

        BatteryRealtimePostProcessContext missingBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group(5, null))
                .cells(cells())
                .build();
        assertFalse(processor.shouldProcess(missingBatch));

        BatteryModuleGroupRealtime mismatchedGroup = group(5, null);
        mismatchedGroup.setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(mismatchedGroup)
                .cells(cells())
                .build();
        assertFalse(processor.shouldProcess(mismatchedBatch));

        List<BatteryModuleCellRealtime> mismatchedCells = cells();
        mismatchedCells.get(0).setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedCellBatch = context(group(5, null), mismatchedCells);
        assertFalse(processor.shouldProcess(mismatchedCellBatch));
    }

    @Test
    void capacityPredictionProcessorShouldSkipUnknownStatus() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryPredictorService predictorService = Mockito.mock(BatteryPredictorService.class);
        ReflectionTestUtils.setField(processor, "batteryPredictorService", predictorService);

        BatteryRealtimePostProcessContext context = context(group(99, null), cells());
        assertTrue(processor.shouldProcess(context));
        processor.process(context);

        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void resistanceStatisticsProcessorShouldSkipWhenStatusMissing() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        BatteryRealtimePostProcessContext context = context(group(6, null), cells());
        assertFalse(processor.shouldProcess(context));

        processor.process(context);

        Mockito.verifyNoInteractions(statBatteryResService);
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void resistanceStatisticsProcessorShouldCallOldServiceWhenStatusExists() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog oldInfo = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(oldInfo);
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryPack pack = new BatteryPack();
        pack.setBatSinSize(2);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(processor, "batteryPackService", batteryPackService);

        List<BatteryModuleCellRealtime> cells = cells();
        cells.get(0).setResistanceRageSlip(9000.0d);

        BatteryRealtimePostProcessContext context = context(group(6, 2), cells);
        assertTrue(processor.shouldProcess(context));

        processor.process(context);

        ArgumentCaptor<Map<String, Object>> packMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List> batteryListCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(statBatteryResService).init(Mockito.eq(1), packMapCaptor.capture(), batteryListCaptor.capture(), Mockito.same(oldInfo));
        assertEquals(2, packMapCaptor.getValue().get("resistanceTestStatus"));
        List<BatteryMonitor> adaptedCells = batteryListCaptor.getValue();
        assertEquals(110, adaptedCells.get(0).getResistance());
        assertNull(adaptedCells.get(0).getResistancerageslip());
    }

    @Test
    void resistanceStatisticsProcessorShouldSkipIncompleteCellCountOrResistance() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryPack pack = new BatteryPack();
        pack.setBatSinSize(2);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(processor, "batteryPackService", batteryPackService);

        processor.process(context(group(6, 7), Arrays.asList(cells().get(0))));
        Mockito.verifyNoInteractions(statBatteryResService);
        Mockito.verifyNoInteractions(reportLogService);

        List<BatteryModuleCellRealtime> cells = cells();
        cells.get(1).setResistance(null);
        processor.process(context(group(6, 7), cells));

        Mockito.verifyNoInteractions(statBatteryResService);
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void resistanceStatisticsProcessorShouldSkipDuplicateBatch() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog oldInfo = new BatteryReportLog();
        Mockito.when(reportLogService.lastCache(1)).thenReturn(oldInfo);
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        BatteryRealtimePostProcessContext context = context(group(6, 7), cells());

        processor.process(context);
        processor.process(context);

        Mockito.verify(statBatteryResService, Mockito.times(1))
                .init(Mockito.eq(1), Mockito.anyMap(), Mockito.anyList(), Mockito.same(oldInfo));
    }

    @Test
    void resistanceStatisticsProcessorShouldRejectMissingOrMismatchedBatch() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        List<BatteryModuleCellRealtime> cells = cells();

        BatteryRealtimePostProcessContext missingBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group(6, 2))
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(missingBatch));

        cells.get(0).setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group(6, 2))
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(mismatchedBatch));
    }

    @Test
    void voltageRangeProcessorShouldUpdatePackFromCellVoltages() {
        VoltageRangeProcessor processor = new VoltageRangeProcessor();
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        BatteryPack pack = new BatteryPack();
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        ReflectionTestUtils.setField(processor, "batteryPackService", batteryPackService);

        processor.process(context(group(6, null), cells()));

        Mockito.verify(batteryPackService).update(pack);
        assertEquals(600, pack.getVoltageRange());
    }

    @Test
    void voltageRangeProcessorShouldRejectMissingOrMismatchedBatch() {
        VoltageRangeProcessor processor = new VoltageRangeProcessor();
        List<BatteryModuleCellRealtime> cells = cells();

        BatteryRealtimePostProcessContext missingBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(missingBatch));

        cells.get(1).setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext mismatchedBatch = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .cells(cells)
                .build();
        assertFalse(processor.shouldProcess(mismatchedBatch));
    }

    @Test
    void postProcessServiceShouldOrderProcessorsAndContinueAfterFailure() {
        BatteryRealtimePostProcessService service = new BatteryRealtimePostProcessService();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .build();
        List<String> executed = new ArrayList<>();
        BatteryRealtimePostProcessor first = processor("first", 10, executed, false);
        BatteryRealtimePostProcessor failing = processor("failing", 20, executed, true);
        BatteryRealtimePostProcessor afterFailure = processor("afterFailure", 30, executed, false);
        ReflectionTestUtils.setField(service, "processors", Arrays.asList(afterFailure, failing, first));

        service.execute(context);

        assertEquals(Arrays.asList("first", "failing", "afterFailure"), executed);
    }

    @Test
    void processorShouldProcessGuardsShouldRejectIncompleteContext() {
        StatisticsProcessor statisticsProcessor = new StatisticsProcessor();
        VoltageRangeProcessor voltageRangeProcessor = new VoltageRangeProcessor();
        BatteryRealtimePostProcessContext empty = BatteryRealtimePostProcessContext.builder().packNum(1).build();

        assertTrue(!statisticsProcessor.shouldProcess(empty));
        assertTrue(!voltageRangeProcessor.shouldProcess(empty));
    }

    private BatteryRealtimePostProcessContext context(BatteryModuleGroupRealtime group,
                                                      List<BatteryModuleCellRealtime> cells) {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group)
                .cells(cells)
                .build();
    }

    private BatteryModuleGroupRealtime group(Integer batteryPackStatus, Integer resistanceTestStatus) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPackVoltage(53.2);
        group.setBatteryPackStatus(batteryPackStatus);
        group.setResistanceTestStatus(resistanceTestStatus);
        group.setDeviceWorkStatus(3);
        group.setDeviceWorkIoStatus(1);
        group.setPollBatchNo(POLL_BATCH_NO);
        return group;
    }

    private List<BatteryModuleCellRealtime> cells() {
        BatteryModuleCellRealtime first = new BatteryModuleCellRealtime();
        first.setPackNum(1);
        first.setBatNum(1);
        first.setVoltage(2.1);
        first.setResistance(110);
        first.setPollBatchNo(POLL_BATCH_NO);

        BatteryModuleCellRealtime second = new BatteryModuleCellRealtime();
        second.setPackNum(1);
        second.setBatNum(2);
        second.setVoltage(2.7);
        second.setResistance(120);
        second.setPollBatchNo(POLL_BATCH_NO);

        return Arrays.asList(first, second);
    }

    private BatteryRealtimePostProcessor processor(String name,
                                                   int order,
                                                   List<String> executed,
                                                   boolean fail) {
        return new BatteryRealtimePostProcessor() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public void process(BatteryRealtimePostProcessContext context) {
                executed.add(name);
                if (fail) {
                    throw new IllegalStateException("failed");
                }
            }
        };
    }
}
