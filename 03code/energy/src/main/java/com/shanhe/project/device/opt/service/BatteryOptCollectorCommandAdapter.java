package com.shanhe.project.device.opt.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandService;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.framework.web.domain.AjaxResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 蓄电池测试计划采集命令适配服务。
 *
 * <p>为后续 {@code /batteryOpt/doCmdOptBatteryTest} 切换 {@code _2/_6} 做准备。
 * 当独立采集命令开关开启且能找到通道时，优先走采集命令队列；否则返回 null 让旧链路兜底。</p>
 *
 * @author wjh
 * @since 2026-06-22
 */
@Slf4j
@Service
public class BatteryOptCollectorCommandAdapter {

    private static final int MAX_BATTERY_COUNT = 245;

    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;

    @Resource
    private BatteryCollectorCommandService batteryCollectorCommandService;

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
        String channelName = batteryCollectorCommandService.resolveChannelName(opt.getPackNum());
        if (channelName == null || channelName.isEmpty()) {
            return null;
        }

        BatteryCollectorCommandResult result = null;
        try {
            if (BatteryTestEnum._2.getDictValue().equals(opt.getTestType())) {
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
            return null;
        }

        if (result != null && result.isSuccess()) {
            return AjaxResult.success("独立采集模块命令已加入下发队列", result);
        }
        return null;
    }

    /**
     * 解析电池组单体数量，异常或空值时使用默认值 245。
     */
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
