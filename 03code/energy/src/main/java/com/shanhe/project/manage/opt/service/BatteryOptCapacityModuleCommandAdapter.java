package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 核容/备电模块测试命令适配入口。
 * <p>旧 M460 的 _3/_5/stop 通过 0x30 转发到核容、空开、容量模块 Modbus 链路，
 * 不属于 600 单体采集命令。当前尚未接入等价的新模块命令通道，因此只保留稳定入口并回退旧链路。
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
        if (!isCapacityModuleTest(opt)) {
            return null;
        }
        log.debug("核容/备电新模块命令通道尚未启用，回退旧 M460 链路, packNum={}, testType={}",
                opt.getPackNum(), opt.getTestType());
        return null;
    }

    /**
     * 尝试停止核容/备电模块命令。
     *
     * @return 已处理时返回结果；返回 null 表示继续旧 M460/980 stop fallback
     */
    public AjaxResult tryStop(DevBatteryOpt opt) {
        if (!isCapacityModuleTest(opt)) {
            return null;
        }
        log.debug("核容/备电停止新模块命令通道尚未启用，回退旧 M460 链路, packNum={}, testType={}",
                opt.getPackNum(), opt.getTestType());
        return null;
    }

    private boolean isCapacityModuleTest(DevBatteryOpt opt) {
        if (opt == null || opt.getTestType() == null) {
            return false;
        }
        return BatteryTestEnum._3.getDictValue().equals(opt.getTestType())
                || BatteryTestEnum._5.getDictValue().equals(opt.getTestType());
    }
}