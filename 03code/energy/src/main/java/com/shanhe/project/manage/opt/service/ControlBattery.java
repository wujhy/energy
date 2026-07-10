package com.shanhe.project.manage.opt.service;

import cn.hutool.core.util.ObjUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.*;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import com.shanhe.project.manage.opt.domain.OptLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * 设备控制。
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class ControlBattery extends ControlBase {

    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** 电池模组实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;
    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;
    /** 测试计划采集命令适配器。 */
    @Resource
    private BatteryOptCollectorCommandAdapter batteryOptCollectorCommandAdapter;
    /** 核容/备电模块命令适配器。 */
    @Resource
    private BatteryOptCapacityModuleCommandAdapter batteryOptCapacityModuleCommandAdapter;

    /** 统一执行蓄电池测试命令，页面立即执行和计划任务触发共用该入口。 */
    public AjaxResult executeBatteryOpt(DevBatteryOpt opt, BatteryOptExecuteType executeType) {
        return executeBatteryOptInternal(opt, executeType);
    }

    /** 立即执行蓄电池操作。 */
    public AjaxResult toSendBatteryCmdToOat(DevBatteryOpt opt) {
        return executeBatteryOptInternal(opt, BatteryOptExecuteType.MANUAL);
    }

    /** 统一执行蓄电池测试命令，所有新测试类型适配都从这里接入。 */
    private AjaxResult executeBatteryOptInternal(DevBatteryOpt opt, BatteryOptExecuteType executeType) {
        BatteryCommandContext context = getCommandContext(opt, executeType);
        if (isUnsupportedCommandType(context.testEnum)) {
            return AjaxResult.error("下发蓄电池测试指令类型失败");
        }

        AjaxResult validateResult = validateCommandContext(context);
        if (validateResult != null) {
            return validateResult;
        }

        AjaxResult adaptedResult = tryExecuteAdaptedCommand(context);
        if (adaptedResult != null) {
            return adaptedResult;
        }
        return AjaxResult.error("下发蓄电池测试指令类型失败");
    }

    /** 尝试走已迁移的新链路；已迁移类型必须返回明确结果。 */
    private AjaxResult tryExecuteAdaptedCommand(BatteryCommandContext context) {
        try {
            switch (context.testEnum) {
                case _4:
                    return AjaxResult.error("浮充测试当前属于 0x31 浮充参数配置转发，不是 600 采集测试命令，请继续通过计划配置保存链路处理", 0);
                case _3:
                case _5:
                    return batteryOptCapacityModuleCommandAdapter.tryExecute(context);
                case _1:
                case _2:
                case _6:
                    AjaxResult collectorResult = batteryOptCollectorCommandAdapter.tryExecutePrepared(context);
                    if (collectorResult != null) {
                        log.info("蓄电池测试已走采集模块命令 packNum={}, testType={}, executeType={}",
                                context.opt.getPackNum(), context.testEnum.getDictValue(), context.executeType);
                    }
                    return collectorResult;
                default:
                    return AjaxResult.error("当前测试类型暂不支持适配命令执行", 0);
            }
        } catch (ServiceException e) {
            log.warn("蓄电池测试适配命令业务异常 packNum={}, testType={}, executeType={}, message={}",
                    context.opt.getPackNum(), context.testEnum.getDictValue(), context.executeType, e.getMessage());
            return AjaxResult.error(e.getMessage(), 0);
        } catch (Exception e) {
            log.error("蓄电池测试适配命令执行异常 packNum={}, testType={}, executeType={}",
                    context.opt.getPackNum(), context.testEnum.getDictValue(), context.executeType, e);
            return AjaxResult.error("蓄电池测试适配命令执行异常", 0);
        }
    }

    /** 立即执行停止测试操作。 */
    public AjaxResult toSendStopBatteryCmdToOat(DevBatteryOpt opt) {
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());
        if (testEnum == null || BatteryTestEnum._99.equals(testEnum)) {
            return AjaxResult.error("停止蓄电池测试指令类型错误");
        }
        switch (testEnum) {
            case _1:
            case _2:
            case _6:
                return batteryOptCollectorCommandAdapter.tryStop(opt);
            case _3:
            case _5:
                return batteryOptCapacityModuleCommandAdapter.tryStop(opt);
            default:
                return AjaxResult.error("停止蓄电池测试指令类型错误");
        }
    }

    /** 统一入口业务校验。 */
    private AjaxResult validateCommandContext(BatteryCommandContext context) {
        if (context.opt == null || context.opt.getPackNum() == null) {
            return null;
        }

        // 是否空闲
        List<OptLog> runningLogs = optLogService.selectRunningList(context.opt.getPackNum());
        if (runningLogs != null && !runningLogs.isEmpty()) {
            return AjaxResult.error("蓄电池正在执行测试工作，请稍后再试！", 0);
        }

        // 是否采集
        BatteryModuleGroupRealtime realtimeGroup = getRealtimeGroup(context.opt.getPackNum());
        if (realtimeGroup == null) {
            return AjaxResult.error("暂无上报数据", 0);
        }

        // 是否连接
        AlarmLog alarmLog = alarmLogService.getByCache(context.opt.getPackNum(), null, ItemCode.TXZT.getCode());
        if (null != alarmLog && ObjUtil.equals(YesNoEnum.NO.getDictValue(), alarmLog.getStatus())) {
            return AjaxResult.error(alarmLog.getDataInfo(), 0);
        }

        return validateTestCondition(context, realtimeGroup);
    }

    /** 校验测试条件 */
    private AjaxResult validateTestCondition(BatteryCommandContext context, BatteryModuleGroupRealtime realtimeGroup) {
        if (BatteryTestEnum._2.equals(context.testEnum)) {
            Double current = realtimeGroup.getPackCurrent();
            if (current != null && Math.abs(current) < 5) {
                throw new RuntimeException("电池组未到达测试条件，需组电流超过 5A 才可以进行连接条测试");
            }
        } else if (BatteryTestEnum._6.equals(context.testEnum)) {
            Integer modelNum = context.opt.getModelNum();
            if (modelNum == null || modelNum < 1 || context.batteryCount < 1 || modelNum > context.batteryCount) {
                return AjaxResult.error("单节内阻测试单体编号无效", 0);
            }
        } else {
            String batteryPackStatus = Objects.toString(realtimeGroup.getBatteryPackStatus(), null);
            if (!BatteryPackStatusEnum.isCode(batteryPackStatus, BatteryPackStatusEnum.IDLE)) {
                return AjaxResult.error("电池组处于非空闲状态，不允许测试！", 0);
            }
        }
        return null;
    }

    private BatteryModuleGroupRealtime getRealtimeGroup(Integer packNum) {
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService == null
                ? null : realtimeSnapshotService.getCachedSnapshot(packNum);
        return snapshot == null ? null : snapshot.getGroup();
    }

    /** 把统一执行入口来源写入旧 dev_opt_log.source，便于区分页面、计划和同步触发。 */
    private String resolveOptLogSource(BatteryOptExecuteType executeType) {
        if (executeType == null) {
            return null;
        }
        switch (executeType) {
            case MANUAL:
                return BatteryDeviceStateConstants.Source.WEB;
            case SCHEDULED:
                return BatteryDeviceStateConstants.Source.AUTO;
            case SYNC:
                return BatteryDeviceStateConstants.Source.JSON_TCP;
            default:
                return executeType.name().toLowerCase();
        }
    }

    /** 校验设备和电池组信息，入口后续会用到的配置聚合起来。 */
    private BatteryCommandContext getCommandContext(DevBatteryOpt devBatteryOpt, BatteryOptExecuteType executeType) {
        // 设备
        Config config = configService.selectDefaultConfig();
        if (config == null) {
            throw new ServiceException("设备不存在，操作执行失败");
        }
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            throw new ServiceException("非蓄电池设备，操作执行失败！");
        }

        // 蓄电池组
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(devBatteryOpt.getPackNum());
        if (batteryPack == null) {
            throw new ServiceException("电池组不存在，操作执行失败！");
        }
        if (Objects.equals(devBatteryOpt.getTestType(), BatteryTestEnum._5.getDictValue())
                && Objects.equals(batteryPack.getIsAllowPower(), YesNoEnum.NO.getDictValue())) {
            throw new ServiceException("该电池组不允许测试！");
        }
        BatteryTestEnum testEnum = BatteryTestEnum.find(devBatteryOpt.getTestType());
        Integer batteryCount = batteryPack.getBatSinSize();

        return new BatteryCommandContext(
                devBatteryOpt,
                testEnum,
                executeType,
                config,
                batteryPack,
                batteryCount == null ? 0 : Math.min(batteryCount, 245),
                resolveOptLogSource(executeType));
    }

    /** 判断是否为当前控制链路不支持的测试类型。 */
    private boolean isUnsupportedCommandType(BatteryTestEnum testEnum) {
        return testEnum == null || BatteryTestEnum._99.equals(testEnum);
    }
}
