package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.capacity.service.BatteryPredictorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

class CapacityPredictionProcessorTest {

    private static final String POLL_BATCH_NO = "batch-1";

    // ===== shouldProcess tests =====

    @Test
    void shouldRejectNullContext() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        Assertions.assertFalse(processor.shouldProcess(null));
    }

    @Test
    void shouldRejectContextWithoutPackNum() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .build();
        Assertions.assertFalse(processor.shouldProcess(context));
    }

    @Test
    void shouldRejectContextWithoutGroup() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .cells(Arrays.asList(cell(1)))
                .build();
        Assertions.assertFalse(processor.shouldProcess(context));
    }

    @Test
    void shouldRejectContextWithEmptyCells() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Collections.emptyList())
                .build();
        Assertions.assertFalse(processor.shouldProcess(context));
    }

    @Test
    void shouldRejectContextWithoutPollBatchNo() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group())
                .cells(Arrays.asList(cell(1)))
                .build();
        Assertions.assertFalse(processor.shouldProcess(context));
    }

    @Test
    void shouldRejectContextWhenGroupBatchDoesNotMatch() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryModuleGroupRealtime group = group();
        group.setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group)
                .cells(Arrays.asList(cell(1)))
                .build();
        Assertions.assertFalse(processor.shouldProcess(context));
    }

    @Test
    void shouldAcceptContextWhenSnapshotCellBatchDoesNotMatch() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryModuleCellRealtime staleCell = cell(2);
        staleCell.setPollBatchNo("other-batch");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1), staleCell))
                .build();
        Assertions.assertTrue(processor.shouldProcess(context));
    }

    @Test
    void shouldAcceptValidContext() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1)))
                .build();
        Assertions.assertTrue(processor.shouldProcess(context));
    }

    // ===== process skip-condition tests =====

    @Test
    void shouldSkipWhenGroupIsNull() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(null)
                .cells(Arrays.asList(cell(1)))
                .build();

        Assertions.assertDoesNotThrow(() -> processor.process(context));
        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void shouldSkipWhenBatteryPackStatusIsNull() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");
        BatteryModuleGroupRealtime group = group();
        // batteryPackStatus is null by default
        BatteryRealtimePostProcessContext context = contextWith(group);

        Assertions.assertDoesNotThrow(() -> processor.process(context));
        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void shouldSkipWhenStatusIsUnknown() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");
        BatteryModuleGroupRealtime group = group();
        group.setBatteryPackStatus(999);
        BatteryRealtimePostProcessContext context = contextWith(group);

        Assertions.assertDoesNotThrow(() -> processor.process(context));
        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void shouldSkipWhenStatusUnchanged() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");

        // First call: set status to IDLE
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));
        processor.process(contextWith(group1));
        Mockito.verifyNoInteractions(predictorService);

        // Second call: same status IDLE
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));
        processor.process(contextWith(group2));
        Mockito.verifyNoInteractions(predictorService);
    }

    // ===== state-transition tests =====

    @Test
    void shouldTriggerPredictionOnBackupToNonBackupTransition() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");

        // First call: set status to BACKUP
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.BACKUP.getCode()));
        processor.process(contextWith(group1));
        Mockito.verifyNoInteractions(predictorService);

        // Second call: status changes to IDLE (from BACKUP → non-BACKUP)
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));
        processor.process(contextWith(group2));
        Mockito.verify(predictorService).doTotalBatteryStep(Mockito.any(), Mockito.any());
    }

    @Test
    void shouldNotTriggerPredictionOnNonBackupToNonBackupTransition() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");

        // First call: IDLE
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));
        processor.process(contextWith(group1));

        // Second call: CHARGE (from IDLE, not BACKUP)
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.CHARGE.getCode()));
        processor.process(contextWith(group2));

        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void shouldNotTriggerPredictionWhenTransitioningToBackup() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");

        // First call: IDLE
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));
        processor.process(contextWith(group1));

        // Second call: BACKUP (from IDLE → BACKUP, not a BACKUP-to-non-BACKUP transition)
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.BACKUP.getCode()));
        processor.process(contextWith(group2));

        Mockito.verifyNoInteractions(predictorService);
    }

    @Test
    void shouldSwallowPredictionException() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");
        Mockito.doThrow(new RuntimeException("prediction failed"))
                .when(predictorService).doTotalBatteryStep(Mockito.any(), Mockito.any());

        // First call: BACKUP
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.BACKUP.getCode()));
        processor.process(contextWith(group1));

        // Second call: transition to IDLE → should swallow exception
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.IDLE.getCode()));

        Assertions.assertDoesNotThrow(() -> processor.process(contextWith(group2)));
    }

    @Test
    void shouldPassCorrectNewStatusToPredictionOnTransition() {
        CapacityPredictionProcessor processor = newProcessor();
        BatteryPredictorService predictorService =
                (BatteryPredictorService) ReflectionTestUtils.getField(processor, "batteryPredictorService");

        // BACKUP
        BatteryModuleGroupRealtime group1 = group();
        group1.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.BACKUP.getCode()));
        processor.process(contextWith(group1));

        // Transition to CHARGE
        BatteryModuleGroupRealtime group2 = group();
        group2.setBatteryPackStatus(Integer.valueOf(BatteryPackStatusEnum.CHARGE.getCode()));
        processor.process(contextWith(group2));

        Mockito.verify(predictorService).doTotalBatteryStep(Mockito.argThat(group -> Integer.valueOf(BatteryPackStatusEnum.CHARGE.getCode()).equals(group.getBatteryPackStatus())), Mockito.any());
    }

    // ===== helpers =====

    private CapacityPredictionProcessor newProcessor() {
        CapacityPredictionProcessor processor = new CapacityPredictionProcessor();
        ReflectionTestUtils.setField(processor, "batteryPredictorService",
                Mockito.mock(BatteryPredictorService.class));
        return processor;
    }

    private BatteryRealtimePostProcessContext contextWith(BatteryModuleGroupRealtime group) {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group)
                .cells(Arrays.asList(cell(1)))
                .build();
    }

    private BatteryModuleGroupRealtime group() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setPollBatchNo(POLL_BATCH_NO);
        return group;
    }

    private BatteryModuleCellRealtime cell(Integer batNum) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(batNum);
        cell.setPollBatchNo(POLL_BATCH_NO);
        return cell;
    }
}
