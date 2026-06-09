package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

class AlarmContextProcessorTest {

    @Test
    void processShouldBuildContextAndSkipAlarmServiceWhenAlarmContextIsEmpty() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext emptyAlarmContext = alarmContext(1);
        BatteryRealtimePostProcessContext context = contextWithoutAlarmContext();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(emptyAlarmContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        Assertions.assertSame(emptyAlarmContext, context.getAlarmContext());
        Mockito.verify(alarmAdaptService).buildContext(context.getGroup(), context.getCells());
        Mockito.verifyNoInteractions(alarmLogService);
    }

    @Test
    void processShouldUseExistingAlarmContextAndCallPackAlarmWhenPackWarnIsNotEmpty() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext alarmContext = alarmContext(1);
        alarmContext.putPackWarn("ZDYGC", "1");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        Mockito.verifyNoInteractions(alarmAdaptService);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.same(alarmContext.getPackWarnParam()));
    }

    @Test
    void processShouldCallCellAlarmOnlyForNonEmptyCellWarn() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext alarmContext = alarmContext(1);
        alarmContext.putCellWarn(2, "DTDYGC", "1");
        alarmContext.getCellWarnParam().put(3, Collections.emptyMap());
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.eq(2), warnParamCaptor.capture());
        Assertions.assertEquals("1", warnParamCaptor.getValue().get("DTDYGC"));
        Mockito.verifyNoMoreInteractions(alarmLogService);
    }

    @Test
    void processShouldMergeCommunicationAlarmContext() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext realtimeContext = alarmContext(1);
        realtimeContext.putPackWarn(ItemCode.ZDYGC.getCode(), "1");
        BatteryModuleAlarmContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = contextWithChannelConfig();
        Mockito.when(alarmAdaptService.buildContext(context.getGroup(), context.getCells()))
                .thenReturn(realtimeContext);
        Mockito.when(alarmAdaptService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        ArgumentCaptor<Map<String, String>> warnParamCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), warnParamCaptor.capture());
        Assertions.assertEquals("1", warnParamCaptor.getValue().get(ItemCode.ZDYGC.getCode()));
        Assertions.assertEquals("1", warnParamCaptor.getValue().get(ItemCode.TXZT.getCode()));
        Assertions.assertSame(realtimeContext, context.getAlarmContext());
    }

    @Test
    void processShouldBuildCommunicationOnlyAlarmContextWhenCellsAreMissing() {
        AlarmContextProcessor processor = newProcessor();
        BatteryModuleAlarmAdaptService alarmAdaptService = Mockito.mock(BatteryModuleAlarmAdaptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext communicationContext = alarmContext(1);
        communicationContext.putPackWarn(ItemCode.TXZT.getCode(), "1");
        BatteryRealtimePostProcessContext context = BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .channelConfig(channelConfig())
                .build();
        Mockito.when(alarmAdaptService.buildCommunicationAlarmContext(1, "COM1"))
                .thenReturn(communicationContext);
        ReflectionTestUtils.setField(processor, "alarmAdaptService", alarmAdaptService);
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        Mockito.verify(alarmLogService).alarmBatteryValue(Mockito.isNull(), Mockito.eq(1),
                Mockito.isNull(), Mockito.argThat(params -> "1".equals(params.get(ItemCode.TXZT.getCode()))));
        Assertions.assertNotNull(context.getAlarmContext());
    }

    @Test
    void processShouldNotCallAlarmServiceWhenAlarmContextMissingAndAdaptServiceMissing() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryRealtimePostProcessContext context = contextWithoutAlarmContext();
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        processor.process(context);

        Assertions.assertNull(context.getAlarmContext());
        Mockito.verifyNoInteractions(alarmLogService);
    }

    @Test
    void processShouldSwallowAlarmServiceException() {
        AlarmContextProcessor processor = newProcessor();
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryModuleAlarmContext alarmContext = alarmContext(1);
        alarmContext.putPackWarn("ZDYGC", "1");
        BatteryRealtimePostProcessContext context = contextWithAlarmContext(alarmContext);
        Mockito.doThrow(new RuntimeException("alarm failed"))
                .when(alarmLogService)
                .alarmBatteryValue(Mockito.isNull(), Mockito.eq(1), Mockito.isNull(), Mockito.anyMap());
        ReflectionTestUtils.setField(processor, "alarmLogService", alarmLogService);

        Assertions.assertDoesNotThrow(() -> processor.process(context));
        Assertions.assertSame(alarmContext, context.getAlarmContext());
    }

    @Test
    void shouldProcessShouldAcceptExistingAlarmContextWithoutCellsWhenAlarmServiceExists() {
        AlarmContextProcessor processor = newProcessor();
        ReflectionTestUtils.setField(processor, "alarmLogService", Mockito.mock(IAlarmLogService.class));

        Assertions.assertTrue(processor.shouldProcess(BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .alarmContext(alarmContext(1))
                .build()));
    }

    @Test
    void processShouldIgnoreNullContext() {
        AlarmContextProcessor processor = newProcessor();

        Assertions.assertDoesNotThrow(() -> processor.process(null));
    }

    private AlarmContextProcessor newProcessor() {
        return new AlarmContextProcessor();
    }

    private BatteryRealtimePostProcessContext contextWithoutAlarmContext() {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group())
                .cells(Arrays.asList(cell(1), cell(2)))
                .build();
    }

    private BatteryRealtimePostProcessContext contextWithChannelConfig() {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .group(group())
                .cells(Arrays.asList(cell(1), cell(2)))
                .channelConfig(channelConfig())
                .build();
    }

    private BatteryRealtimePostProcessContext contextWithAlarmContext(BatteryModuleAlarmContext alarmContext) {
        return BatteryRealtimePostProcessContext.builder()
                .packNum(1)
                .alarmContext(alarmContext)
                .build();
    }

    private BatteryModuleAlarmContext alarmContext(Integer packNum) {
        BatteryModuleAlarmContext alarmContext = new BatteryModuleAlarmContext();
        alarmContext.setPackNum(packNum);
        return alarmContext;
    }

    private BatteryModuleGroupRealtime group() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        return group;
    }

    private BatteryModuleCellRealtime cell(Integer batNum) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(batNum);
        return cell;
    }

    private BatteryCollectorChannelConfig channelConfig() {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("COM1");
        config.setBatteryGroup(1);
        return config;
    }
}
