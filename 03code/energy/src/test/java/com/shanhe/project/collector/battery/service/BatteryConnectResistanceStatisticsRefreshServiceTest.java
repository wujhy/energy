package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.realtime.BatteryModuleGroupCalculationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryConnectResistanceStatisticsRefreshServiceTest {

    @Test
    void shouldRefreshGroupCalculationAndSnapshotAfterCompletedTest() {
        BatteryConnectResistanceStatisticsRefreshService service = new BatteryConnectResistanceStatisticsRefreshService();
        BatteryModuleGroupCalculationService groupCalculationService = Mockito.mock(BatteryModuleGroupCalculationService.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "groupCalculationService", groupCalculationService);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        service.refreshAfterCompletedTest(2);

        Mockito.verify(groupCalculationService).calculateAndSave(2);
        Mockito.verify(snapshotService).evict(2);
    }

    @Test
    void shouldIgnoreNullPackNum() {
        BatteryConnectResistanceStatisticsRefreshService service = new BatteryConnectResistanceStatisticsRefreshService();
        BatteryModuleGroupCalculationService groupCalculationService = Mockito.mock(BatteryModuleGroupCalculationService.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "groupCalculationService", groupCalculationService);
        ReflectionTestUtils.setField(service, "snapshotService", snapshotService);

        service.refreshAfterCompletedTest(null);

        Mockito.verifyNoInteractions(groupCalculationService, snapshotService);
    }

    @Test
    void shouldSwallowRefreshException() {
        BatteryConnectResistanceStatisticsRefreshService service = new BatteryConnectResistanceStatisticsRefreshService();
        BatteryModuleGroupCalculationService groupCalculationService = Mockito.mock(BatteryModuleGroupCalculationService.class);
        Mockito.doThrow(new RuntimeException("refresh failed"))
                .when(groupCalculationService).calculateAndSave(2);
        ReflectionTestUtils.setField(service, "groupCalculationService", groupCalculationService);

        service.refreshAfterCompletedTest(2);

        Mockito.verify(groupCalculationService).calculateAndSave(2);
    }
}