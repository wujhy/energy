package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.stat.service.IStatBatteryResService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

class ResistanceStatisticsAfterCompletionProcessorTest {

    @Test
    void shouldSkipCompletionBatchAndWriteNextCompleteBatch() {
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        ResistanceStatisticsAfterCompletionProcessor processor = processor(statBatteryResService);
        processor.deferAfterNextRealtimeBatch(1, 100L, "batch-1");
        List<BatteryModuleCellRealtime> completionBatchCells = cells("batch-1");

        processor.process(context("batch-1", completionBatchCells, 2, Collections.emptySet()));

        Mockito.verifyNoInteractions(statBatteryResService);
        List<BatteryModuleCellRealtime> nextBatchCells = cells("batch-2");
        processor.process(context("batch-2", nextBatchCells, 2, Collections.emptySet()));
        processor.process(context("batch-2", nextBatchCells, 2, Collections.emptySet()));

        Mockito.verify(statBatteryResService, Mockito.times(1)).initRealtime(1, nextBatchCells);
    }

    @Test
    void shouldWaitUntilNextBatchHasCompleteCurrentCells() {
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        ResistanceStatisticsAfterCompletionProcessor processor = processor(statBatteryResService);
        processor.deferAfterNextRealtimeBatch(1, 100L, "batch-1");

        processor.process(context("batch-2",
                Collections.singletonList(cell("batch-2", 1, 110)), 2, Collections.singleton(2)));

        Mockito.verifyNoInteractions(statBatteryResService);
        List<BatteryModuleCellRealtime> completeCells = cells("batch-3");
        processor.process(context("batch-3", completeCells, 2, Collections.emptySet()));

        Mockito.verify(statBatteryResService).initRealtime(1, completeCells);
    }

    @Test
    void shouldNotProcessMixedOrMissingResistanceCells() {
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        ResistanceStatisticsAfterCompletionProcessor processor = processor(statBatteryResService);
        processor.deferAfterNextRealtimeBatch(1, 100L, "batch-1");
        List<BatteryModuleCellRealtime> cells = Arrays.asList(
                cell("batch-2", 1, 110),
                cell("batch-2", 2, null));

        processor.process(context("batch-2", cells, 2, Collections.emptySet()));

        Mockito.verifyNoInteractions(statBatteryResService);
    }

    private ResistanceStatisticsAfterCompletionProcessor processor(IStatBatteryResService statBatteryResService) {
        ResistanceStatisticsAfterCompletionProcessor processor = new ResistanceStatisticsAfterCompletionProcessor();
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        return processor;
    }

    private BatteryRealtimePostProcessContext context(String batchNo,
                                                      List<BatteryModuleCellRealtime> cells,
                                                      Integer expectedCellCount,
                                                      java.util.Set<Integer> missingCellNums) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPollBatchNo(batchNo);
        BatteryModuleRealtimeSnapshot snapshot = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(expectedCellCount)
                .pollBatchNo(batchNo)
                .group(group)
                .cells(cells)
                .missingCellNums(new LinkedHashSet<>(missingCellNums))
                .build();
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(batchNo)
                .group(group)
                .cells(cells)
                .realtimeSnapshot(snapshot)
                .build();
    }

    private List<BatteryModuleCellRealtime> cells(String batchNo) {
        return Arrays.asList(cell(batchNo, 1, 110), cell(batchNo, 2, 120));
    }

    private BatteryModuleCellRealtime cell(String batchNo, Integer batNum, Integer resistance) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(batNum);
        cell.setResistance(resistance);
        cell.setPollBatchNo(batchNo);
        return cell;
    }
}
