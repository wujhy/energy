package com.shanhe.project.manage.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import com.shanhe.project.manage.config.domain.DevBatteryOpt;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 蓄电池测试计划采集命令适配服务。
 * <p>该适配器只接入已确认等价的 600 采集模块命令。`_1` 整组内阻当前只有稳定插入点，
 * 在 600 显式整组状态机未就绪前必须回退旧 M460 链路；不得降级为循环调用 `_6`。</p>
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
     * @return 命令已处理时返回结果；无法处理时返回 null，由旧链路兜底
     */
    public AjaxResult tryExecute(DevBatteryOpt opt) {
        CollectorCommandContext context = resolveContext(opt);
        if (context == null) {
            return null;
        }
        AjaxResult runningResult = rejectWhenCollectorModeRunning(context.packNum, context.mode);
        if (runningResult != null) {
            return runningResult;
        }
        if (context.channelName == null || context.channelName.isEmpty()) {
            return AjaxResult.error("未找到电池组采集通道", 0);
        }

        BatteryCollectorCommandResult result;
        try {
            result = executeCollectorCommand(context);
            if (isGroupInternalResistance(context) && shouldFallbackLegacyM460(result)) {
                log.debug("group internal-resistance is not mapped to 600 command, fallback legacy M460, packNum={}",
                        context.packNum);
                return null;
            }
        } catch (Exception e) {
            log.warn("collector command adapter failed, packNum={}, testType={}, reason={}",
                    context.packNum, context.testType, e.getMessage());
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
        BatteryCollectorCommandResult result = batteryCollectorCommandService.stopRunningTest(opt.getPackNum(), mode, opt.getTestType());
        if (result != null && result.isSuccess()) {
            return AjaxResult.success(result.getMessage(), result);
        }
        return AjaxResult.error(result == null ? "停止测试失败" : result.getMessage(), 0);
    }

    private BatteryCollectorCommandResult executeCollectorCommand(CollectorCommandContext context) {
        if (BatteryTestEnum._1.getDictValue().equals(context.testType)) {
            int batteryCount = resolveBatteryCount(context.packNum);
            return batteryCollectorCommandService.groupInternalResistanceTest(
                    context.channelName, context.packNum, batteryCount, null);
        }
        if (BatteryTestEnum._2.getDictValue().equals(context.testType)) {
            int batteryCount = resolveBatteryCount(context.packNum);
            return batteryCollectorCommandService.connectResistanceTest(
                    context.channelName, context.packNum, batteryCount, null);
        }
        if (BatteryTestEnum._6.getDictValue().equals(context.testType)) {
            Integer modelNum = context.opt.getModelNum();
            int batteryCount = resolveBatteryCount(context.packNum);
            if (modelNum == null || modelNum < 1 || modelNum > batteryCount) {
                return BatteryCollectorCommandResult.builder()
                        .success(false)
                        .message("单节内阻测试单体编号无效")
                        .build();
            }
            return batteryCollectorCommandService.singleInternalResistanceTest(
                    context.channelName, context.packNum, modelNum, null);
        }
        return null;
    }

    private CollectorCommandContext resolveContext(DevBatteryOpt opt) {
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
        String channelName = batteryCollectorCommandService.resolveChannelName(opt.getPackNum());
        return new CollectorCommandContext(opt, opt.getTestType(), opt.getPackNum(), mode, channelName);
    }

    private boolean isGroupInternalResistance(CollectorCommandContext context) {
        return BatteryTestEnum._1.getDictValue().equals(context.testType);
    }

    /** `_1` 整组内阻未映射为 600 模块命令时，必须继续旧 M460 状态机。 */
    private boolean shouldFallbackLegacyM460(BatteryCollectorCommandResult result) {
        return result != null && !result.isMappedToModuleCommand();
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

    /** 当前电池组已有 600 采集测试运行时拒绝重复入队。 */
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
            log.debug("获取电池组单体数失败, packNum={}, reason={}", packNum, e.getMessage());
        }
        return MAX_BATTERY_COUNT;
    }

    private static class CollectorCommandContext {
        private final DevBatteryOpt opt;
        private final Integer testType;
        private final Integer packNum;
        private final Integer mode;
        private final String channelName;

        private CollectorCommandContext(DevBatteryOpt opt,
                                        Integer testType,
                                        Integer packNum,
                                        Integer mode,
                                        String channelName) {
            this.opt = opt;
            this.testType = testType;
            this.packNum = packNum;
            this.mode = mode;
            this.channelName = channelName;
        }
    }
}
