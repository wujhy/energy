package com.shanhe.project.manage.config.service.impl;

import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.mapper.BatteryPackMapper;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

class BatteryPackServiceImplTest {

    @Test
    void deleteByPackIdsShouldEvictRealtimeSnapshots() {
        BatteryPackServiceImpl service = new BatteryPackServiceImpl();
        BatteryPackMapper mapper = Mockito.mock(BatteryPackMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        ReflectionTestUtils.setField(service, "batteryPackMapper", mapper);
        ReflectionTestUtils.setField(service, "alarmLogService", Mockito.mock(IAlarmLogService.class));
        ReflectionTestUtils.setField(service, "configAttributeService", Mockito.mock(IConfigAttributeService.class));
        ReflectionTestUtils.setField(service, "realtimeSnapshotService", snapshotService);

        BatteryPack packOne = new BatteryPack();
        packOne.setPackId(11L);
        packOne.setPackNum(1);
        BatteryPack packTwo = new BatteryPack();
        packTwo.setPackId(12L);
        packTwo.setPackNum(2);
        Mockito.when(mapper.selectBatteryPackByPackIds(Arrays.asList(11L, 12L)))
                .thenReturn(Arrays.asList(packOne, packTwo));

        service.deleteBatteryPackByBatPackIds(Arrays.asList(11L, 12L));

        Mockito.verify(mapper).deleteBatteryPackByBatPackIds(Arrays.asList(11L, 12L));
        Mockito.verify(snapshotService).evict(1);
        Mockito.verify(snapshotService).evict(2);
    }
}
