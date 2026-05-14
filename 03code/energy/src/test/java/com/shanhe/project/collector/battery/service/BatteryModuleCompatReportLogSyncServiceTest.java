package com.shanhe.project.collector.battery.service;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.iot.service.DataService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class BatteryModuleCompatReportLogSyncServiceTest {

    @Test
    void shouldSkipWhenChannelIdentityIncomplete() {
        BatteryModuleCompatReportLogSyncService service = new BatteryModuleCompatReportLogSyncService();
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        ReflectionTestUtils.setField(service, "adapterService", adapterService);
        ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);

        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();

        service.sync(channelConfig, new BatteryModuleGroupRealtime(), Collections.emptyList());

        Mockito.verifyNoInteractions(adapterService);
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void shouldSkipWhenAdaptedReportHasNoCells() {
        BatteryModuleCompatReportLogSyncService service = new BatteryModuleCompatReportLogSyncService();
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        DataService dataService = Mockito.mock(DataService.class);
        ReflectionTestUtils.setField(service, "adapterService", adapterService);
        ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(service, "dataService", dataService);
        Mockito.when(adapterService.buildReportLog(Mockito.eq(Constants.DEFAULT_CONFIG_ID), Mockito.eq(1), Mockito.any(), Mockito.any()))
                .thenReturn(new BatteryReportLog());

        service.sync(channelConfig(), new BatteryModuleGroupRealtime(), Collections.emptyList());

        Mockito.verify(adapterService).buildReportLog(Mockito.eq(Constants.DEFAULT_CONFIG_ID), Mockito.eq(1), Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(dataService);
        Mockito.verifyNoInteractions(reportLogService);
    }

    @Test
    void shouldInsertAdaptedReportThroughOldHistoryService() {
        BatteryModuleCompatReportLogSyncService service = new BatteryModuleCompatReportLogSyncService();
        BatteryModuleReportLogAdapterService adapterService = Mockito.mock(BatteryModuleReportLogAdapterService.class);
        BatteryReportLogService reportLogService = Mockito.mock(BatteryReportLogService.class);
        DataService dataService = Mockito.mock(DataService.class);
        ReflectionTestUtils.setField(service, "adapterService", adapterService);
        ReflectionTestUtils.setField(service, "batteryReportLogService", reportLogService);
        ReflectionTestUtils.setField(service, "dataService", dataService);

        Map<String, Object> packParam = new LinkedHashMap<>();
        packParam.put("packVoltage", "220.1");
        BatteryMonitor monitor = new BatteryMonitor();
        BatteryReportLog reportLog = new BatteryReportLog();
        reportLog.setPackParam(packParam);
        reportLog.setBatteryList(Collections.singletonList(monitor));
        Mockito.when(adapterService.buildReportLog(Mockito.eq(Constants.DEFAULT_CONFIG_ID), Mockito.eq(1), Mockito.any(), Mockito.any()))
                .thenReturn(reportLog);
        Mockito.when(dataService.isInsert("1")).thenReturn(true);

        service.sync(channelConfig(), new BatteryModuleGroupRealtime(), Collections.singletonList(new BatteryModuleCellRealtime()));

        Mockito.verify(reportLogService).insert(1, packParam, reportLog.getBatteryList(), true);
    }

    private BatteryCollectorChannelConfig channelConfig() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setConfigId(10L);
        channelConfig.setBatteryGroup(1);
        return channelConfig;
    }
}
