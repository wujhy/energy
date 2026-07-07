package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.opt.domain.BatteryCommandContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 蓄电池测试计划采集命令适配服务。
 * <p>入口负责业务校验和数据准备，本类只按已准备上下文分流到 600 采集命令。</p>
 *
 * @author wjh
 * @since 2026-06-22
 */
@Slf4j
@Service
public class BatteryOptCollectorCommandAdapter {

    /** 采集配置属性。 */
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;

    /** 采集命令服务。 */
    @Resource
    private BatteryCollectorCommandService batteryCollectorCommandService;

    /**
     * 尝试将测试计划转为独立采集模块命令执行。
     *
     * @param context 入口已准备的测试上下文
     * @return 命令已处理时返回结果；无法处理时返回 null，由旧链路兜底
     */
    public AjaxResult tryExecutePrepared(BatteryCommandContext context) {
        if (resolveMode(context.opt.getTestType()) == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpModuleCommandEnabled())) {
            return AjaxResult.error("独立采集模块命令未启用，已迁移测试类型禁止回退旧 M460 指令", 0);
        }
        BatteryCollectorCommandResult result;
        try {
            result = executeCollectorCommand(context);
        } catch (Exception e) {
            log.warn("采集模块命令适配失败, packNum={}, testType={}, 原因={}",
                    context.opt.getPackNum(), context.opt.getTestType(), e.getMessage());
            return AjaxResult.error("独立采集模块命令执行失败", 0);
        }

        if (result != null && result.isSuccess()) {
            return AjaxResult.success("独立采集模块命令已加入下发队列", result);
        }
        return AjaxResult.error(result == null ? "独立采集模块命令执行失败" : result.getMessage(), 0);
    }

    private BatteryCollectorCommandResult executeCollectorCommand(BatteryCommandContext context) {
        Integer packNum = context.opt.getPackNum();
        String channelName = batteryCollectorCommandService.resolveChannelName(packNum);
        if (BatteryTestEnum._1.equals(context.testEnum)) {
            return batteryCollectorCommandService.groupInternalResistanceTest(
                    channelName, packNum, context.batteryCount, null);
        }
        if (BatteryTestEnum._2.equals(context.testEnum)) {
            return batteryCollectorCommandService.connectResistanceTest(
                    channelName, packNum, context.batteryCount, null);
        }
        if (BatteryTestEnum._6.equals(context.testEnum)) {
            return batteryCollectorCommandService.singleInternalResistanceTest(
                    channelName, packNum, context.opt.getModelNum(), null);
        }
        return null;
    }

    /**
     * 尝试停止已接入 600 队列的测试命令。
     *
     * @param opt 停止参数
     * @return 已处理时返回结果；无法处理时返回 null
     */
    public AjaxResult tryStop(DevBatteryOpt opt) {
        if (opt == null || opt.getTestType() == null || opt.getPackNum() == null) {
            return null;
        }
        Integer mode = resolveMode(opt.getTestType());
        if (mode == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpModuleCommandEnabled())) {
            return AjaxResult.error("独立采集模块命令未启用，已迁移测试类型禁止回退旧 M460 指令", 0);
        }
        BatteryCollectorCommandResult result = batteryCollectorCommandService.stopRunningTest(
                opt.getPackNum(), mode, opt.getTestType());
        if (result != null && result.isSuccess()) {
            return AjaxResult.success(result.getMessage(), result);
        }
        return AjaxResult.error(result == null ? "停止测试失败" : result.getMessage(), 0);
    }

    /** 将测试类型映射为 600 采集侧工作模式。 */
    private Integer resolveMode(Integer testType) {
        if (BatteryTestEnum._1.getDictValue().equals(testType)
                || BatteryTestEnum._6.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_INTERNAL_RESISTANCE;
        }
        if (BatteryTestEnum._2.getDictValue().equals(testType)) {
            return BatteryModeStatusService.MODE_CONNECT_RESISTANCE;
        }
        return null;
    }
}
