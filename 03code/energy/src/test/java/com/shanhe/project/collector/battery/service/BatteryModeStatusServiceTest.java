package com.shanhe.project.collector.battery.service;

import com.shanhe.project.iot.model.BatteryModeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
