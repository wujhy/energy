package com.shanhe.project.device.opt.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.service.BatteryCollectorService;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
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
 * 复位服务实现
 *
 * @author wjh
 * @since 2025/10/14
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
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    @Resource
    private BatteryCollectorService batteryCollectorService;
    @Resource
    private com.shanhe.project.collector.battery.service.BatteryModuleCellCompatibilityFillService compatibilityFillService;

    /**
     * 恢复出厂设置
     *
     * @param batterySetVO 设置参数
     */
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
        devBatteryOptService.deleteByPackNum(null);

        // 删除属性
        configAttributeService.deleteDefaultDeviceAttributes();
        configAttributeService.updateCache();
        // 删除告警
        alarmLogService.deleteDefaultDeviceAlarmLogs();
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByPackNum(null);
        batteryReportLogService.updateCache();

        // 删除操作日志
        optLogService.deleteDefaultDeviceLogs();
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByPackNum(null);
        statBatteryPackService.deleteByPackNum(null);

        // 删除内阻统计数据
        statBatteryResService.deleteByPackNum(null);

        // 删除预估容量
        preBatteryGroupService.deleteByPackNum(null);
        preBatteryGroupService.updateCache();

        // 删除设备状态
        batteryDeviceStateService.deleteAll();
        // 清除采集缓存
        batteryModeStatusService.clear(null);
        batteryCollectorService.resetModuleAddressCacheByBatteryGroup(null);
        batteryCollectorService.clearDeviceStateDedupCacheByBatteryGroup(null);
        compatibilityFillService.clearConnectResistanceCache(null);

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

    /**
     * 删除电池组数据
     *
     * @param batterySetVO 设置参数
     */
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
        devBatteryOptService.deleteByPackNum(batterySetVO.getPackNum());

        // 删除告警
        alarmLogService.deleteBatteryAlarmLogByPackNum(batterySetVO.getPackNum());
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByPackNum(batterySetVO.getPackNum());
        batteryReportLogService.updateCache();

        // 删除600节模块端标准实时数据
        batteryModuleRealtimeMapper.deleteCellsByPackNum(batterySetVO.getPackNum());
        batteryModuleRealtimeMapper.deleteGroupByPackNum(batterySetVO.getPackNum());

        // 删除操作日志
        optLogService.deleteByPackNum(batterySetVO.getPackNum());
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByPackNum(batterySetVO.getPackNum());
        statBatteryPackService.deleteByPackNum(batterySetVO.getPackNum());

        // 删除内阻统计数据
        statBatteryResService.deleteByPackNum(batterySetVO.getPackNum());

        // 删除预估容量
        preBatteryGroupService.deleteByPackNum(batterySetVO.getPackNum());
        preBatteryGroupService.updateCache();

        // 删除设备状态
        batteryDeviceStateService.deleteByPackNum(batterySetVO.getPackNum());
        // 清除采集缓存
        batteryModeStatusService.clear(batterySetVO.getPackNum());
        batteryCollectorService.resetModuleAddressCacheByBatteryGroup(batterySetVO.getPackNum());
        batteryCollectorService.clearDeviceStateDedupCacheByBatteryGroup(batterySetVO.getPackNum());
        compatibilityFillService.clearConnectResistanceCache(batterySetVO.getPackNum());

    }

    private void buzzerStatus() {
        controlBatterySet.saveBuzzerStatus(0);
    }

    private void balanced() {
        controlBatterySet.saveBalancedStatus(0, 0);
    }
}
