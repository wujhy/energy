package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.capacity.vo.PreBatteryGroup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryModuleGroupCompatibilityFillServiceTest {

    private final BatteryModuleGroupCompatibilityFillService service = new BatteryModuleGroupCompatibilityFillService();

    @Test
    void shouldFillAliasesAndStatusWithoutPredictionCache() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setAvgCellTemperature(25.6d);
        group.setVoltageRange(0.12d);
        group.setPackCurrent(-3.4d);

        service.fillAfterCalculation(null, group);

        Assertions.assertEquals(25.6d, group.getBatteryAvgTemperature(), 0.0001d);
        Assertions.assertEquals(0.12d, group.getBatteryVoltageRange(), 0.0001d);
        Assertions.assertEquals(5, group.getBatteryPackStatus());
        Assertions.assertNull(group.getBcapacity());
        Assertions.assertNull(group.getBatteryPackSoh());
    }

    @Test
    void shouldLeaveStatusEmptyWhenNotDischarging() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackCurrent(1.0d);

        service.fillAfterCalculation(null, group);

        Assertions.assertNull(group.getBatteryPackStatus());
    }

    @Test
    void shouldFillCapacityFieldsFromPredictionCache() {
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class);
        ReflectionTestUtils.setField(service, "preBatteryGroupService", preBatteryGroupService);
        PreBatteryGroup preBatteryGroup = new PreBatteryGroup();
        preBatteryGroup.setBcapacity(90.5d);
        preBatteryGroup.setBackUpDuration(120);
        preBatteryGroup.setDischargeCapacity(18.6d);
        preBatteryGroup.setSoh(86.4d);
        Mockito.when(preBatteryGroupService.lastCache(10L, 1)).thenReturn(preBatteryGroup);

        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setConfigId(10L);
        channelConfig.setBatteryGroup(1);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();

        service.fillAfterCalculation(channelConfig, group);

        Assertions.assertEquals(90.5d, group.getBcapacity(), 0.0001d);
        Assertions.assertEquals(120, group.getBackupDuration());
        Assertions.assertEquals(18.6d, group.getDisChargeCapacity(), 0.0001d);
        Assertions.assertEquals(86.4d, group.getBatteryPackSoh(), 0.0001d);
    }

    @Test
    void shouldSkipPredictionCacheWhenChannelIdentityMissing() {
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class);
        ReflectionTestUtils.setField(service, "preBatteryGroupService", preBatteryGroupService);
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setBatteryGroup(1);

        service.fillAfterCalculation(channelConfig, new BatteryModuleGroupRealtime());

        Mockito.verifyNoInteractions(preBatteryGroupService);
    }
}
