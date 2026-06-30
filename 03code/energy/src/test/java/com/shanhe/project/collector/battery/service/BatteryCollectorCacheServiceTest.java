package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class BatteryCollectorCacheServiceTest {

    private final BatteryCollectorCacheService service = new BatteryCollectorCacheService();

    @Test
    void shouldResetSelectedChannelAndEvictRealtimeSnapshot() {
        BatteryCollectorChannelState selected = channelState("battery-group-1", 1);
        selected.getActiveModuleAddresses().add(8);
        selected.getModuleAddressMissCounts().put(8, 2);
        selected.getFullDiscoveryRequested().set(false);
        BatteryCollectorChannelState other = channelState("battery-group-2", 2);
        other.getActiveModuleAddresses().add(9);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        boolean matched = service.resetModuleAddressCache(Arrays.asList(selected, other),
                snapshotService, "battery-group-1");

        Assertions.assertTrue(matched);
        Assertions.assertTrue(selected.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(selected.getModuleAddressMissCounts().isEmpty());
        Assertions.assertTrue(selected.getFullDiscoveryRequested().get());
        Assertions.assertFalse(other.getActiveModuleAddresses().isEmpty());
        Mockito.verify(snapshotService).evict(1);
        Mockito.verify(snapshotService, Mockito.never()).evict(2);
    }

    @Test
    void shouldResetSelectedBatteryGroupOnly() {
        BatteryCollectorChannelState selected = channelState("battery-group-1", 1);
        selected.getActiveModuleAddresses().add(8);
        BatteryCollectorChannelState other = channelState("battery-group-2", 2);
        other.getActiveModuleAddresses().add(9);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        boolean matched = service.resetModuleAddressCacheByBatteryGroup(Arrays.asList(selected, other),
                snapshotService, 1);

        Assertions.assertTrue(matched);
        Assertions.assertTrue(selected.getActiveModuleAddresses().isEmpty());
        Assertions.assertFalse(other.getActiveModuleAddresses().isEmpty());
        Mockito.verify(snapshotService).evict(1);
        Mockito.verify(snapshotService, Mockito.never()).evict(2);
    }

    @Test
    void shouldClearDeviceStateDedupCacheForSelectedBatteryGroupOnly() {
        Map<String, String> lastStateValues = new ConcurrentHashMap<>();
        lastStateValues.put("1:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-1:CHANNEL_OPEN", "open");
        lastStateValues.put("battery-group-1:8:MODULE_ACTIVE", "active");
        lastStateValues.put("2:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-2:CHANNEL_OPEN", "open");

        int removed = service.clearDeviceStateDedupCacheByBatteryGroup(lastStateValues,
                Arrays.asList(channelState("battery-group-1", 1), channelState("battery-group-2", 2)),
                1);

        Assertions.assertEquals(3, removed);
        Assertions.assertFalse(lastStateValues.containsKey("1:GROUP_246_FRESHNESS"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:CHANNEL_OPEN"));
        Assertions.assertFalse(lastStateValues.containsKey("battery-group-1:8:MODULE_ACTIVE"));
        Assertions.assertTrue(lastStateValues.containsKey("2:GROUP_246_FRESHNESS"));
        Assertions.assertTrue(lastStateValues.containsKey("battery-group-2:CHANNEL_OPEN"));
    }

    @Test
    void shouldClearAllDeviceStateDedupCacheWhenBatteryGroupMissing() {
        Map<String, String> lastStateValues = new ConcurrentHashMap<>();
        lastStateValues.put("1:GROUP_246_FRESHNESS", "fresh");
        lastStateValues.put("battery-group-1:CHANNEL_OPEN", "open");

        int removed = service.clearDeviceStateDedupCacheByBatteryGroup(lastStateValues, null, null);

        Assertions.assertEquals(2, removed);
        Assertions.assertTrue(lastStateValues.isEmpty());
    }

    @Test
    void shouldNotThrowWhenResetSingleStateWithNull() {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        Assertions.assertDoesNotThrow(() -> service.resetModuleAddressCache(null, snapshotService));
    }

    @Test
    void shouldResetStateAndSkipSnapshotEvictionWhenServiceIsNull() {
        BatteryCollectorChannelState state = channelState("ch-1", 1);
        state.getActiveModuleAddresses().add(5);
        state.getModuleAddressMissCounts().put(5, 1);

        service.resetModuleAddressCache(state, null);

        Assertions.assertTrue(state.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(state.getModuleAddressMissCounts().isEmpty());
        Assertions.assertTrue(state.getFullDiscoveryRequested().get());
    }

    @Test
    void shouldNotThrowWhenClearRealtimeSnapshotWithNullService() {
        BatteryCollectorChannelState state = channelState("ch-1", 1);
        Assertions.assertDoesNotThrow(() -> service.clearRealtimeSnapshot(state, null));
    }

    @Test
    void shouldNotThrowWhenClearRealtimeSnapshotWithNullState() {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        Assertions.assertDoesNotThrow(() -> service.clearRealtimeSnapshot(null, snapshotService));
        Mockito.verifyNoInteractions(snapshotService);
    }

    @Test
    void shouldNotThrowWhenResetListWithNullChannelStates() {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        boolean matched = service.resetModuleAddressCache(null, snapshotService, "any");

        Assertions.assertFalse(matched);
    }

    @Test
    void shouldResetAllChannelsWhenBatteryGroupIsNull() {
        BatteryCollectorChannelState ch1 = channelState("ch-1", 1);
        ch1.getActiveModuleAddresses().add(5);
        BatteryCollectorChannelState ch2 = channelState("ch-2", 2);
        ch2.getActiveModuleAddresses().add(9);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        boolean matched = service.resetModuleAddressCacheByBatteryGroup(
                Arrays.asList(ch1, ch2), snapshotService, null);

        Assertions.assertTrue(matched);
        Assertions.assertTrue(ch1.getActiveModuleAddresses().isEmpty());
        Assertions.assertTrue(ch2.getActiveModuleAddresses().isEmpty());
        Mockito.verify(snapshotService).evict(1);
        Mockito.verify(snapshotService).evict(2);
    }

    @Test
    void shouldReturnFalseWhenNoChannelMatchesName() {
        BatteryCollectorChannelState ch1 = channelState("ch-1", 1);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);

        boolean matched = service.resetModuleAddressCache(
                java.util.Collections.singletonList(ch1), snapshotService, "nonexistent");

        Assertions.assertFalse(matched);
    }

    @Test
    void shouldReturnZeroWhenClearDedupCacheWithNullMap() {
        int removed = service.clearDeviceStateDedupCacheByBatteryGroup(null, null, 1);
        Assertions.assertEquals(0, removed);
    }

    private BatteryCollectorChannelState channelState(String name, Integer batteryGroup) {
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName(name);
        config.setBatteryGroup(batteryGroup);
        return new BatteryCollectorChannelState(config);
    }
}
