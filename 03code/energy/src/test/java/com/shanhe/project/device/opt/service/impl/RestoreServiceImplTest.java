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
        IBatteryPackService batteryPackService = Mockito.mock(IBatteryPackService.class, Mockito.CALLS_REAL_METHODS);
        IDevBatteryMonomerService devBatteryMonomerService = Mockito.mock(IDevBatteryMonomerService.class);
        IDevBatteryOptService devBatteryOptService = Mockito.mock(IDevBatteryOptService.class);
        IAlarmLogService alarmLogService = Mockito.mock(IAlarmLogService.class, Mockito.CALLS_REAL_METHODS);
        BatteryReportLogService batteryReportLogService = Mockito.mock(BatteryReportLogService.class, Mockito.CALLS_REAL_METHODS);
        BatteryModuleRealtimeMapper batteryModuleRealtimeMapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        OptLogService optLogService = Mockito.mock(OptLogService.class);
        IStatBatteryBatService statBatteryBatService = Mockito.mock(IStatBatteryBatService.class, Mockito.CALLS_REAL_METHODS);
        IStatBatteryPackService statBatteryPackService = Mockito.mock(IStatBatteryPackService.class, Mockito.CALLS_REAL_METHODS);
        IStatBatteryResService statBatteryResService = Mockito.mock(IStatBatteryResService.class, Mockito.CALLS_REAL_METHODS);
        PreBatteryGroupService preBatteryGroupService = Mockito.mock(PreBatteryGroupService.class, Mockito.CALLS_REAL_METHODS);

        Config config = new Config();
        config.setConfigId(Constants.DEFAULT_CONFIG_ID);
        Mockito.when(configService.selectDefaultConfig()).thenReturn(config);
        BatteryPack batteryPack = new BatteryPack();
        batteryPack.setPackId(12L);
        Mockito.when(batteryPackService.selectBatteryInfoByPackNum(2)).thenReturn(batteryPack);

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

        Mockito.verify(configService).selectDefaultConfig();
        Mockito.verify(batteryPackService).selectBatteryInfoByPackNum(2);
        Mockito.verify(devBatteryOptService).deleteByPackNum(2);
        Mockito.verify(alarmLogService).deleteBatteryAlarmLogByPackNum(2);
        Mockito.verify(batteryReportLogService).deleteByPackNum(2);
        Mockito.verify(optLogService).deleteByPackNum(2);
        Mockito.verify(statBatteryBatService).deleteByPackNum(2);
        Mockito.verify(statBatteryPackService).deleteByPackNum(2);
        Mockito.verify(statBatteryResService).deleteByPackNum(2);
        Mockito.verify(preBatteryGroupService).deleteByPackNum(2);
    }
}
