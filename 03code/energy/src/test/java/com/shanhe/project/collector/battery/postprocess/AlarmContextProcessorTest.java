package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryAlarmEvaluationService;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

class AlarmContextProcessorTest {

    private static final String POLL_BATCH_NO = "batch-1";

    @Test
    void processShouldBuildContextAndCallAlarmFixWhenAlarmContextIsEmpty() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext emptyAlarmContext = alarmContext(1);
        BatteryRealtimePostProcessContext context = contextWithoutAlarmContext();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(emptyAlarmContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Assertions.assertSame(emptyAlarmContext, context.getAlarmContext());
        Mockito.verify(alarmAdaptService).buildContext(context.getGroup(), context.getCells());
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.eq(Arrays.asList(1, 2)), Mockito.anyList());
    }

    @Test
    void processShouldUseExistingAlarmContextAndCallPackAlarmWhenPackWarnIsNotEmpty() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putPackWarn("ZDYGC", "1");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verifyNoInteractions(alarmAdaptService);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.same(alarmContext.getPackWarnParam()));
        Mockito.verify(alarmLogService, Mockito.never()).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.anyList(), Mockito.anyList());
    }

    @Test
    void processShouldCallCellAlarmOnlyForNonEmptyCellWarn() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putCellWarn(2, "DTDYGC", "1");
        alarmContext.getCellWarnParam().put(3, Collections.emptyMap());
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.eq(2), warnParamCaptor.capture());
        Assertions.assertEquals("1", warnParamCaptor.getValue().get("DTDYGC"));
        Mockito.verify(alarmLogService, Mockito.never()).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.anyList(), Mockito.anyList());
        Mockito.verifyNoMoreInteractions(alarmLogService);
    }

    @Test
    void processShouldMergeCommunicationAlarmContext() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext realtimeContext = alarmContext(1);
        realtimeContext.putPackWarn(ItemCode.ZDYGC.getCode(), "1");
        BatteryAlarmEvaluationContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = contextWithChannelConfig();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(realtimeContext);
        Mockito.when(alarmStateContextService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), warnParamCaptor.capture());
        Assertions.assertEquals("1", warnParamCaptor.getValue().get(ItemCode.ZDYGC.getCode()));
        Assertions.assertEquals("1", warnParamCaptor.getValue().get(ItemCode.TXZT.getCode()));
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.eq(Arrays.asList(1, 2)), Mockito.anyList());
        Assertions.assertSame(realtimeContext, context.getAlarmContext());
    }

    @Test
    void processShouldKeepRealtimeCommunicationAlarmWhenRecoveredStateIsMerged() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext realtimeContext = alarmContext(1);
        realtimeContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryAlarmEvaluationContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "0");
        BatteryRealtimePostProcessContext context = contextWithChannelConfig();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(realtimeContext);
        Mockito.when(alarmStateContextService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.argThat(params -> "1".equals(params.get(ItemCode.TXZT.getCode()))));
    }
    @Test
    void processShouldSendMergedPackAndCellAlarmContext() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext realtimeContext = alarmContext(1);
        realtimeContext.putPackWarn(ItemCode.ZDYGC.getCode(), "230.5");
        realtimeContext.putCellWarn(2, ItemCode.DTDYGC.getCode(), "2.1");
        BatteryAlarmEvaluationContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = contextWithChannelConfig();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(realtimeContext);
        Mockito.when(alarmStateContextService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.argThat(params ->
                        "230.5".equals(params.get(ItemCode.ZDYGC.getCode()))
                                && "1".equals(params.get(ItemCode.TXZT.getCode()))));
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.eq(2), Mockito.argThat(params ->
                        "2.1".equals(params.get(ItemCode.DTDYGC.getCode()))));
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.eq(Arrays.asList(1, 2)), Mockito.anyList());
        Mockito.verifyNoMoreInteractions(alarmLogService);
        Assertions.assertSame(realtimeContext, context.getAlarmContext());
    }

    @Test
    void processShouldBuildCommunicationOnlyAlarmContextWhenCellsAreMissing() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .channelConfig(channelConfig())
                .build();
        Mockito.when(alarmStateContextService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.argThat(params -> "1".equals(params.get(ItemCode.TXZT.getCode()))));
        Mockito.verify(alarmLogService, Mockito.never()).alarmFix(Mockito.eq(1), Mockito.eq(true), Mockito.anyList(), Mockito.anyList());
        Assertions.assertNotNull(context.getAlarmContext());
    }

    @Test
    void processShouldNotCallAlarmServiceWhenAlarmContextMissingAndAdaptServiceMissing() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryRealtimePostProcessContext context = contextWithoutAlarmContext();
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Assertions.assertNull(context.getAlarmContext());
        Mockito.verifyNoInteractions(alarmLogService);
    }

    @Test
    void processShouldSwallowAlarmServiceException() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putPackWarn("ZDYGC", "1");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        Mockito.doThrow(new RuntimeException("alarm failed"))
                .when(alarmLogService)
                .alarmBatteryValue(Mockito.isNull(), Mockito.eq(1), Mockito.isNull(), Mockito.anyMap());
        setEvaluationService(processor, alarmLogService);

        Assertions.assertDoesNotThrow(() -> processor.process(context));
        Assertions.assertSame(alarmContext, context.getAlarmContext());
    }

    @Test
    void shouldProcessShouldAcceptExistingAlarmContextWithoutCellsWhenAlarmServiceExists() {
        AlarmContextProcessor processor = newProcessor();
        setEvaluationService(processor, Mockito.mock(IAlarmLogService.class));

        Assertions.assertTrue(processor.shouldProcess(BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .alarmContext(alarmContext(1))
                .build()));
    }

    @Test
    void shouldProcessShouldRejectContextWithoutPollBatchNo() {
        AlarmContextProcessor processor = newProcessor();
        ReflectionTestUtils.setField(processor, "alarmAdaptService", Mockito.mock(BatteryModuleAlarmAdaptService.class));

        Assertions.assertFalse(processor.shouldProcess(BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group())
                .cells(Arrays.asList(cell(1), cell(2)))
                .build()));
    }

    @Test
    void shouldProcessShouldRejectRealtimeContextWhenGroupBatchDoesNotMatch() {
        AlarmContextProcessor processor = newProcessor();
        ReflectionTestUtils.setField(processor, "alarmAdaptService", Mockito.mock(BatteryModuleAlarmAdaptService.class));
        BatteryModuleGroupRealtime group = group();
        group.setPollBatchNo("other-batch");

        Assertions.assertFalse(processor.shouldProcess(BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group)
                .cells(Arrays.asList(cell(1), cell(2)))
                .build()));
    }

    @Test
    void shouldProcessShouldRejectRealtimeContextWhenCellBatchDoesNotMatch() {
        AlarmContextProcessor processor = newProcessor();
        ReflectionTestUtils.setField(processor, "alarmAdaptService", Mockito.mock(BatteryModuleAlarmAdaptService.class));
        BatteryModuleCellRealtime staleCell = cell(2);
        staleCell.setPollBatchNo("other-batch");

        Assertions.assertFalse(processor.shouldProcess(BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1), staleCell))
                .build()));
    }

    @Test
    void processShouldNotBuildRealtimeAlarmContextFromDifferentBatchWhenCommunicationAlarmExists() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        BatteryAlarmStateContextService alarmStateContextService = Mockito.mock(BatteryAlarmStateContextService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleCellRealtime staleCell = cell(2);
        staleCell.setPollBatchNo("other-batch");
        BatteryAlarmEvaluationContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1), staleCell))
                .channelConfig(channelConfig())
                .build();
        Mockito.when(alarmStateContextService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmStateContextService", alarmStateContextService);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verify(alarmAdaptService, Mockito.never()).buildContext(Mockito.any(), Mockito.anyList());
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.argThat(params -> "1".equals(params.get(ItemCode.TXZT.getCode()))));
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true),
                Mockito.eq(Collections.singletonList(1)), Mockito.anyList());
        Assertions.assertEquals(Collections.singletonList(1), context.getAlarmContext().getCurrentBatchCellNums());
        Assertions.assertEquals(Collections.singletonList(2), context.getAlarmContext().getStaleCellNums());
        Assertions.assertEquals(Boolean.FALSE, context.getAlarmContext().getSnapshotFresh());
    }

    @Test
    void processShouldIgnoreNullContext() {
        AlarmContextProcessor processor = newProcessor();

        Assertions.assertDoesNotThrow(() -> processor.process(null));
    }

    @Test
    void processShouldDispatchCellAlarmsIsolatedByBatNum() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putCellWarn(1, ItemCode.DTDYGC.getCode(), "2.1");
        alarmContext.putCellWarn(2, ItemCode.DTDYGC.getCode(), "2.2");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.eq(1), warnParamCaptor.capture());
        Assertions.assertEquals("2.1", warnParamCaptor.getValue().get(ItemCode.DTDYGC.getCode()));

        ArgumentCaptor<Map<String, String>> warnParamCaptor2 = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.eq(2), warnParamCaptor2.capture());
        Assertions.assertEquals("2.2", warnParamCaptor2.getValue().get(ItemCode.DTDYGC.getCode()));
    }

    @Test
    void processShouldCallAlarmFixWithActiveCellNumsForRecovery() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putCellWarn(1, ItemCode.DTDYGC.getCode(), "2.1");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .alarmContext(alarmContext)
                .cells(Arrays.asList(cell(1), cell(2)))
                .build();
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<String>> codesCaptor = ArgumentCaptor.forClass(java.util.List.class);
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true),
                Mockito.eq(Arrays.asList(1, 2)), codesCaptor.capture());
        Assertions.assertFalse(codesCaptor.getValue().isEmpty());
        Assertions.assertTrue(codesCaptor.getValue().contains(ItemCode.DTDYGC.getCode()));
    }

    @Test
    void processShouldNotCallAlarmFixWithoutCurrentBatchCells() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationContext alarmContext = alarmContext(1);
        alarmContext.putCellWarn(1, ItemCode.DTDYGC.getCode(), "2.1");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        setEvaluationService(processor, alarmLogService);

        processor.process(context);

        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true),
                Mockito.eq(Collections.emptyList()), Mockito.anyList());
    }

    private AlarmContextProcessor newProcessor() {
        return new AlarmContextProcessor();
    }

    private void setEvaluationService(AlarmContextProcessor processor, IAlarmLogService alarmLogService) {
        BatteryAlarmEvaluationService evaluationService = new BatteryAlarmEvaluationService();
        ReflectionTestUtils.setField(evaluationService, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(processor, "alarmEvaluationService", evaluationService);
    }

    private BatteryRealtimePostProcessContext contextWithoutAlarmContext() {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1), cell(2)))
                .build();
    }

    private BatteryRealtimePostProcessContext contextWithChannelConfig() {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .group(group())
                .cells(Arrays.asList(cell(1), cell(2)))
                .channelConfig(channelConfig())
                .build();
    }

    private BatteryRealtimePostProcessContext contextWithAlarmContext(BatteryAlarmEvaluationContext alarmContext) {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .pollBatchNo(POLL_BATCH_NO)
                .alarmContext(alarmContext)
                .build();
    }

    private BatteryAlarmEvaluationContext alarmContext(Integer packNum) {
        BatteryAlarmEvaluationContext alarmContext = new BatteryAlarmEvaluationContext();
        alarmContext.setPackNum(packNum);
        return alarmContext;
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

    private BatteryCollectorChannelConfig channelConfig() {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("COM1");
        config.setBatteryGroup(1);
        return config;
    }
}
