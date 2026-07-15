package com.shanhe.project.manage.opt.service;

import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

class BatteryTestLifecycleServiceTest {

    @Test
    void shouldOwnBusinessLogAndModeProjectionLifecycle() {
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        Mockito.when(optLogService.selectRunningList(1)).thenReturn(Collections.emptyList());
        Mockito.when(optLogService.insert(1, 6, null, "web")).thenReturn(100L);
        BatteryTestLifecycleService service = new BatteryTestLifecycleService();
        ReflectionTestUtils.setField(service, "optLogService", optLogService);
        ReflectionTestUtils.setField(service, "modeStatusService", modeStatusService);

        Long id = service.start(1, 6, "web");
        service.markRunning(id);
        service.complete(id, 1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, true);

        InOrder order = Mockito.inOrder(optLogService, modeStatusService);
        order.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.STARTING, null);
        order.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.RUNNING, null);
        order.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.SUCCEEDED, 0);
        order.verify(modeStatusService).markStopped(
                1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, true, 100L);
    }
}
