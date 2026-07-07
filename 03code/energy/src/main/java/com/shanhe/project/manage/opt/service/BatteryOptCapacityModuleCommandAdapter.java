package com.shanhe.project.manage.opt.service;

import cn.hutool.core.util.StrUtil;
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
 * `_5` 备电时长外部模块控制适配边界。
 * <p>
 * `_5` 不进入 600 单体采集命令队列；这里只负责外部备电模块启停控制。
 * 放电容量、预估容量等结果继续由采集后处理链路根据上报数据计算。
 * `_3` 核容暂不在本类扩展，继续保留旧链路兼容。
 *
 * @author wjh
 * @since 2026-07-01
 */
@Slf4j
@Service
public class BatteryOptCapacityModuleCommandAdapter extends ControlBase {

    private static final String BACKUP_START_MODE = "1";
    private static final String BACKUP_STOP_MODE = "4";

    /** 旧 M460/980 核容模块 0x30 指令生成器，当前仅用于备电外部模块启停。 */
    @Resource
    private CmdBatteryControlService cmdBatteryControlService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;

    /** 兼容旧调用方；没有完整上下文时不在适配器内直接执行。 */
    public AjaxResult tryExecute(DevBatteryOpt opt) {
        if (isBackupTest(opt)) {
            log.debug("备电开始命令缺少执行上下文，保留调用方边界处理, packNum={}, testType={}",
                    opt.getPackNum(), opt.getTestType());
        }
        return null;
    }

    /** `_5` 备电时长开始入口：下发外部模块 0x30 mode=1，并等待 E0 回执。 */
    public AjaxResult tryExecute(BatteryCommandContext context) {
        DevBatteryOpt opt = context == null ? null : context.opt;
        if (!isBackupTest(opt)) {
            return null;
        }

        String cmdStr = cmdBatteryControlService.genCmd30(
                context.config,
                opt.getPackNum(),
                BACKUP_START_MODE,
                opt.getDischargeTime(),
                opt.getEndVoltage());
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池备电开始失败，指令生成失败", 0);
        }

        String resultKey = super.setControlStatus(
                context.config,
                opt.getPackNum(),
                BatteryCidEnum._E0.getDictValue(),
                CacheKeyEnum.RESULT);
        CommServer.returnCmd(cmdStr);

        AjaxResult result = super.getControlResult(resultKey, CacheKeyEnum.RESULT);
        boolean success = Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
        if (success) {
            optLogService.insert(opt.getPackNum(), opt.getTestType(), null, context.optLogSource);
        }
        return result;
    }

    /** 兼容旧调用方；没有完整上下文时不在适配器内直接执行。 */
    public AjaxResult tryStop(DevBatteryOpt opt) {
        if (isBackupTest(opt)) {
            log.debug("备电停止命令缺少执行上下文，保留调用方边界处理, packNum={}, testType={}",
                    opt.getPackNum(), opt.getTestType());
        }
        return null;
    }

    /** `_5` 备电时长停止入口：下发外部模块 0x30 mode=4，并关闭运行日志。 */
    public AjaxResult tryStop(BatteryCommandContext context) {
        DevBatteryOpt opt = context == null ? null : context.opt;
        if (!isBackupTest(opt)) {
            return null;
        }

        String cmdStr = cmdBatteryControlService.genCmd30(
                context.config,
                opt.getPackNum(),
                BACKUP_STOP_MODE,
                0,
                0D);
        if (StrUtil.isBlank(cmdStr)) {
            return AjaxResult.error("下发蓄电池备电停止失败，指令生成失败", 0);
        }

        CommServer.returnCmd(cmdStr);
        optLogService.doStopTest(opt.getPackNum(), opt.getTestType());
        return AjaxResult.success();
    }

    private boolean isBackupTest(DevBatteryOpt opt) {
        return opt != null && BatteryTestEnum._5.getDictValue().equals(opt.getTestType());
    }
}
