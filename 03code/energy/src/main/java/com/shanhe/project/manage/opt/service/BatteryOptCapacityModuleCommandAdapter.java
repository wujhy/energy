package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 核容/备电模块测试命令适配入口。
 * <p>旧 M460 的 _3/_5/stop 通过 0x30 转发到核容、空开、容量模块 Modbus 链路，
 * 不属于 600 单体采集命令。当前类只负责固定新链路接入点和旧链路 fallback 边界，
 * 后续接入等价模块通道时在这里替换 fallback 判断，不改 ControlBattery 主流程。
 *
 * @author wjh
 * @since 2026-07-01
 */
@Slf4j
@Service
public class BatteryOptCapacityModuleCommandAdapter {

    /**
     * 尝试执行核容/备电模块命令。
     *
     * @return 已处理时返回结果；返回 null 表示继续旧 M460/980 fallback
     */
    public AjaxResult tryExecute(DevBatteryOpt opt) {
        CapacityModuleCommand command = resolveExecuteCommand(opt);
        if (command == null) {
            return null;
        }
        log.debug("核容/备电新模块命令通道尚未启用，回退旧 M460 链路, packNum={}, testType={}, mode={}",
                opt.getPackNum(), opt.getTestType(), command.getLegacyWorkMode());
        return null;
    }

    /**
     * 尝试停止核容/备电模块命令。
     *
     * @return 已处理时返回结果；返回 null 表示继续旧 M460/980 stop fallback
     */
    public AjaxResult tryStop(DevBatteryOpt opt) {
        CapacityModuleCommand command = resolveStopCommand(opt);
        if (command == null) {
            return null;
        }
        log.debug("核容/备电停止新模块命令通道尚未启用，回退旧 M460 链路, packNum={}, testType={}, mode={}",
                opt.getPackNum(), opt.getTestType(), command.getLegacyWorkMode());
        return null;
    }

    private CapacityModuleCommand resolveExecuteCommand(DevBatteryOpt opt) {
        if (opt == null || opt.getTestType() == null) {
            return null;
        }
        if (BatteryTestEnum._3.getDictValue().equals(opt.getTestType())) {
            return CapacityModuleCommand.CAPACITY_TEST;
        }
        if (BatteryTestEnum._5.getDictValue().equals(opt.getTestType())) {
            return CapacityModuleCommand.BACKUP_POWER_TEST;
        }
        return null;
    }

    private CapacityModuleCommand resolveStopCommand(DevBatteryOpt opt) {
        return resolveExecuteCommand(opt) == null ? null : CapacityModuleCommand.STOP;
    }

    /** 核容/备电模块旧 0x30 工作模式映射。 */
    @Getter
    private enum CapacityModuleCommand {
        /** 核容测试，旧 0x30 mode=2。 */
        CAPACITY_TEST("2"),
        /** 备电时长测试，旧 0x30 mode=1。 */
        BACKUP_POWER_TEST("1"),
        /** 停止核容/备电，旧 0x30 mode=4。 */
        STOP("4");

        private final String legacyWorkMode;

        CapacityModuleCommand(String legacyWorkMode) {
            this.legacyWorkMode = legacyWorkMode;
        }
    }
}
