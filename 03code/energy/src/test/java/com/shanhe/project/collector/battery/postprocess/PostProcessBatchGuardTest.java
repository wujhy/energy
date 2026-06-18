package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

class PostProcessBatchGuardTest {

    private static final String POLL_BATCH_NO = "batch-1";

    @Test
    void hasTextShouldMatchExistingTrimBehavior() {
        Assertions.assertFalse(PostProcessBatchGuard.hasText(null));
        Assertions.assertFalse(PostProcessBatchGuard.hasText(" "));
        Assertions.assertTrue(PostProcessBatchGuard.hasText(POLL_BATCH_NO));
    }

    @Test
    void sameRealtimeBatchShouldRequireContextGroupAndCellsInSameBatch() {
        Assertions.assertTrue(PostProcessBatchGuard.sameRealtimeBatch(context(group(POLL_BATCH_NO),
                Arrays.asList(cell(POLL_BATCH_NO), cell(POLL_BATCH_NO)))));

        Assertions.assertFalse(PostProcessBatchGuard.sameRealtimeBatch(context(group("other-batch"),
                Arrays.asList(cell(POLL_BATCH_NO), cell(POLL_BATCH_NO)))));
        Assertions.assertFalse(PostProcessBatchGuard.sameRealtimeBatch(context(group(POLL_BATCH_NO),
                Arrays.asList(cell(POLL_BATCH_NO), cell("other-batch")))));
        Assertions.assertFalse(PostProcessBatchGuard.sameRealtimeBatch(context(group(POLL_BATCH_NO),
                Arrays.asList(cell(POLL_BATCH_NO), null))));
        Assertions.assertFalse(PostProcessBatchGuard.sameRealtimeBatch(context(group(POLL_BATCH_NO),
                Collections.emptyList())));
    }

    @Test
    void sameCellBatchShouldKeepVoltageRangeCellOnlyBehavior() {
        Assertions.assertTrue(PostProcessBatchGuard.sameCellBatch(POLL_BATCH_NO,
                Arrays.asList(cell(POLL_BATCH_NO), cell(POLL_BATCH_NO))));
        Assertions.assertFalse(PostProcessBatchGuard.sameCellBatch(" ",
                Arrays.asList(cell(POLL_BATCH_NO), cell(POLL_BATCH_NO))));
        Assertions.assertFalse(PostProcessBatchGuard.sameCellBatch(POLL_BATCH_NO,
                Arrays.asList(cell(POLL_BATCH_NO), cell("other-batch"))));
    }

    private BatteryRealtimePostProcessContext context(BatteryModuleGroupRealtime group,
                                                      java.util.List<BatteryModuleCellRealtime> cells) {
        return BatteryRealtimePostProcessContext.builder()
                .pollBatchNo(POLL_BATCH_NO)
                .group(group)
                .cells(cells)
                .build();
    }

    private BatteryModuleGroupRealtime group(String pollBatchNo) {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPollBatchNo(pollBatchNo);
        return group;
    }

    private BatteryModuleCellRealtime cell(String pollBatchNo) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPollBatchNo(pollBatchNo);
        return cell;
    }
}
