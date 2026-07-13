package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

class BatteryModuleReportLogAdapterServiceTest {

    private final BatteryModuleReportLogAdapterService service = new BatteryModuleReportLogAdapterService();

    @Test
    void shouldAdaptRealtimeToOldReportLogShape() {
        Date createTime = new Date(1_000L);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setCreateTime(createTime);
        group.setPackVoltage(220.1d);
        group.setBatteryPackOuterVoltage(219.8d);
        group.setPackCurrent(-12.3d);
        group.setBatteryPackFloatCurrent(0.12d);
        group.setEnvironmentTemperature1(25.1d);
        group.setBatteryAvgTemperature(25.6d);
        group.setMaxVoltageBatNum(2);
        group.setMaxCellVoltage(2.30d);
        group.setMinResistanceBatNum(1);
        group.setMinInternalResistance(98);
        group.setBatteryPackSoc(80.5d);
        group.setBatteryPackStatus(5);
        group.setBcapacity(99.9d);

        BatteryReportLog reportLog = service.buildReportLog(1, group,
                Arrays.asList(cell(1, 2.10d, 100, 25.0d), cell(2, 2.30d, 120, 26.0d)));

        Assertions.assertEquals(Constants.DEFAULT_CONFIG_ID, reportLog.getConfigId());
        Assertions.assertEquals(1, reportLog.getPackNum());
        Assertions.assertEquals(createTime, reportLog.getCreateTime());
        Assertions.assertEquals(2, reportLog.getBatteryList().size());
        Assertions.assertNotNull(reportLog.getPackData());
        Assertions.assertNotNull(reportLog.getMonitorData());

        Map<String, Object> packParam = reportLog.getPackParam();
        Assertions.assertEquals("220.1", packParam.get("packVoltage"));
        Assertions.assertEquals("219.8", packParam.get("batteryPackOuterVoltage"));
        Assertions.assertEquals("-12.3", packParam.get("packCurrent"));
        Assertions.assertEquals("0.12", packParam.get("batteryPackFloatCurrent"));
        Assertions.assertEquals("25.1", packParam.get("environmentTemperature1"));
        Assertions.assertEquals("25.6", packParam.get("batteryAvgTemperature"));
        Assertions.assertEquals("2", packParam.get("maxVoltageBatteryNumber"));
        Assertions.assertEquals("2.3", packParam.get("batteryMaxVoltage"));
        Assertions.assertEquals("1", packParam.get("minResistanceBatteryNumber"));
        Assertions.assertEquals("98", packParam.get("batteryMinEsistance"));
        Assertions.assertEquals("80.5", packParam.get("batteryPackSoc"));
        Assertions.assertEquals("5", packParam.get("batteryPackStatus"));
        Assertions.assertEquals("99.9", packParam.get("bcapacity"));

        BatteryMonitor monitor = reportLog.getBatteryList().get(0);
        Assertions.assertEquals(Constants.DEFAULT_CONFIG_ID, monitor.getConfigId());
        Assertions.assertEquals(1, monitor.getPackNum());
        Assertions.assertEquals(1, monitor.getBatNum());
        Assertions.assertEquals(2.10d, monitor.getVoltage(), 0.0001d);
        Assertions.assertEquals(100, monitor.getResistance());
        Assertions.assertEquals(25.0d, monitor.getTemperature(), 0.0001d);
        Assertions.assertEquals(50.0d, monitor.getBcapacity(), 0.0001d);
        Assertions.assertEquals(1.2d, monitor.getResistancerageslip(), 0.0001d);
        Assertions.assertEquals(0.01d, monitor.getResistanceRateChange(), 0.0001d);
        Assertions.assertEquals(3.3d, monitor.getGbvoltage(), 0.0001d);
    }

    @Test
    void shouldBuildEmptyShapeWhenRealtimeDataMissing() {
        BatteryReportLog reportLog = service.buildReportLog(1, null, null);

        Assertions.assertEquals(Constants.DEFAULT_CONFIG_ID, reportLog.getConfigId());
        Assertions.assertEquals(1, reportLog.getPackNum());
        Assertions.assertTrue(reportLog.getPackParam().isEmpty());
        Assertions.assertTrue(reportLog.getBatteryList().isEmpty());
        Assertions.assertEquals("{}", reportLog.getPackData());
        Assertions.assertEquals("[]", reportLog.getMonitorData());
    }

    @Test
    void shouldBuildFromCachedSnapshotWithoutMapperQueries() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackVoltage(220.0d);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group)
                .cells(Arrays.asList(cell(1, 2.10d, 100, 25.0d)))
                .build());
        ReflectionTestUtils.setField(service, "realtimeMapper", mapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        BatteryReportLog reportLog = service.buildReportLog(1);

        Assertions.assertEquals("220.0", reportLog.getPackParam().get("packVoltage"));
        Assertions.assertEquals(1, reportLog.getBatteryList().size());
        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void shouldReturnEmptyShapeWithoutMapperQueryWhenSnapshotCacheMisses() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);
        ReflectionTestUtils.setField(service, "realtimeMapper", mapper);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        BatteryReportLog reportLog = service.buildReportLog(1);

        Assertions.assertTrue(reportLog.getPackParam().isEmpty());
        Assertions.assertTrue(reportLog.getBatteryList().isEmpty());
        Mockito.verify(snapshotService).getCachedSnapshot(1);
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void shouldReturnRealtimeWhenCurrentReportLogIsUsable() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackVoltage(220.0d);
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.10d, 100, 25.0d)));
        ReflectionTestUtils.setField(service, "realtimeMapper", mapper);
        ReflectionTestUtils.setField(service, "snapshotService", null);
        ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);

        BatteryReportLog reportLog = service.currentOrLastCache(1, true);

        Assertions.assertEquals("220.0", reportLog.getPackParam().get("packVoltage"));
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void shouldFallbackToLastCacheWhenCurrentReportLogIsEmpty() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryReportLog historyLog = new BatteryReportLog();
        historyLog.setPackNum(1);
        Mockito.when(reportLogService.lastCache(1)).thenReturn(historyLog);
        ReflectionTestUtils.setField(service, "realtimeMapper", mapper);
        ReflectionTestUtils.setField(service, "snapshotService", null);
        ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);

        BatteryReportLog reportLog = service.currentOrLastCache(1, true);

        Assertions.assertSame(historyLog, reportLog);
        Mockito.verify(reportLogService).lastCache(1);
    }

    @Test
    void shouldNotHardCalculateSocSohOrCapacityWhenCompatibilityFieldsMissing() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackVoltage(220.0d);
        group.setBatteryPackOuterVoltage(219.0d);
        group.setPackCurrent(-10.0d);
        group.setMaxCellVoltage(2.30d);
        group.setMinCellVoltage(2.10d);
        group.setAvgCellVoltage(2.20d);
        group.setBatteryAvgTemperature(25.0d);
        group.setBatteryPackStatus(5);

        Map<String, Object> packParam = service.toPackParam(group);

        Assertions.assertFalse(packParam.containsKey("batteryPackSoc"));
        Assertions.assertFalse(packParam.containsKey("batteryPackSoh"));
        Assertions.assertFalse(packParam.containsKey("bcapacity"));
        Assertions.assertFalse(packParam.containsKey("capacity"));
        Assertions.assertFalse(packParam.containsKey("disChargeCapacity"));
        Assertions.assertFalse(packParam.containsKey("backupDuration"));
        Assertions.assertEquals("220.0", packParam.get("packVoltage"));
        Assertions.assertEquals("-10.0", packParam.get("packCurrent"));
        Assertions.assertEquals("5", packParam.get("batteryPackStatus"));
    }

    private BatteryModuleCellRealtime cell(int batNum, double voltage, int resistance, double temperature) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setCreateTime(new Date(1_000L + batNum));
        cell.setBatNum(batNum);
        cell.setVoltage(voltage);
        cell.setResistance(resistance);
        cell.setTemperature(temperature);
        cell.setCapacity(50.0d);
        cell.setResistanceRageSlip(1.2d);
        cell.setResistanceRateChange(0.01d);
        cell.setSwollenVoltage(3.3d);
        return cell;
    }
}
