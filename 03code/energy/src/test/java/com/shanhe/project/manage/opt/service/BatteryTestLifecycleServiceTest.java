package com.shanhe.project.manage.opt.service;

import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.opt.domain.OptLog;
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
        Mockito.when(optLogService.insert(1, 6, null, "web", null)).thenReturn(100L);
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

    @Test
    void shouldLeaveRunStoppingWhenRestoreFails() {
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        OptLog running = runningLog();
        Mockito.when(optLogService.getRunningOptLog(1, 5)).thenReturn(running);
        BatteryTestLifecycleService service = service(optLogService, modeStatusService);

        boolean stopped = service.stop(1, 5, null, null, () -> false);

        org.junit.jupiter.api.Assertions.assertFalse(stopped);
        Mockito.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.STOPPING, null);
        Mockito.verify(optLogService, Mockito.never()).updateRuntime(
                100L, BatteryTestLifecycleService.CANCELLED, 1);
        Mockito.verifyNoInteractions(modeStatusService);
    }

    @Test
    void shouldFinishOnlyAfterStopActionSucceeds() {
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        BatteryModeStatusService modeStatusService = Mockito.mock(BatteryModeStatusService.class);
        Mockito.when(optLogService.getRunningOptLog(1, 6)).thenReturn(runningLog());
        BatteryTestLifecycleService service = service(optLogService, modeStatusService);
        InOrder order = Mockito.inOrder(optLogService, modeStatusService);

        service.stop(1, 6, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, () -> {
            order.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.STOPPING, null);
            return true;
        });

        order.verify(optLogService).updateRuntime(100L, BatteryTestLifecycleService.CANCELLED, 1);
        order.verify(modeStatusService).markStopped(
                1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, true, 100L);
    }

    private BatteryTestLifecycleService service(OptLogService optLogService,
                                                BatteryModeStatusService modeStatusService) {
        BatteryTestLifecycleService service = new BatteryTestLifecycleService();
        ReflectionTestUtils.setField(service, "optLogService", optLogService);
        ReflectionTestUtils.setField(service, "modeStatusService", modeStatusService);
        return service;
    }

    private OptLog runningLog() {
        OptLog log = new OptLog();
        log.setId(100L);
        log.setPackNum(1);
        log.setType(6);
        return log;
    }}
