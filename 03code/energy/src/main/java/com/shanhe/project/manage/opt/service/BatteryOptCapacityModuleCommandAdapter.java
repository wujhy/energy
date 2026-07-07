package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.cmd.CmdBatteryControlService;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 核容/备电模块控制适配边界。
 * <p>
 * `_5` 不进入 600 单体采集命令队列，只负责外部备电模块启停控制。
 * `_3` 暂按 M460 底层实现兼容：`0x30 mode=2/4` 切换核容/空闲模式。
 * 放电容量、预估容量等结果继续由采集后处理链路根据上报数据计算。
 *
 * @author wjh
 * @since 2026-07-01
 */
@Slf4j
@Service
public class BatteryOptCapacityModuleCommandAdapter extends ControlBase {

    /** 外部备电模块 energy 直控服务。 */
    @Resource
    private BackupExternalModuleControlService backupExternalModuleControlService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** M460 核容模式命令生成器，仅用于 `_3` 兼容边界。 */
    @Resource
    private CmdBatteryControlService cmdBatteryControlService;

    private final CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    /** 兼容旧调用方；没有完整上下文时不在适配器内直接执行。 */
    public AjaxResult tryExecute(DevBatteryOpt opt) {
        if (isBackupTest(opt) || isCapacityTest(opt)) {
            log.debug("核容/备电开始命令缺少执行上下文，保留调用方边界处理, packNum={}, testType={}",
                    opt.getPackNum(), opt.getTestType());
        }
        return null;
    }

    /** `_3/_5` 开始入口。 */
    public AjaxResult tryExecute(BatteryCommandContext context) {
        DevBatteryOpt opt = context == null ? null : context.opt;
        if (isCapacityTest(opt)) {
            return executeCapacityTest(context);
        }
        if (!isBackupTest(opt)) {
            return null;
        }

        AjaxResult result = backupExternalModuleControlService.startBackup(opt.getPackNum());
        boolean success = Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (success) {
            optLogService.insert(opt.getPackNum(), opt.getTestType(), null, context.optLogSource);
        }
        return result;
    }

    /** 兼容旧调用方；没有完整上下文时不在适配器内直接执行。 */
    public AjaxResult tryStop(DevBatteryOpt opt) {
        if (isBackupTest(opt) || isCapacityTest(opt)) {
            log.debug("核容/备电停止命令缺少执行上下文，保留调用方边界处理, packNum={}, testType={}",
                    opt.getPackNum(), opt.getTestType());
        }
        return null;
    }

    /** `_3/_5` 停止入口。 */
    public AjaxResult tryStop(BatteryCommandContext context) {
        DevBatteryOpt opt = context == null ? null : context.opt;
        if (isCapacityTest(opt)) {
            return stopCapacityTest(context);
        }
        if (!isBackupTest(opt)) {
            return null;
        }

        AjaxResult result = backupExternalModuleControlService.stopBackup(opt.getPackNum());
        boolean success = Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (success) {
            optLogService.doStopTest(opt.getPackNum(), opt.getTestType());
        }
        return result;
    }

    private boolean isBackupTest(DevBatteryOpt opt) {
        return opt != null && BatteryTestEnum._5.getDictValue().equals(opt.getTestType());
    }

    private boolean isCapacityTest(DevBatteryOpt opt) {
        return opt != null && BatteryTestEnum._3.getDictValue().equals(opt.getTestType());
    }

    /** `_3` 核容兼容入口：参考 M460 `0x30 mode=2` 底层核容模式实现。 */
    private AjaxResult executeCapacityTest(BatteryCommandContext context) {
        DevBatteryOpt opt = context.opt;
        String cmdStr = cmdBatteryControlService.genCmd30(
                context.config, opt.getPackNum(), "2", opt.getDischargeTime(), opt.getEndVoltage());
        if (cmdStr == null || cmdStr.trim().isEmpty()) {
            return AjaxResult.error("下发蓄电池核容测试失败，指令生成失败", 0);
        }
        String resultKey = super.setControlStatus(
                context.config, opt.getPackNum(), BatteryCidEnum._E0.getDictValue(), cacheKeyEnum);
        CommServer.returnCmd(cmdStr);
        AjaxResult result = super.getControlResult(resultKey, cacheKeyEnum);
        boolean success = Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (success) {
            optLogService.insert(opt.getPackNum(), opt.getTestType(), null, context.optLogSource);
        }
        return result;
    }

    /** `_3` 核容停止兼容入口：参考 M460 `0x30 mode=4` 底层空闲模式实现。 */
    private AjaxResult stopCapacityTest(BatteryCommandContext context) {
        DevBatteryOpt opt = context.opt;
        String cmdStr = cmdBatteryControlService.genCmd30(context.config, opt.getPackNum(), "4", 0, 0D);
        if (cmdStr == null || cmdStr.trim().isEmpty()) {
            return AjaxResult.error("下发蓄电池核容停止失败，指令生成失败", 0);
        }
        CommServer.returnCmd(cmdStr);
        optLogService.doStopTest(opt.getPackNum(), opt.getTestType());
        return AjaxResult.success();
    }
}
