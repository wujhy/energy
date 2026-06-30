package com.shanhe.project.manage.opt.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.service.BatteryCollectorService;
import com.shanhe.project.collector.battery.service.BatteryDeviceStateService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.*;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.manage.opt.service.ControlBatterySet;
import com.shanhe.project.manage.opt.service.OptLogService;
import com.shanhe.project.manage.opt.service.RestoreService;
import com.shanhe.project.manage.opt.vo.BatterySetVO;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.manage.stat.service.IStatBatteryBatService;
import com.shanhe.project.manage.stat.service.IStatBatteryPackService;
import com.shanhe.project.manage.stat.service.IStatBatteryResService;
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

    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 蓄电池单体服务。 */
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;
    /** 蓄电池测试操作参数服务。 */
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** 蓄电池单体统计服务。 */
    @Resource
    private IStatBatteryBatService statBatteryBatService;
    /** 蓄电池组统计服务。 */
    @Resource
    private IStatBatteryPackService statBatteryPackService;
    /** 蓄电池内阻统计服务。 */
    @Resource
    private IStatBatteryResService statBatteryResService;
    /** 预估容量服务。 */
    @Resource
    private PreBatteryGroupService preBatteryGroupService;
    /** 系统操作日志服务。 */
    @Resource
    private IOperLogService operLogService;
    /** 主机服务。 */
    @Resource
    private IHostService hostService;
    /** 蓄电池设置控制服务。 */
    @Resource
    private ControlBatterySet controlBatterySet;
    /** 上报日志服务。 */
    @Resource
    private BatteryReportLogService batteryReportLogService;
    /** 配置属性服务。 */
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    public IConfigService configService;
    /** 模块实时数据访问层。 */
    @Resource
    private BatteryModuleRealtimeMapper batteryModuleRealtimeMapper;
    /** 设备状态服务。 */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;
    /** 工作模式状态服务。 */
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    /** 采集服务。 */
    @Resource
    private BatteryCollectorService batteryCollectorService;
    /** 单体兼容性填充服务。 */
    @Resource
    private com.shanhe.project.collector.battery.service.BatteryModuleCellCompatibilityFillService compatibilityFillService;
    /** 实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;

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
        realtimeSnapshotService.evictAll();

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
        realtimeSnapshotService.evict(batterySetVO.getPackNum());

    }

    private void buzzerStatus() {
        controlBatterySet.saveBuzzerStatus(0);
    }

    private void balanced() {
        controlBatterySet.saveBalancedStatus(0, 0);
    }
}
