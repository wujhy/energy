package com.shanhe.project.collector.battery.service;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.capacity.vo.PreBatteryGroup;
import com.shanhe.project.manage.capacity.vo.PreBatteryVo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

class BatteryModuleCellCompatibilityFillServiceTest {

    private final BatteryModuleCellCompatibilityFillService service = new BatteryModuleCellCompatibilityFillService();

    @Test
    void shouldFillConnectResistanceFromCacheByChannelGroup() {
        service.putConnectResistance(1, 8, 12.3d);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(2);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(12.3d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldFallbackToCellPackWhenChannelMissing() {
        service.putConnectResistance(2, 8, 45.6d);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(2);
        cell.setBatNum(8);

        service.fillFromCache(null, cell);

        Assertions.assertEquals(45.6d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldLeaveConnectResistanceEmptyWhenCacheMissing() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertNull(cell.getResistanceRageSlip());
    }

    @Test
    void shouldScopeConnectResistanceByBatteryGroupAndCellNumber() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(1, 9, 23.4d);
        service.putConnectResistance(2, 8, 45.6d);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(2);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(45.6d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldIgnoreNullConnectResistanceInputs() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(null, 8, 99.9d);
        service.putConnectResistance(1, null, 99.9d);
        service.putConnectResistance(1, 8, null);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(12.3d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldNotOverwriteExistingConnectResistanceWhenCacheMissing() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(1);
        cell.setBatNum(8);
        cell.setResistanceRageSlip(77.7d);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertEquals(77.7d, cell.getResistanceRageSlip(), 0.0001d);
    }

    @Test
    void shouldFillCellCapacityFromPredictionCacheOnlyWhenBatteryEntryExists() {
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class);
        ReflectionTestUtils.setField(service, "preBatteryGroupService", preBatteryGroupService);
        PreBatteryVo battery = PreBatteryVo.getNewPreBatteryInfo();
        battery.setBcapacity(88.8d);
        Map<String, PreBatteryVo> mapBattery = new HashMap<>();
        mapBattery.put(Constants.CAP_BAT + 8, battery);
        PreBatteryGroup group = new PreBatteryGroup();
        group.setMapBattery(mapBattery);
        Mockito.when(preBatteryGroupService.lastCache(1)).thenReturn(group);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);

        BatteryModuleCellRealtime cachedCell = new BatteryModuleCellRealtime();
        cachedCell.setBatNum(8);
        service.fillFromCache(channelConfig, cachedCell);
        BatteryModuleCellRealtime missingCell = new BatteryModuleCellRealtime();
        missingCell.setBatNum(9);
        service.fillFromCache(channelConfig, missingCell);

        Assertions.assertEquals(88.8d, cachedCell.getCapacity(), 0.0001d);
        Assertions.assertNull(missingCell.getCapacity());
    }

    @Test
    void shouldNotInventCellCapacityWhenPredictionCacheMissing() {
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class);
        ReflectionTestUtils.setField(service, "preBatteryGroupService", preBatteryGroupService);
        Mockito.when(preBatteryGroupService.lastCache(1)).thenReturn(null);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(8);
        cell.setVoltage(2.1d);
        cell.setResistance(100);

        service.fillFromCache(channelConfig, cell);

        Assertions.assertNull(cell.getCapacity());
    }

    @Test
    void shouldClearOnlySpecifiedPackConnectResistanceCache() {
        service.putConnectResistance(1, 8, 12.3d);
        service.putConnectResistance(2, 8, 45.6d);

        service.clearConnectResistanceCache(1);

        BatteryModuleCellRealtime clearedCell = new BatteryModuleCellRealtime();
        clearedCell.setPackNum(1);
        clearedCell.setBatNum(8);
        service.fillFromCache(null, clearedCell);
        Assertions.assertNull(clearedCell.getResistanceRageSlip());

        BatteryModuleCellRealtime retainedCell = new BatteryModuleCellRealtime();
        retainedCell.setPackNum(2);
        retainedCell.setBatNum(8);
        service.fillFromCache(null, retainedCell);
        Assertions.assertEquals(45.6d, retainedCell.getResistanceRageSlip(), 0.0001d);
    }
}
