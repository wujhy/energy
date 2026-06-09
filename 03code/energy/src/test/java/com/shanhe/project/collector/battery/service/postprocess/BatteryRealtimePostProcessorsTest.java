package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatteryRealtimePostProcessorsTest {

    @Test
    void statisticsProcessorShouldAdaptRealtimeModel() {
        StatisticsProcessor processor = new StatisticsProcessor();
        IStatBatteryPackService statBatteryPackService = Mockito.mock(IStatBatteryPackService.class);
        ReflectionTestUtils.setField(processor, "statBatteryPackService", statBatteryPackService);

        processor.process(context(group(6, null), cells()));

        ArgumentCaptor<Map<String, Object>> packMapCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(statBatteryPackService).insertList(Mockito.eq(1), packMapCaptor.capture(), Mockito.anyList());
        assertEquals(6, packMapCaptor.getValue().get("batteryPackStatus"));
        assertEquals(3, packMapCaptor.getValue().get("deviceWorkStatus"));
        assertEquals(1, packMapCaptor.getValue().get("deviceWorkIOStatus"));
        assertEquals(53.2, packMapCaptor.getValue().get("packVoltage"));
    }

    @Test
    void operationLogProcessorShouldSkipUnknownStatus() {
        OperationLogProcessor processor = new OperationLogProcessor();
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        ReflectionTestUtils.setField(processor, "optLogService", optLogService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        processor.process(context(group(99, null), cells()));

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

        processor.process(context(group(6, null), cells()));

        Mockito.verify(reportLogService).lastCache(1);
        Mockito.verify(optLogService).insertBattery(Mockito.eq(1), Mockito.anyMap(), Mockito.same(oldInfo));
    }

    @Test
    void resistanceStatisticsProcessorShouldSkipWhenStatusMissing() {
        ResistanceStatisticsProcessor processor = new ResistanceStatisticsProcessor();
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        processor.process(context(group(6, null), cells()));

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
        ReflectionTestUtils.setField(processor, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(processor, "batteryReportLogService", reportLogService);

        List<BatteryModuleCellRealtime> cells = cells();
        cells.get(0).setResistanceRageSlip(9000.0d);

        processor.process(context(group(6, 2), cells));

        ArgumentCaptor<Map<String, Object>> packMapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<List> batteryListCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(statBatteryResService).init(Mockito.eq(1), packMapCaptor.capture(), batteryListCaptor.capture(), Mockito.same(oldInfo));
        assertEquals(2, packMapCaptor.getValue().get("resistanceTestStatus"));
        List<BatteryMonitor> adaptedCells = batteryListCaptor.getValue();
        assertEquals(110, adaptedCells.get(0).getResistance());
        assertNull(adaptedCells.get(0).getResistancerageslip());
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
        return group;
    }

    private List<BatteryModuleCellRealtime> cells() {
        BatteryModuleCellRealtime first = new BatteryModuleCellRealtime();
        first.setPackNum(1);
        first.setBatNum(1);
        first.setVoltage(2.1);
        first.setResistance(110);

        BatteryModuleCellRealtime second = new BatteryModuleCellRealtime();
        second.setPackNum(1);
        second.setBatNum(2);
        second.setVoltage(2.7);
        second.setResistance(120);

        return Arrays.asList(first, second);
    }
}
