package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class BatteryModuleRealtimeSnapshotServiceTest {

    private final BatteryModuleRealtimeSnapshotService service = new BatteryModuleRealtimeSnapshotService();

    @Test
    void shouldFillMissingCellsFromPreviousSnapshotByModelNum() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-2")
                .cells(Arrays.asList(cell(1), cell(2), cell(4), cell(6)))
                .groups(Collections.emptyList())
                .build();
        BatteryModuleRealtimeSnapshot previous = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(6)
                .cells(Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6)))
                .cellMap(map(Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6))))
                .cellMissCounts(Collections.emptyMap())
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6)),
                new BatteryModuleGroupRealtime(),
                previous);

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), nums(snapshot.getCells()));
        Assertions.assertEquals(1, snapshot.getCellMissCounts().get(3));
        Assertions.assertEquals(1, snapshot.getCellMissCounts().get(5));
        Assertions.assertTrue(snapshot.getStaleCellNums().isEmpty());
    }

    @Test
    void shouldNotFillCellAfterTwoMissedBatches() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-3")
                .cells(Arrays.asList(cell(1), cell(2), cell(4), cell(6)))
                .groups(Collections.emptyList())
                .build();
        BatteryModuleRealtimeSnapshot previous = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(6)
                .cells(Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6)))
                .cellMap(map(Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6))))
                .cellMissCounts(mapMiss(3, 1, 5, 1))
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(1), cell(2), cell(3), cell(4), cell(5), cell(6)),
                new BatteryModuleGroupRealtime(),
                previous);

        Assertions.assertEquals(Arrays.asList(1, 2, 4, 6), nums(snapshot.getCells()));
        Assertions.assertTrue(snapshot.getStaleCellNums().containsAll(Arrays.asList(3, 5)));
    }

    @Test
    void shouldUseCurrentCellsOnlyWhenCurrentBatchReachesConfiguredSize() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-2")
                .cells(Arrays.asList(cell(8), cell(2), cell(6), cell(4)))
                .groups(Collections.emptyList())
                .build();
        BatteryModuleRealtimeSnapshot previous = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(3)
                .cells(Arrays.asList(cell(1), cell(3), cell(5)))
                .cellMap(map(Arrays.asList(cell(1), cell(3), cell(5))))
                .cellMissCounts(Collections.emptyMap())
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(2), cell(4), cell(6), cell(8)),
                null,
                previous);

        Assertions.assertEquals(Arrays.asList(2, 4, 6), nums(snapshot.getCells()));
    }

    @Test
    void shouldNotTreatBatSinSizeAsModelNumRangeWhenResolvingMissingCells() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-2")
                .cells(Arrays.asList(cell(2), cell(4), cell(6)))
                .groups(Collections.emptyList())
                .build();
        BatteryModuleRealtimeSnapshot previous = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(3)
                .cells(Arrays.asList(cell(2), cell(4), cell(6)))
                .cellMap(map(Arrays.asList(cell(2), cell(4), cell(6))))
                .cellMissCounts(Collections.emptyMap())
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(2), cell(4), cell(6)),
                null,
                previous);

        Assertions.assertEquals(Arrays.asList(2, 4, 6), nums(snapshot.getCells()));
        Assertions.assertTrue(snapshot.getMissingCellNums().isEmpty());
    }

    @Test
    void shouldReturnNullFromRefreshAfterPollWhenPackNumIsNull() {
        BatteryModulePollContext context = BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();

        BatteryModuleRealtimeSnapshot result = service.refreshAfterPoll(null, context, null);
        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnNullFromRefreshAfterPollWhenContextIsNull() {
        BatteryModuleRealtimeSnapshot result = service.refreshAfterPoll(1, null, null);
        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnNullFromGetSnapshotWhenPackNumIsNull() {
        BatteryModuleRealtimeSnapshot result = service.getSnapshot(null);
        Assertions.assertNull(result);
    }

    @Test
    void shouldReturnNullFromGetCachedSnapshotWhenPackNumIsNull() {
        BatteryModuleRealtimeSnapshot result = service.getCachedSnapshot(null);
        Assertions.assertNull(result);
    }

    @Test
    void shouldNotThrowWhenEvictWithNullPackNum() {
        Assertions.assertDoesNotThrow(() -> service.evict(null));
    }

    @Test
    void shouldBuildSnapshotWithNullPreviousSnapshot() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .cells(Arrays.asList(cell(1), cell(2), cell(3)))
                .groups(Collections.emptyList())
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(1), cell(2), cell(3)),
                new BatteryModuleGroupRealtime(),
                null);

        Assertions.assertNotNull(snapshot);
        Assertions.assertEquals(1, snapshot.getPackNum());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), nums(snapshot.getCells()));
        Assertions.assertEquals(0, snapshot.getCellMissCounts().get(1));
        Assertions.assertEquals(0, snapshot.getCellMissCounts().get(2));
        Assertions.assertEquals(0, snapshot.getCellMissCounts().get(3));
        Assertions.assertTrue(snapshot.getStaleCellNums().isEmpty());
    }

    @Test
    void shouldBuildSnapshotWithEmptyCurrentCells() {
        BatteryModulePollContext current = BatteryModulePollContext.builder()
                .pollBatchNo("batch-2")
                .cells(Collections.emptyList())
                .groups(Collections.emptyList())
                .build();
        BatteryModuleRealtimeSnapshot previous = BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .batSinSize(3)
                .cells(Arrays.asList(cell(1), cell(2), cell(3)))
                .cellMap(map(Arrays.asList(cell(1), cell(2), cell(3))))
                .cellMissCounts(Collections.emptyMap())
                .build();

        BatteryModuleRealtimeSnapshot snapshot = service.buildSnapshot(
                1,
                current,
                Arrays.asList(cell(1), cell(2), cell(3)),
                null,
                previous);

        Assertions.assertEquals(Arrays.asList(1, 2, 3), nums(snapshot.getCells()));
        Assertions.assertEquals(1, snapshot.getCellMissCounts().get(1));
        Assertions.assertEquals(1, snapshot.getCellMissCounts().get(2));
        Assertions.assertEquals(1, snapshot.getCellMissCounts().get(3));
    }

    private BatteryModuleCellRealtime cell(int batNum) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(batNum);
        cell.setVoltage(2.0d + batNum / 1000d);
        return cell;
    }

    private java.util.Map<Integer, BatteryModuleCellRealtime> map(List<BatteryModuleCellRealtime> cells) {
        return cells.stream().collect(Collectors.toMap(
                BatteryModuleCellRealtime::getBatNum,
                cell -> cell,
                (left, right) -> right,
                java.util.LinkedHashMap::new));
    }

    private java.util.Map<Integer, Integer> mapMiss(int firstKey, int firstValue, int secondKey, int secondValue) {
        java.util.Map<Integer, Integer> result = new java.util.LinkedHashMap<>();
        result.put(firstKey, firstValue);
        result.put(secondKey, secondValue);
        return result;
    }

    private List<Integer> nums(List<BatteryModuleCellRealtime> cells) {
        return cells.stream().map(BatteryModuleCellRealtime::getBatNum).collect(Collectors.toList());
    }
}
