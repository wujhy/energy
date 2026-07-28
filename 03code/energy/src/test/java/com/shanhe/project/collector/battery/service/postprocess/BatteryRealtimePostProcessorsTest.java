package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessor;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.postprocess.CapacityPredictionProcessor;
import com.shanhe.project.collector.battery.postprocess.StatisticsProcessor;
import com.shanhe.project.collector.battery.postprocess.VoltageRangeProcessor;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessService;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.capacity.service.BatteryPredictorService;
import com.shanhe.project.manage.stat.service.IStatBatteryPackService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        ArgumentCaptor<BatteryModuleGroupRealtime> groupCaptor = ArgumentCaptor.forClass(BatteryModuleGroupRealtime.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatteryModuleCellRealtime>> cellsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(statBatteryPackService).insertRealtime(Mockito.eq(1), groupCaptor.capture(), cellsCaptor.capture());
        assertEquals(Integer.valueOf(6), groupCaptor.getValue().getBatteryPackStatus());
        assertEquals(Integer.valueOf(3), groupCaptor.getValue().getDeviceWorkStatus());
        assertEquals(Integer.valueOf(1), groupCaptor.getValue().getDeviceWorkIoStatus());
        assertEquals(53.2, groupCaptor.getValue().getPackVoltage());
        assertEquals(2, cellsCaptor.getValue().size());
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

        ArgumentCaptor<BatteryModuleGroupRealtime> groupCaptor = ArgumentCaptor.forClass(BatteryModuleGroupRealtime.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BatteryModuleCellRealtime>> cellsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(predictorService).doTotalBatteryStep(groupCaptor.capture(), cellsCaptor.capture());
        assertEquals(Integer.valueOf(6), groupCaptor.getValue().getBatteryPackStatus());
        assertEquals(2, cellsCaptor.getValue().size());
    }

    @Test
    void capacityPredictionProcessorShouldRejectMissingOrMismatchedGroupBatch() {
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
        assertTrue(processor.shouldProcess(mismatchedCellBatch));
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
