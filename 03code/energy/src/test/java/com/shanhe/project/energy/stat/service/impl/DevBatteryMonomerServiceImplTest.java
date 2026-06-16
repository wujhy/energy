package com.shanhe.project.energy.stat.service.impl;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.energy.stat.domain.DevBatteryMonomer;
import com.shanhe.project.energy.stat.mapper.DevBatteryMonomerMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

class DevBatteryMonomerServiceImplTest {

    @Test
    void shouldUseRealtimeReportLogWhenEnabled() {
        DevBatteryMonomerServiceImpl service = newService(true);
        BatteryReportLogService oldService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLog realtimeLog = reportLog(monitor(1, 120));
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(realtimeLog);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertSame(realtimeLog, result);
        Mockito.verifyNoInteractions(oldService);
    }

    @Test
    void shouldFallbackToOldCacheWhenRealtimeReportLogUnavailable() {
        DevBatteryMonomerServiceImpl service = newService(true);
        BatteryReportLogService oldService = (BatteryReportLogService) ReflectionTestUtils.getField(service, "batteryReportLogService");
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        BatteryReportLog oldLog = reportLog(monitor(1, 100));
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(new BatteryReportLog());
        Mockito.when(oldService.lastCache(1)).thenReturn(oldLog);

        BatteryReportLog result = service.resolveBatteryReportLog(1);

        Assertions.assertSame(oldLog, result);
    }

    @Test
    void shouldCalculateMaxResistanceFromRealtimeReportLog() {
        DevBatteryMonomerServiceImpl service = newService(true);
        BatteryModuleReportLogAdapterService adapterService =
                (BatteryModuleReportLogAdapterService) ReflectionTestUtils.getField(service, "batteryModuleReportLogAdapterService");
        IBatteryPackService batteryPackService = (IBatteryPackService) ReflectionTestUtils.getField(service, "batteryPackService");
        DevBatteryMonomerMapper monomerMapper = (DevBatteryMonomerMapper) ReflectionTestUtils.getField(service, "devBatteryMonomerMapper");
        BatteryPack pack = new BatteryPack();
        pack.setPackId(10L);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(1)).thenReturn(pack);
        Mockito.when(adapterService.buildReportLog(1)).thenReturn(reportLog(monitor(1, 120), monitor(2, 100)));
        Mockito.when(monomerMapper.selectList(10L)).thenReturn(Arrays.asList(
                new DevBatteryMonomer(10L, 1, 100),
                new DevBatteryMonomer(10L, 2, 100)));

        Double maxResistance = service.getMaxResistance(1);

        Assertions.assertEquals(0.2d, maxResistance, 0.0001d);
    }

    private DevBatteryMonomerServiceImpl newService(boolean realtimeSourceEnabled) {
        DevBatteryMonomerServiceImpl service = new DevBatteryMonomerServiceImpl();
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setJsonTcpRealtimeSourceEnabled(realtimeSourceEnabled);
        ReflectionTestUtils.setField(service, "batteryCollectorProperties", properties);
        ReflectionTestUtils.setField(service, "batteryModuleReportLogAdapterService",
                Mockito.mock(BatteryModuleReportLogAdapterService.class));
        ReflectionTestUtils.setField(service, "batteryReportLogService", Mockito.mock(BatteryReportLogService.class));
        ReflectionTestUtils.setField(service, "batteryPackService", Mockito.mock(IBatteryPackService.class));
        ReflectionTestUtils.setField(service, "devBatteryMonomerMapper", Mockito.mock(DevBatteryMonomerMapper.class));
        return service;
    }

    private BatteryReportLog reportLog(BatteryMonitor... monitors) {
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setBatteryList(monitors == null ? Collections.emptyList() : Arrays.asList(monitors));
        return reportLog;
    }

    private BatteryMonitor monitor(Integer batNum, Integer resistance) {
        BatteryMonitor monitor = new BatteryMonitor();
        monitor.setBatNum(batNum);
        monitor.setResistance(resistance);
        return monitor;
    }
}
