package com.shanhe.project.device.opt.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.*;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.device.opt.service.RestoreService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.energy.capacity.service.PreBatteryGroupService;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.energy.stat.service.IStatBatteryBatService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import com.shanhe.project.monitor.operlog.service.IOperLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @author zhoubin
 * @date 2025/10/14
 */
@Service
public class RestoreServiceImpl implements RestoreService {

    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IStatBatteryBatService statBatteryBatService;
    @Resource
    private IStatBatteryPackService statBatteryPackService;
    @Resource
    private IStatBatteryResService statBatteryResService;
    @Resource
    private PreBatteryGroupService preBatteryGroupService;
    @Resource
    private IOperLogService operLogService;
    @Resource
    private IHostService hostService;
    @Resource
    private ControlBatterySet controlBatterySet;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    public IConfigService configService;
    @Resource
    private BatteryModuleRealtimeMapper batteryModuleRealtimeMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(BatterySetVO batterySetVO) {
        Config config = configService.selectDefaultConfig();
        if (config == null) {
            throw new ServiceException("设备不存在！");
        }

        // 内阻初装值
        devBatteryMonomerService.delete();
        // 电池操作记录
        devBatteryOptService.deleteByConfigId(null);

        // 删除属性
        configAttributeService.deleteDefaultDeviceAttributes();
        configAttributeService.updateCache();
        // 删除告警
        alarmLogService.deleteDefaultDeviceAlarmLogs();
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByConfigId(null);
        batteryReportLogService.updateCache();

        // 删除操作日志
        optLogService.deleteDefaultDeviceLogs();
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByConfigId(null);
        statBatteryPackService.deleteByConfigId(null);

        // 删除内阻统计数据
        statBatteryResService.deleteByConfigId(null);

        // 删除预估容量
        preBatteryGroupService.deleteByConfigId(null);
        preBatteryGroupService.updateCache();

        // 系统操作记录
        operLogService.cleanOperLog();

        batteryPackService.deleteDefaultDevicePacks();
        batteryPackService.updateCache();

        // 清空主机基本信息
        hostService.restore();

        balanced();
        buzzerStatus();

        // M460 source command 0x75/0xF5 restored factory defaults on the old board.
        // energy now owns the local cleanup above and does not send the old aggregate frame.
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPack(BatterySetVO batterySetVO) {
        Config config = configService.selectDefaultConfig();
        if (config == null) {
            throw new RuntimeException("设备不存在！");
        }

        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(batterySetVO.getPackNum());
        if (null == batteryPack) {
            throw new ServiceException("电池组不存在，操作执行失败！");
        }

        // 内阻初装值
        devBatteryMonomerService.deleteByPackId(batteryPack.getPackId());
        // 电池操作记录
        devBatteryOptService.deleteByConfigId(batterySetVO.getPackNum());

        // 删除告警
        alarmLogService.deleteBatteryAlarmLogByPackNum(batterySetVO.getPackNum());
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByConfigId(batterySetVO.getPackNum());
        batteryReportLogService.updateCache();

        // 删除600节模块端标准实时数据
        batteryModuleRealtimeMapper.deleteCellsByPackNum(batterySetVO.getPackNum());
        batteryModuleRealtimeMapper.deleteGroupByPackNum(batterySetVO.getPackNum());

        // 删除操作日志
        optLogService.deleteByPackNum(batterySetVO.getPackNum());
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByConfigId(batterySetVO.getPackNum());
        statBatteryPackService.deleteByConfigId(batterySetVO.getPackNum());

        // 删除内阻统计数据
        statBatteryResService.deleteByConfigId(batterySetVO.getPackNum());

        // 删除预估容量
        preBatteryGroupService.deleteByConfigId(batterySetVO.getPackNum());
        preBatteryGroupService.updateCache();

    }

    private void buzzerStatus() {
        controlBatterySet.saveBuzzerStatus(0);
    }

    private void balanced() {
        controlBatterySet.saveBalancedStatus(0, 0);
    }
}
