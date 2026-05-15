package com.shanhe.project.device.config.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

class ConfigServiceImplTest {

    @Test
    void updatePackShouldMatchByPackNumAndIgnoreIncomingPackId() {
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        IConfigAttributeService configAttributeService = Mockito.mock(IConfigAttributeService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);

        BatteryPack oldPack = pack(100L, 1);
        BatteryPack incomingExisting = pack(999L, 1);
        incomingExisting.setBatSinModel(8);
        BatteryPack incomingNew = pack(888L, 2);
        incomingNew.setBatSinModel(9);

        Mockito.when(batteryPackService.selectBatteryPackListConfigId(null)).thenReturn(Collections.singletonList(oldPack));

        ConfigServiceImpl service = new ConfigServiceImpl();
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(service, "configAttributeService", configAttributeService);
        ReflectionTestUtils.setField(service, "alarmLogService", alarmLogService);

        Config config = new Config();
        config.setType(1);
        config.setPackList(Arrays.asList(incomingExisting, incomingNew));

        service.updatePack(config);

        ArgumentCaptor<BatteryPack> updateCaptor = ArgumentCaptor.forClass(BatteryPack.class);
        Mockito.verify(batteryPackService).update(updateCaptor.capture());
        Assertions.assertEquals(Long.valueOf(100L), updateCaptor.getValue().getPackId());
        Assertions.assertEquals(Integer.valueOf(1), updateCaptor.getValue().getPackNum());
        Assertions.assertEquals(Long.valueOf(Constants.DEFAULT_CONFIG_ID), updateCaptor.getValue().getConfigId());

        ArgumentCaptor<BatteryPack> insertCaptor = ArgumentCaptor.forClass(BatteryPack.class);
        Mockito.verify(batteryPackService).insertBatteryPack(insertCaptor.capture());
        Assertions.assertEquals(Integer.valueOf(2), insertCaptor.getValue().getPackNum());
        Assertions.assertEquals(Long.valueOf(Constants.DEFAULT_CONFIG_ID), insertCaptor.getValue().getConfigId());
        Assertions.assertNotNull(insertCaptor.getValue().getPackId());

        Mockito.verify(batteryPackService, Mockito.never()).deleteBatteryPackByBatPackIds(Mockito.anyList());
        Mockito.verify(configAttributeService).insertByTemplateAttribute(2, 9);
        Mockito.verify(configAttributeService, Mockito.never()).deleteConfigAttributeByPackNums(Mockito.anyList());
    }

    private BatteryPack pack(Long packId, Integer packNum) {
        BatteryPack pack = new BatteryPack();
        pack.setPackId(packId);
        pack.setPackNum(packNum);
        return pack;
    }
}
