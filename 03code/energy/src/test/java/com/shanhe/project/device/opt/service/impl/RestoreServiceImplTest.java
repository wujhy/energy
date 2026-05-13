package com.shanhe.project.device.opt.service.impl;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.energy.stat.service.IStatBatteryBatService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class RestoreServiceImplTest {

    @Test
    void delPackShouldIgnoreRequestConfigIdAndUseDefaultConfig() {
        RestoreServiceImpl service = new RestoreServiceImpl();
        IConfigService configService = Mockito.mock(IConfigService.class);
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class);
        IDevBatteryMonomerService devBatteryMonomerService = Mockito.mock(IDevBatteryMonomerService.class);
        IDevBatteryOptService devBatteryOptService = Mockito.mock(IDevBatteryOptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class);
        BatteryModuleRealtimeMapper batteryModuleRealtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        IStatBatteryBatService statBatteryBatService = Mockito.mock(IStatBatteryBatService.class);
        IStatBatteryPackService statBatteryPackService = Mockito.mock(IStatBatteryPackService.class);
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class);
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class);

        Config config = new Config();
        config.setConfigId(Constants.DEFAULT_CONFIG_ID);
        Mockito.when(configService.selectConfigByConfigId(Constants.DEFAULT_CONFIG_ID)).thenReturn(config);
        BatteryPack batteryPack = new BatteryPack();
        batteryPack.setPackId(12L);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(Constants.DEFAULT_CONFIG_ID, 2)).thenReturn(batteryPack);

        ReflectionTestUtils.setField(service, "configService", configService);
        ReflectionTestUtils.setField(service, "batteryPackService", batteryPackService);
        ReflectionTestUtils.setField(service, "devBatteryMonomerService", devBatteryMonomerService);
        ReflectionTestUtils.setField(service, "devBatteryOptService", devBatteryOptService);
        ReflectionTestUtils.setField(service, "alarmLogService", alarmLogService);
        ReflectionTestUtils.setField(service, "batteryReportLogService", batteryReportLogService);
        ReflectionTestUtils.setField(service, "batteryModuleRealtimeMapper", batteryModuleRealtimeMapper);
        ReflectionTestUtils.setField(service, "optLogService", optLogService);
        ReflectionTestUtils.setField(service, "statBatteryBatService", statBatteryBatService);
        ReflectionTestUtils.setField(service, "statBatteryPackService", statBatteryPackService);
        ReflectionTestUtils.setField(service, "statBatteryResService", statBatteryResService);
        ReflectionTestUtils.setField(service, "preBatteryGroupService", preBatteryGroupService);

        BatterySetVO request = new BatterySetVO();
        request.setConfigId(99L);
        request.setPackNum(2);

        service.delPack(request);

        Mockito.verify(configService).selectConfigByConfigId(Constants.DEFAULT_CONFIG_ID);
        Mockito.verify(batteryPackService).selectBatteryInfoByPackNum(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(devBatteryOptService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(alarmLogService).deleteAlarmLogByConfigIdPackNum(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(batteryReportLogService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(optLogService).deleteByConfigIdPackNum(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(statBatteryBatService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(statBatteryPackService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(statBatteryResService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(preBatteryGroupService).deleteByConfigId(Constants.DEFAULT_CONFIG_ID, 2);
        Mockito.verify(configService, Mockito.never()).selectConfigByConfigId(99L);
        Mockito.verify(batteryPackService, Mockito.never()).selectBatteryInfoByPackNum(99L, 2);
    }
}
