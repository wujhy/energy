package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

class BatteryModeStatusServiceTest {

    private final BatteryModeStatusService service = new BatteryModeStatusService();
    private final TestCacheAccessor cacheAccessor = new TestCacheAccessor();

    BatteryModeStatusServiceTest() {
        ReflectionTestUtils.setField(service, "cacheAccessor", cacheAccessor);
    }

    @Test
    void shouldReturnIdleWhenCacheIsMissing() {
        BatteryModeInfo modeInfo = service.get(2);

        Assertions.assertEquals(2, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getResult());
        Assertions.assertEquals(0, modeInfo.getStatus());
    }

    @Test
    void shouldUseConfigIndependentCacheKey() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);

        Object result = cacheAccessor.get("device-result", service.key());

        Assertions.assertTrue(result instanceof BatteryModeInfo);
        BatteryModeInfo modeInfo = (BatteryModeInfo) result;
        Assertions.assertEquals(2, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(8, modeInfo.getAddress());
    }

    @Test
    void shouldMarkStoppedAndKeepLastMode() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);

        service.markStopped(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, true);
        BatteryModeInfo modeInfo = service.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(0, modeInfo.getResult());
        Assertions.assertEquals(2, modeInfo.getLastPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getLastMode());
        Assertions.assertEquals(8, modeInfo.getLastAddress());
    }

    @Test
    void shouldPersistWorkModeWithOptLogId() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", stateService);

        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 123L);
        service.markStopped(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, true, 123L);

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(2)).upsert(captor.capture());
        Assertions.assertEquals(123L, captor.getAllValues().get(0).getOptLogId());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.RUNNING, captor.getAllValues().get(0).getStateLevel());
        Assertions.assertEquals(123L, captor.getAllValues().get(1).getOptLogId());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.NORMAL, captor.getAllValues().get(1).getStateLevel());
    }

    @Test
    void shouldKeepM460TemporaryIdleForInitialInternalResistance() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 1);
        BatteryModeInfo m460Idle = new BatteryModeInfo();
        m460Idle.setPackNum(2);
        m460Idle.setResult(0);
        m460Idle.setMode(BatteryModeStatusService.MODE_IDLE);
        m460Idle.setStatus(0);
        m460Idle.setAddress(0);

        service.putFromM460(m460Idle);
        BatteryModeInfo modeInfo = service.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(1, modeInfo.getAddress());
    }

    @Test
    void shouldClearCacheWhenPackNumMatches() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);

        service.clear(2);

        BatteryModeInfo modeInfo = service.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
    }

    @Test
    void shouldNotClearCacheWhenPackNumDoesNotMatch() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);

        service.clear(3);

        BatteryModeInfo modeInfo = service.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
    }

    @Test
    void shouldClearCacheUnconditionallyWhenPackNumIsNull() {
        service.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 5);

        service.clear(null);

        BatteryModeInfo modeInfo = service.get(2);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
    }

    @Test
    void shouldNotThrowWhenClearOnEmptyCache() {
        Assertions.assertDoesNotThrow(() -> service.clear(1));
        Assertions.assertDoesNotThrow(() -> service.clear(null));
    }

    @Test
    void shouldMarkStoppedWithFailureResultAndWarnLevel() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", stateService);

        service.markRunning(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 5, 200L);
        service.markStopped(1, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 5, false, 200L);

        BatteryModeInfo modeInfo = service.get(1);
        Assertions.assertEquals(1, modeInfo.getResult());
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());

        ArgumentCaptor<BatteryDeviceState> captor = ArgumentCaptor.forClass(BatteryDeviceState.class);
        Mockito.verify(stateService, Mockito.times(2)).upsert(captor.capture());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateLevel.WARN,
                captor.getAllValues().get(1).getStateLevel());
    }

    @Test
    void shouldFallbackLastModeWhenNoPreviousCacheEntry() {
        service.markStopped(3, BatteryModeStatusService.MODE_BALANCE, 10, true);

        BatteryModeInfo modeInfo = service.get(3);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(3, modeInfo.getLastPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_BALANCE, modeInfo.getLastMode());
    }

    @Test
    void shouldIgnoreNullM460Input() {
        Assertions.assertDoesNotThrow(() -> service.putFromM460(null));
        BatteryModeInfo modeInfo = service.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
    }

    @Test
    void shouldApplyM460StopWhenAddressIsNotOne() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);
        BatteryModeInfo m460Stop = new BatteryModeInfo();
        m460Stop.setPackNum(2);
        m460Stop.setResult(0);
        m460Stop.setMode(BatteryModeStatusService.MODE_IDLE);
        m460Stop.setStatus(0);
        m460Stop.setAddress(5);

        service.putFromM460(m460Stop);
        BatteryModeInfo modeInfo = service.get(2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(0, modeInfo.getStatus());
        Assertions.assertEquals(5, modeInfo.getAddress());
    }

    @Test
    void shouldApplyM460RunningStatusDirectly() {
        BatteryModeInfo running = new BatteryModeInfo();
        running.setPackNum(1);
        running.setResult(0);
        running.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
        running.setStatus(1);
        running.setAddress(3);

        service.putFromM460(running);
        BatteryModeInfo modeInfo = service.get(1);

        Assertions.assertEquals(BatteryModeStatusService.MODE_AUTO_MODEL_NUM, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(3, modeInfo.getAddress());
    }

    @Test
    void shouldSwallowPersistException() {
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.doThrow(new RuntimeException("db error")).when(stateService).upsert(Mockito.any());
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", stateService);

        Assertions.assertDoesNotThrow(() ->
                service.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 100L));

        BatteryModeInfo modeInfo = service.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
    }

    @Test
    void shouldReturnIdleWhenCacheContainsNonModeInfo() {
        cacheAccessor.put("device-result", service.key(), "not-a-mode-info");

        BatteryModeInfo modeInfo = service.get(5);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(5, modeInfo.getPackNum());
    }

    @Test
    void shouldReturnExactKey() {
        Assertions.assertEquals("battery:mode:status:EB", service.key());
    }

    @Test
    void shouldNotPersistWhenDeviceStateServiceIsNull() {
        ReflectionTestUtils.setField(service, "batteryDeviceStateService", null);

        Assertions.assertDoesNotThrow(() ->
                service.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8, 50L));

        BatteryModeInfo modeInfo = service.get(1);
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
    }

    private static class TestCacheAccessor implements BatteryModeStatusService.CacheAccessor {
        private final Map<String, Object> cache = new HashMap<>();

        @Override
        public Object get(String cacheName, String key) {
            return cache.get(cacheName + ":" + key);
        }

        @Override
        public void put(String cacheName, String key, Object value) {
            cache.put(cacheName + ":" + key, value);
        }

        @Override
        public void remove(String cacheName, String key) {
            cache.remove(cacheName + ":" + key);
        }
    }
}
