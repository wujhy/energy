package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 核容/备电模块控制适配边界。
 * <p>
 * `_5` 不进入 600 单体采集命令队列，只负责外部备电模块启停控制。
 * `_3` 仅在本适配器占位；对应 M460 底层能力是 `0x30 mode=2/4` 切换核容/空闲模式，当前不做兼容下发。
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
            log.debug("核容测试 `_3` 已收进容量模块适配器占位；M460 对应底层能力为 0x30 mode=2，当前不做兼容下发, packNum={}",
                    opt.getPackNum());
            return null;
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
            log.debug("核容测试 `_3` 已收进容量模块适配器占位；M460 对应底层能力为 0x30 mode=4，当前不做兼容下发, packNum={}",
                    opt.getPackNum());
            return null;
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
}
