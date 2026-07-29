package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class BatteryAlarmEvaluationServiceTest {

    @Test
    void evaluateShouldSubmitPackAndCellWarnings() {
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationService service = service(alarmLogService);
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.setPackNum(2);
        context.putPackWarn(ItemCode.TXZT.getCode(), "1");
        context.putCellWarn(3, ItemCode.DTDYGC.getCode(), "2.35");
        context.setCurrentBatchCellNums(Collections.singletonList(3));

        service.evaluate(1, context);

        Mockito.verify(alarmLogService).alarmBatteryValue(null, 2, null, context.getPackWarnParam());
        Mockito.verify(alarmLogService).alarmBatteryValue(null, 2, 3, context.getCellWarnParam().get(3));
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(2), Mockito.eq(true),
                Mockito.eq(Collections.singletonList(3)), Mockito.argThat(codes ->
                        codes.contains(ItemCode.DTDYGC.getCode())
                                && codes.contains(ItemCode.DTTXZT.getCode())));
    }

    @Test
    void evaluateShouldUseFallbackPackNumWhenContextPackNumMissing() {
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationService service = service(alarmLogService);
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.putPackWarn(ItemCode.TXZT.getCode(), "1");

        service.evaluate(5, context);

        Mockito.verify(alarmLogService).alarmBatteryValue(null, 5, null, context.getPackWarnParam());
        Mockito.verify(alarmLogService, Mockito.never()).alarmFix(Mockito.anyInt(), Mockito.eq(true),
                Mockito.anyList(), Mockito.anyList());
    }

    @Test
    void evaluateShouldNotRecoverCellAlarmsWithoutCurrentBatchCells() {
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationService service = service(alarmLogService);
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.setPackNum(1);
        context.putPackWarn(ItemCode.TXZT.getCode(), "1");
        context.setCurrentBatchCellNums(Collections.emptyList());

        service.evaluate(null, context);

        Mockito.verify(alarmLogService).alarmBatteryValue(null, 1, null, context.getPackWarnParam());
        Mockito.verify(alarmLogService, Mockito.never()).alarmFix(Mockito.eq(1), Mockito.eq(true),
                Mockito.anyList(), Mockito.anyList());
    }

    @Test
    void evaluateShouldIgnoreEmptyCellWarningMaps() {
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationService service = service(alarmLogService);
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.setPackNum(1);
        context.setCurrentBatchCellNums(Collections.singletonList(1));
        Map<Integer, Map<String, String>> cellWarnParam = new HashMap<>();
        cellWarnParam.put(1, Collections.emptyMap());
        cellWarnParam.put(2, null);
        context.setCellWarnParam(cellWarnParam);

        service.evaluate(null, context);

        Mockito.verify(alarmLogService, Mockito.never()).alarmBatteryValue(
                Mockito.isNull(), Mockito.eq(1), Mockito.anyInt(), Mockito.anyMap());
        Mockito.verify(alarmLogService).alarmFix(Mockito.eq(1), Mockito.eq(true),
                Mockito.eq(Collections.singletonList(1)), Mockito.anyList());
    }

    @Test
    void evaluateShouldReturnWhenServiceContextOrPackNumMissing() {
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryAlarmEvaluationService service = service(alarmLogService);

        service.evaluate(1, null);
        service.evaluate(null, new BatteryAlarmEvaluationContext());

        Mockito.verifyNoInteractions(alarmLogService);
    }

    @Test
    void evaluateShouldReturnWhenAlarmLogServiceMissing() {
        BatteryAlarmEvaluationService service = new BatteryAlarmEvaluationService();
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        context.setPackNum(1);
        context.putPackWarn(ItemCode.TXZT.getCode(), "1");

        service.evaluate(null, context);
    }


    private BatteryAlarmEvaluationService service(IAlarmLogService alarmLogService) {
        BatteryAlarmEvaluationService service = new BatteryAlarmEvaluationService();
        ReflectionTestUtils.setField(service, "alarmLogService", alarmLogService);
        return service;
    }
}
