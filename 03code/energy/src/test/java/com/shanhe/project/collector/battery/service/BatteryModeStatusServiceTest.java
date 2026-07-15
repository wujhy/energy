package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;
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
    void shouldUsePackScopedCacheKey() {
        service.markRunning(2, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 8);

        Object result = cacheAccessor.get(service.cacheKeyEnum.getCache(), String.format(service.cacheKeyEnum.getKey(), 2));

        Assertions.assertTrue(result instanceof BatteryModeInfo);
        BatteryModeInfo modeInfo = (BatteryModeInfo) result;
        Assertions.assertEquals(2, modeInfo.getPackNum());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, modeInfo.getMode());
        Assertions.assertEquals(1, modeInfo.getStatus());
        Assertions.assertEquals(8, modeInfo.getAddress());
    }

    @Test
    void shouldKeepModesIndependentByPack() {
        service.markRunning(1, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, 1);
        service.markRunning(2, BatteryModeStatusService.MODE_CONNECT_RESISTANCE, 2);

        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, service.get(1).getMode());
        Assertions.assertEquals(BatteryModeStatusService.MODE_CONNECT_RESISTANCE, service.get(2).getMode());
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
    void shouldClearAllPacksWhenPackNumIsNull() {
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
        cacheAccessor.put(service.cacheKeyEnum.getCache(), String.format(service.cacheKeyEnum.getKey(), 5), "not-a-mode-info");

        BatteryModeInfo modeInfo = service.get(5);
        Assertions.assertEquals(BatteryModeStatusService.MODE_IDLE, modeInfo.getMode());
        Assertions.assertEquals(5, modeInfo.getPackNum());
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

        @Override
        public void removeByPrefix(String cacheName, String keyPrefix) {
            cache.keySet().removeIf(key -> key.startsWith(cacheName + ":" + keyPrefix));
        }
    }
}
