package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.iot.model.BatteryModeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 蓄电池测试计划采集命令适配服务。
 *
 * <p>为后续 {@code /batteryOpt/doCmdOptBatteryTest} 切换 {@code _2/_6} 做准备。
 * 当独立采集命令开关开启且能找到通道时，优先走采集命令队列。</p>
 *
 * @author wjh
 * @since 2026-06-22
 */
@Slf4j
@Service
public class BatteryOptCollectorCommandAdapter {

    /** 电池单体数量上限默认值。 */
    private static final int MAX_BATTERY_COUNT = 245;

    /** 采集配置属性。 */
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;

    /** 采集命令服务。 */
    @Resource
    private BatteryCollectorCommandService batteryCollectorCommandService;

    /** 工作模式状态服务。 */
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;

    /**
     * 尝试将测试计划转为独立采集模块命令执行。
     *
     * @param opt 测试计划参数
     * @return 命令已入队时返回成功结果；无法处理时返回 null，由旧链路兜底
     */
    public AjaxResult tryExecute(DevBatteryOpt opt) {
        if (opt == null || opt.getTestType() == null || opt.getPackNum() == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpModuleCommandEnabled())) {
            return null;
        }
        Integer mode = resolveMode(opt.getTestType());
        if (mode == null) {
            return null;
        }
        AjaxResult runningResult = rejectWhenCollectorModeRunning(opt.getPackNum(), mode);
        if (runningResult != null) {
            return runningResult;
        }
        String channelName = batteryCollectorCommandService.resolveChannelName(opt.getPackNum());
        if (channelName == null || channelName.isEmpty()) {
            return AjaxResult.error("未找到电池组采集通道", 0);
        }

        BatteryCollectorCommandResult result;
        try {
            if (BatteryTestEnum._1.getDictValue().equals(opt.getTestType())) {
                int batteryCount = resolveBatteryCount(opt.getPackNum());
                result = batteryCollectorCommandService.groupInternalResistanceTest(
                        channelName, opt.getPackNum(), batteryCount, null);
                if (isGroupInternalResistanceNotReady(result)) {
                    return null;
                }
            } else if (BatteryTestEnum._2.getDictValue().equals(opt.getTestType())) {
                int batteryCount = resolveBatteryCount(opt.getPackNum());
                result = batteryCollectorCommandService.connectResistanceTest(
                        channelName, opt.getPackNum(), batteryCount, null);
            } else if (BatteryTestEnum._6.getDictValue().equals(opt.getTestType())) {
                Integer modelNum = opt.getModelNum();
                int batteryCount = resolveBatteryCount(opt.getPackNum());
                if (modelNum == null || modelNum < 1 || modelNum > batteryCount) {
                    return AjaxResult.error("单节内阻测试单体编号无效", 0);
                }
                result = batteryCollectorCommandService.singleInternalResistanceTest(
                        channelName, opt.getPackNum(), modelNum, null);
            } else {
                return null;
            }
        } catch (Exception e) {
            log.warn("采集命令适配异常, packNum={}, testType={}, 原因={}",
                    opt.getPackNum(), opt.getTestType(), e.getMessage());
            return AjaxResult.error("独立采集模块命令执行失败", 0);
        }

        if (result != null && result.isSuccess()) {
            return AjaxResult.success("独立采集模块命令已加入下发队列", result);
        }
        return AjaxResult.error(result == null ? "独立采集模块命令执行失败" : result.getMessage(), 0);
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
        if (!Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpModuleCommandEnabled())) {
            return null;
        }
        Integer mode = resolveMode(opt.getTestType());
        if (mode == null) {
            return null;
        }
        BatteryCollectorCommandResult result = batteryCollectorCommandService.stopRunningTest(opt.getPackNum(), mode);
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

    /** 整组内阻显式状态机未就绪时回退旧 M460 链路。 */
    private boolean isGroupInternalResistanceNotReady(BatteryCollectorCommandResult result) {
        return result != null
                && !result.isSuccess()
                && result.getMessage() != null
                && result.getMessage().contains("整组内阻测试尚未实现");
    }

    /** 当前电池组已有600采集测试运行时拒绝重复入队。 */
    private AjaxResult rejectWhenCollectorModeRunning(Integer packNum, Integer expectedMode) {
        if (batteryModeStatusService == null || packNum == null || expectedMode == null) {
            return null;
        }
        BatteryModeInfo modeInfo = batteryModeStatusService.get(packNum);
        if (modeInfo == null
                || !Objects.equals(modeInfo.getPackNum(), packNum)
                || !Objects.equals(modeInfo.getStatus(), 1)) {
            return null;
        }
        if (Objects.equals(modeInfo.getMode(), expectedMode)) {
            return AjaxResult.error("当前电池组已有同类型测试运行中", 0);
        }
        return AjaxResult.error("当前电池组有其他测试运行中", 0);
    }

    /** 解析电池组单体数量，异常或空值时使用默认值 245。 */
    private int resolveBatteryCount(Integer packNum) {
        try {
            Integer count = batteryPackService.getBatteryMaxNumber(packNum);
            if (count != null && count > 0) {
                return Math.min(count, MAX_BATTERY_COUNT);
            }
        } catch (Exception e) {
            log.debug("获取电池组单体数失败, packNum={}, 原因={}", packNum, e.getMessage());
        }
        return MAX_BATTERY_COUNT;
    }
}
