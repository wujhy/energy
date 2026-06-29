package com.shanhe.project.device.opt.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.DateUtils;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.*;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.cmd.CmdBatteryControlService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;
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

    /** 旧 M460/980 指令生成器，新测试控制不应继续扩展该链路。 */
    @Resource
    private CmdBatteryControlService cmdBatteryControlService;
    @Resource
    private OptLogService optLogService;
    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private BatteryModuleReportLogAdapterService batteryModuleReportLogAdapterService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private ControlBatterySet controlBatterySet;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryOptCollectorCommandAdapter batteryOptCollectorCommandAdapter;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    /**
     * 蓄电池推送测试指令到终端设备
     *
     * @deprecated 旧 M460/980 测试计划配置下发链路。新计划保存不应调用本方法，
     * 后续测试控制统一迁移到 600 模块端显式命令队列。
     */
    @Deprecated
    public AjaxResult toSendCmdToOat(DevBatteryOpt opt) {
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());
        if (isUnsupportedCommandType(testEnum)) {
            return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        // 校验设备
        Config config = this.getConfig(opt);

        // 保存操作参数
        devBatteryOptService.insertDevBatteryOpt(opt);

        // 是否上报
        if (!opt.getIsSync()) {
            clientReportService.uploadBatteryOpt(opt);
        }

        // 时间格式化
        opt.setReplaceTime(DateUtils.parseDateToStr(DateUtils.YYYYMMDDHHMMSS, opt.getTestTime()));
        // 命令内容、动态指令号
        String dynCid, cmdStr;
        switch (testEnum) {
            // 内阻测试配置
            case _1:
                cmdStr = cmdBatteryControlService.getCmd32(config, opt);
                dynCid = BatteryCidEnum._E2.getDictValue();
                break;
            // 连接条电阻测试配置
            case _2:
                cmdStr = cmdBatteryControlService.getCmd33(config, opt);
                dynCid = BatteryCidEnum._E3.getDictValue();
                break;
            // 核容测试配置
            case _3:
                cmdStr = cmdBatteryControlService.getCmd34(config, opt);
                dynCid = BatteryCidEnum._E4.getDictValue();
                break;
            // 浮充管理配置
            case _4:
                cmdStr = cmdBatteryControlService.getCmd31(config, opt);
                dynCid = BatteryCidEnum._E1.getDictValue();
                break;
            // 备电时长测试配置
            case _5:
                cmdStr = cmdBatteryControlService.getCmd35(config, opt);
                dynCid = BatteryCidEnum._E5.getDictValue();
                break;
            default:
                return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池测试指令失败，指令生成失败", 0);
        }

        // 是否重复请求
        String resultKey = super.setControlStatus(config, opt.getPackNum(), dynCid, cacheKeyEnum);
        // 走 CommServer.returnCmd 直发链路，待迁移到 600 命令队列。
        CommServer.returnCmd(cmdStr);

        // 结果监控
        return super.getControlResult(resultKey, cacheKeyEnum);
    }

    /**
     * 统一执行蓄电池测试命令，页面立即执行和计划任务触发共用该入口。
     */
    public AjaxResult executeBatteryOpt(DevBatteryOpt opt, BatteryOptExecuteType executeType) {
        return toSendBatteryCmdToOat(opt);
    }

    /**
     * 立即执行蓄电池操作。
     */
    public AjaxResult toSendBatteryCmdToOat(DevBatteryOpt opt) {
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());
        if (isUnsupportedCommandType(testEnum)) {
            return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        // 校验设备
        Config config = this.getConfig(opt);

        // 校验上报数据和告警状态
        BatteryReportLog batteryReportLog = getCurrentReportLog(opt.getPackNum());
        AjaxResult validateResult = validateBeforeCommand(opt, batteryReportLog);
        if (validateResult != null) {
            return validateResult;
        }

        // 校验测试条件
        AjaxResult conditionResult = validateTestCondition(testEnum, batteryReportLog);
        if (conditionResult != null) {
            return conditionResult;
        }

        AjaxResult collectorResult = batteryOptCollectorCommandAdapter.tryExecute(opt);
        if (collectorResult != null) {
            return collectorResult;
        }

        // 生成命令
        CommandInfo cmdInfo = generateCommand(testEnum, config, opt);
        if (cmdInfo == null) {
            return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        if (StrUtil.isBlank(cmdInfo.cmdStr)) {
            return AjaxResult.error("下发蓄电池测试指令失败，指令生成失败", 0);
        }

        // 执行命令并记录日志
        return executeCommandAndLog(config, opt, testEnum, cmdInfo);
    }

    /**
     * 校验上报数据和告警状态
     */
    private AjaxResult validateBeforeCommand(DevBatteryOpt opt, BatteryReportLog batteryReportLog) {
        if (null == batteryReportLog || null == batteryReportLog.getPackParam()) {
            return AjaxResult.error("暂无上报数据", 0);
        }
        AlarmLog alarmLog = alarmLogService.getByCache(opt.getPackNum(), null, ItemCode.TXZT.getCode());
        if (null != alarmLog && ObjUtil.equals(YesNoEnum.NO.getDictValue(), alarmLog.getStatus())) {
            return AjaxResult.error(alarmLog.getDataInfo(), 0);
        }
        return null;
    }

    /**
     * 校验测试条件
     */
    private AjaxResult validateTestCondition(BatteryTestEnum testEnum, BatteryReportLog batteryReportLog) {
        if (BatteryTestEnum._2.getDictValue().equals(testEnum.getDictValue())) {
            // 连接条测试
            Double current = MapUtil.getDouble(batteryReportLog.getPackParam(), "packCurrent");
            if (current != null && Math.abs(current) < 5) {
                throw new RuntimeException("电池组未到达测试条件，需组电流超过 5A 才可以进行连接条测试");
            }
        } else {
            // 其他测试
            Map<String, Object> packParam = batteryReportLog.getPackParam();
            String batteryPackStatus = packParam != null ? Objects.toString(packParam.get("batteryPackStatus"), null) : null;
            if (!BatteryPackStatusEnum.isCode(batteryPackStatus, BatteryPackStatusEnum.IDLE)) {
                return AjaxResult.error("电池组处于非空闲状态，不允许测试！", 0);
            }
        }
        return null;
    }

    /**
     * 命令信息内部类
     */
    private static class CommandInfo {
        String cmdStr;
        String dynCid;
        boolean needWait;
        boolean needCommandLog;
        boolean needRunningLog;

        CommandInfo(String cmdStr, String dynCid, boolean needWait, boolean needCommandLog, boolean needRunningLog) {
            this.cmdStr = cmdStr;
            this.dynCid = dynCid;
            this.needWait = needWait;
            this.needCommandLog = needCommandLog;
            this.needRunningLog = needRunningLog;
        }
    }

    /**
     * 生成测试命令
     */
    private CommandInfo generateCommand(BatteryTestEnum testEnum, Config config, DevBatteryOpt opt) {
        switch (testEnum) {
            // 立即执行内阻测试
            case _1:
                BatteryModeInfo modelResult = controlBatterySet.getModelResult(opt.getPackNum());
                boolean isIdle = modelResult == null || (modelResult.getMode() == 0 && modelResult.getStatus() == 0);
                if (!isIdle) {
                    String mode = modelResult.getMode() == 1 ? "自动编号" : modelResult.getMode() == 6 ? "内阻测试" : modelResult.getMode() == 10 ? "连接条电阻测试" : "未知";
                    throw new RuntimeException("正在进行" + mode + "，请勿进行其他操作");
                }
                OptLog optLog = optLogService.lastType(opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                if (null != optLog) {
                    if (null == optLog.getUpdateTime()) {
                        throw new RuntimeException("正在内阻测试");
                    }
                    if (System.currentTimeMillis() - optLog.getUpdateTime().getTime() < 5 * 60 * 1000) {
                        throw new RuntimeException("5分钟内不允许重复测试内阻");
                    }
                }
                return new CommandInfo(
                        cmdBatteryControlService.genCmd05(config, "79", String.valueOf(opt.getPackNum())),
                        BatteryCidEnum._85.getDictValue(), false, false, false);
            // 立即执行连接条电阻测试
            case _2:
                return new CommandInfo(
                        cmdBatteryControlService.genCmd0F(config, opt),
                        BatteryCidEnum._8F.getDictValue(), true, true, false);
            // 立即执行核容测试
            case _3:
                return new CommandInfo(
                        cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "2", opt.getDischargeTime(), opt.getEndVoltage()),
                        BatteryCidEnum._E0.getDictValue(), true, false, true);
            // 立即执行备电时长测试
            case _5:
                return new CommandInfo(
                        cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "1", opt.getDischargeTime(), opt.getEndVoltage()),
                        BatteryCidEnum._E0.getDictValue(), true, false, true);
            // 单节内阻测试
            case _6:
                return new CommandInfo(
                        cmdBatteryControlService.getCmd36(config, opt),
                        BatteryCidEnum._E6.getDictValue(), true, false, false);
            default:
                return null;
        }
    }

    /**
     * 执行命令并记录日志
     */
    private AjaxResult executeCommandAndLog(Config config, DevBatteryOpt opt, BatteryTestEnum testEnum, CommandInfo cmdInfo) {
        // 是否重复请求
        String resultKey = super.setControlStatus(config, opt.getPackNum(), cmdInfo.dynCid, cacheKeyEnum);

        // 记录操作日志
        Long optLogId = null;
        if (cmdInfo.needCommandLog) {
            optLogId = optLogService.insert(opt.getPackNum(), opt.getTestType(), null);
        }

        // 走 CommServer.returnCmd 直发链路
        CommServer.returnCmd(cmdInfo.cmdStr);

        AjaxResult ajaxResult = AjaxResult.success();
        // 延迟等待设备响应
        if (cmdInfo.needWait) {
            ajaxResult = super.getControlResult(resultKey, cacheKeyEnum);
        }

        // 更新日志结果
        boolean success = Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (cmdInfo.needCommandLog && optLogId != null) {
            optLogService.update(optLogId, success ? 0 : 1, null);
        }
        if (cmdInfo.needRunningLog && success) {
            optLogService.insert(opt.getPackNum(), opt.getTestType(), null);
        }
        if (success && testEnum == BatteryTestEnum._1) {
            batteryModeStatusService.markRunning(
                    opt.getPackNum(),
                    BatteryModeStatusService.MODE_INTERNAL_RESISTANCE,
                    1);
        }
        return ajaxResult;
    }

    /**
     * 立即执行停止备电操作
     */
    public AjaxResult toSendStopBatteryCmdToOat(DevBatteryOpt opt) {
        // 校验设备
        Config config = this.getConfig(opt);

        // 停止内阻测试
        if (Objects.equals(opt.getTestType(), BatteryTestEnum._1.getDictValue())) {

            BatteryReportLog batteryReportLog = getCurrentReportLog(opt.getPackNum());

            // 无数据上报结束
            if (null == batteryReportLog || null == batteryReportLog.getPackParam()) {
                optLogService.doStopTest(opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }

            // 当前不在内阻测试状态
            String resistanceTestStatus = Objects.toString(batteryReportLog.getPackParam().get("resistanceTestStatus"), null);
            if (!ResistanceTestStatusEnum.isCode(resistanceTestStatus, ResistanceTestStatusEnum.TESTING)) {
                optLogService.doStopTest(opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }

            // 上报时间超过 3 分钟
            int diff = DateUtils.differentMillsByMillisecond(batteryReportLog.getCreateTime(), new Date());
            if (diff > 3) {
                optLogService.doStopTest(opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                return AjaxResult.success();
            }
            return AjaxResult.success();
        }

        if (Objects.equals(opt.getTestType(), BatteryTestEnum._2.getDictValue())
                || Objects.equals(opt.getTestType(), BatteryTestEnum._6.getDictValue())) {
            return batteryOptCollectorCommandAdapter.tryStop(opt);
        }

        String cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "4", 0, 0D);
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池停止备电失败，指令生成失败", 0);
        }

        // 走 CommServer.returnCmd 直发链路，待迁移到 600 命令队列。
        CommServer.returnCmd(cmdStr);
        if (Objects.equals(opt.getTestType(), BatteryTestEnum._3.getDictValue())
                || Objects.equals(opt.getTestType(), BatteryTestEnum._5.getDictValue())) {
            // 核容/备电停止命令下发后先关闭运行日志，避免手动停止后继续阻塞后续测试。
            optLogService.doStopTest(opt.getPackNum(), opt.getTestType());
        }
        return AjaxResult.success();
    }

    /**
     * 校验设备信息
     */
    private Config getConfig(DevBatteryOpt devBatteryOpt) {
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
        return config;
    }

    /**
     * 判断是否为当前控制链路不支持的测试类型。
     */
    private boolean isUnsupportedCommandType(BatteryTestEnum testEnum) {
        return testEnum == null || BatteryTestEnum._99.equals(testEnum);
    }

    /**
     * 读取测试控制前置判断使用的当前上报数据。
     * <p>
     * 标准实时切源开启时优先读取 600 实时快照适配结果，缺少组参数时回退旧上报缓存。
     */
    private BatteryReportLog getCurrentReportLog(Integer packNum) {
        if (Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpRealtimeSourceEnabled())) {
            try {
                BatteryReportLog realtimeLog = batteryModuleReportLogAdapterService.buildReportLog(packNum);
                if (realtimeLog != null && realtimeLog.getPackParam() != null && !realtimeLog.getPackParam().isEmpty()) {
                    return realtimeLog;
                }
            } catch (Exception e) {
                log.warn("读取标准实时控制判断数据失败, packNum={}", packNum, e);
            }
        }
        return batteryReportLogService.lastCache(packNum);
    }

}
