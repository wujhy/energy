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
 * 设备控制类
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
            case _1:  //内阻测试配置
                cmdStr = cmdBatteryControlService.getCmd32(config, opt);
                dynCid = BatteryCidEnum._E2.getDictValue();
                break;
            case _2:   //连接条电阻测试配置
                cmdStr = cmdBatteryControlService.getCmd33(config, opt);
                dynCid = BatteryCidEnum._E3.getDictValue();
                break;
            case _3://核容测试配置
                cmdStr = cmdBatteryControlService.getCmd34(config, opt);
                dynCid = BatteryCidEnum._E4.getDictValue();
                break;
            case _4:  //浮充管理配置
                cmdStr = cmdBatteryControlService.getCmd31(config, opt);
                dynCid = BatteryCidEnum._E1.getDictValue();
                break;
            case _5:  //备电时长测试配置
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
        // 旧 CommServer.returnCmd 直发链路，待迁移为 600 命令队列。
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
     * 立即执行蓄电池操作
     */
    public AjaxResult toSendBatteryCmdToOat(DevBatteryOpt opt) {
        BatteryTestEnum testEnum = BatteryTestEnum.find(opt.getTestType());
        if (isUnsupportedCommandType(testEnum)) {
            return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        // 校验设备
        Config config = this.getConfig(opt);

        BatteryReportLog batteryReportLog = getCurrentReportLog(opt.getPackNum());
        if (null == batteryReportLog) {
            return AjaxResult.error("暂无上报数据", 0);
        }
        if (null == batteryReportLog.getPackParam()) {
            return AjaxResult.error("暂无上报数据", 0);
        }
        AlarmLog alarmLog = alarmLogService.getByCache(opt.getPackNum(), null, ItemCode.TXZT.getCode());
        if (null != alarmLog) {
            if (ObjUtil.equals(YesNoEnum.NO.getDictValue(), alarmLog.getStatus())) {
                return AjaxResult.error(alarmLog.getDataInfo(), 0);
            }
        }

        //连接条测试
        if (BatteryTestEnum._2.getDictValue().equals(testEnum.getDictValue())) {

            Double current = MapUtil.getDouble(batteryReportLog.getPackParam(), "packCurrent");
            //电池组充放电电流
            if (current != null && Math.abs(current) < 5) {
                throw new RuntimeException("电池组未到达测试条件，需组电流超过±5A才可以进行连接条测试！");
            }
        } else {

            Map<String, Object> packParam = batteryReportLog.getPackParam();
            String batteryPackStatus = packParam != null ? Objects.toString(packParam.get("batteryPackStatus"), null) : null;
            if (!BatteryPackStatusEnum.isCode(batteryPackStatus, BatteryPackStatusEnum.IDLE)) {
                return AjaxResult.error("电池组处于非空闲状态，不允许测试！", 0);
            }

        }

        AjaxResult collectorResult = batteryOptCollectorCommandAdapter.tryExecute(opt);
        if (collectorResult != null) {
            return collectorResult;
        }

        // 默认需要等待执行结果；部分命令需要记录一次性响应日志或长任务运行日志
        boolean needWait = true, needCommandLog = false, needRunningLog = false;
        // 命令内容、动态指令号
        String cmdStr, dynCid;
        switch (testEnum) {
            case _1:  //立即执行内阻测试
                BatteryModeInfo modelResult = controlBatterySet.getModelResult(opt.getPackNum());
                if (modelResult != null) {
                    if (modelResult.getMode() == 0 && modelResult.getStatus() == 0) {
                    } else {
                        // 当前模式 0： 无测试 1： 自动编号 6： 内阻测试 10：连接条电阻测试 */
                        String mode = modelResult.getMode() == 1 ? "自动编号" : modelResult.getMode() == 6 ? "内阻测试" : modelResult.getMode() == 10 ? "连接条电阻测试" : "无";
                        return AjaxResult.error("正在进行" + mode + "，请勿进行其他操作");
                    }
                }

                OptLog optLog = optLogService.lastType(opt.getPackNum(), BatteryTestEnum._1.getDictValue());
                // 5 分钟内不允许测试
                if (null != optLog) {
                    if (null == optLog.getUpdateTime()) {
                        return AjaxResult.error("正在内阻测试中", 0);
                    }
                    if (System.currentTimeMillis() - optLog.getUpdateTime().getTime() < 5 * 60 * 1000) {
                        return AjaxResult.error("5分钟内不允许重复测试内阻", 0);
                    }
                }
                cmdStr = cmdBatteryControlService.genCmd05(config, "79", String.valueOf(opt.getPackNum()));
                dynCid = BatteryCidEnum._85.getDictValue();
                needWait = false;
                break;
            case _2:   //立即执行连接条电阻测试
                cmdStr = cmdBatteryControlService.genCmd0F(config, opt);
                dynCid = BatteryCidEnum._8F.getDictValue();
                needCommandLog = true;
                break;
            case _3:  //立即执行核容测试
                cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "2", opt.getDischargeTime(), opt.getEndVoltage());
                dynCid = BatteryCidEnum._E0.getDictValue();
                needRunningLog = true;
                break;
            case _5:  //立即执行备电时长测试
                cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "1", opt.getDischargeTime(), opt.getEndVoltage());
                dynCid = BatteryCidEnum._E0.getDictValue();
                needRunningLog = true;
                break;
            case _6:  //单节内阻测试
                cmdStr = cmdBatteryControlService.getCmd36(config, opt);
                dynCid = BatteryCidEnum._E6.getDictValue();
                break;
            default:
                return AjaxResult.error("下发蓄电池测试指令类型失败", 0);
        }
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池测试指令失败，指令生成失败", 0);
        }

        // 是否重复请求
        String resultKey = super.setControlStatus(config, opt.getPackNum(), dynCid, cacheKeyEnum);
        // 记录操作日志
        Long optLogId = null;
        if (needCommandLog) {
            optLogId = optLogService.insert(opt.getPackNum(), opt.getTestType(), null);
        }

        // 旧 CommServer.returnCmd 直发链路，待迁移为 600 命令队列。
        CommServer.returnCmd(cmdStr);

        AjaxResult ajaxResult = AjaxResult.success();
        //延迟等待设备响应
        if (needWait) {
            ajaxResult = super.getControlResult(resultKey, cacheKeyEnum);
        }
        // 更新日志结果
        boolean success = Objects.equals(ajaxResult.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (needCommandLog && optLogId != null) {
            optLogService.update(optLogId, success ? 0 : 1, null);
        }
        if (needRunningLog && success) {
            // 核容/备电是长任务，成功启动后先落运行日志，后续实时状态负责关闭。
            optLogService.insert(opt.getPackNum(), opt.getTestType(), null);
        }
        if (success && testEnum == BatteryTestEnum._1) {
            // 内阻测试不等待设备回包，成功下发后先标记运行态，后续实时上报负责刷新测试结果。
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
            return AjaxResult.error("当前测试类型暂不支持停止命令", 0);
        }

        String cmdStr = cmdBatteryControlService.genCmd30(config, opt.getPackNum(), "4", 0, 0D);
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池停止备电失败，指令生成失败", 0);
        }

        // 旧 CommServer.returnCmd 直发链路，待迁移为 600 命令队列。
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
            throw new ServiceException("设备不存在，操作执行失败！");
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
