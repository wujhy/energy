package com.shanhe.project.device.opt.service.impl;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.framework.enums.BatteryCidEnum;
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

        Config config = configService.selectConfigByConfigId(batterySetVO.getConfigId());
        if (config == null) {
            throw new ServiceException("设备不存在！");
        }

        // 内阻初装值
        devBatteryMonomerService.delete();
        // 电池操作记录
        devBatteryOptService.deleteByConfigId(batterySetVO.getConfigId(), null);

        String[] configIdArr = Convert.toStrArray(String.valueOf(batterySetVO.getConfigId()));

        // 删除属性
        configAttributeService.deleteConfigAttributeByConfigIds(configIdArr);
        configAttributeService.updateCache();
        // 删除告警
        alarmLogService.deleteAlarmLogByConfigIds(configIdArr);
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByConfigId(batterySetVO.getConfigId(), null);
        batteryReportLogService.updateCache();

        // 删除操作日志
        optLogService.deleteByConfigIds(configIdArr);
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByConfigId(batterySetVO.getConfigId(), null);
        statBatteryPackService.deleteByConfigId(batterySetVO.getConfigId(), null);

        // 删除内阻统计数据
        statBatteryResService.deleteByConfigId(batterySetVO.getConfigId(), null);

        // 删除预估容量
        preBatteryGroupService.deleteByConfigId(batterySetVO.getConfigId(), null);
        preBatteryGroupService.updateCache();

        // 系统操作记录
        operLogService.cleanOperLog();

        batteryPackService.deleteByConfigIds(configIdArr);
        batteryPackService.updateCache();

        // 清空主机基本信息
        hostService.restore();

        balanced();
        buzzerStatus();

        // 何工的主板恢复出厂指令
        batterySetVO.setNeedDynResult(false);
        controlBatterySet.doSet(batterySetVO, BatteryCidEnum._75);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delPack(BatterySetVO batterySetVO) {
        Config config = configService.selectConfigByConfigId(batterySetVO.getConfigId());
        if (config == null) {
            throw new RuntimeException("设备不存在！");
        }

        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(batterySetVO.getConfigId(), batterySetVO.getPackNum());
        if (null == batteryPack) {
            throw new ServiceException("电池组不存在，操作执行失败！");
        }

        // 内阻初装值
        devBatteryMonomerService.deleteByPackId(batteryPack.getPackId());
        // 电池操作记录
        devBatteryOptService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());

        // 删除告警
        alarmLogService.deleteAlarmLogByConfigIdPackNum(config.getConfigId(), batterySetVO.getPackNum());
        alarmLogService.updateCache();

        // 删除历史记录
        batteryReportLogService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());
        batteryReportLogService.updateCache();

        // 删除600节模块端标准实时数据
        batteryModuleRealtimeMapper.deleteCellsByPackNum(batterySetVO.getPackNum());
        batteryModuleRealtimeMapper.deleteGroupByPackNum(batterySetVO.getPackNum());

        // 删除操作日志
        optLogService.deleteByConfigIdPackNum(batterySetVO.getConfigId(), batterySetVO.getPackNum());
        optLogService.updateCache();

        // 删除统计数据
        statBatteryBatService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());
        statBatteryPackService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());

        // 删除内阻统计数据
        statBatteryResService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());

        // 删除预估容量
        preBatteryGroupService.deleteByConfigId(batterySetVO.getConfigId(), batterySetVO.getPackNum());
        preBatteryGroupService.updateCache();

    }

    private void buzzerStatus() {
        controlBatterySet.saveBuzzerStatus(0);
    }

    private void balanced() {
        controlBatterySet.saveBalancedStatus(0, 0);
    }
}
