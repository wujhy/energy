package com.shanhe.project.scheduled;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import com.shanhe.project.device.config.domain.DevBatteryOpt;
import com.shanhe.project.device.config.service.IDevBatteryOptService;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.service.BatteryOptExecuteType;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.OptLogService;
import com.shanhe.project.iot.model.BatteryModeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 蓄电池测试计划到点调度任务。
 *
 * <p>每分钟扫描已启用的测试计划，到点后自动执行；
 * 执行前检查是否已有运行中测试，避免重复触发。
 * 执行成功后更新下次调度时间或禁用一次性计划。</p>
 *
 * @author wjh
 * @since 2026-06-22
 */
@Slf4j
@Component
@EnableScheduling
public class BatteryOptScheduleJob {

    private static final long MIN_INTERVAL_MILLIS = 60_000L;

    @Resource
    private IDevBatteryOptService devBatteryOptService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private OptLogService optLogService;
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

    private final Set<String> runningKeys = ConcurrentHashMap.newKeySet();

    /**
     * 扫描并执行到期的蓄电池测试计划。
     */
    @Scheduled(cron = "${job.batteryOptSchedule:0 0/1 * * * ?}")
    public void executeDueBatteryOpt() {
        DevBatteryOpt query = new DevBatteryOpt();
        query.setIsEnabled(YesNoEnum.YES.getDictValue());
        List<DevBatteryOpt> optList = devBatteryOptService.selectDevBatteryOptList(query);
        if (optList == null || optList.isEmpty()) {
            return;
        }

        Date now = new Date();
        for (DevBatteryOpt opt : optList) {
            if (!isDue(opt, now)) {
                continue;
            }
            executeOne(opt, now);
        }
    }

    /** 判断测试计划是否已到执行时间。 */
    private boolean isDue(DevBatteryOpt opt, Date now) {
        return opt != null
                && opt.getPackNum() != null
                && opt.getTestType() != null
                && opt.getTestTime() != null
                && !opt.getTestTime().after(now);
    }

    /** 执行单个测试计划，使用 runningKeys 防止重复触发。 */
    private void executeOne(DevBatteryOpt opt, Date now) {
        String key = opt.getPackNum() + ":" + opt.getTestType();
        if (!runningKeys.add(key)) {
            return;
        }
        try {
            if (hasRunningOptLog(opt)) {
                log.info("蓄电池测试计划到点执行跳过，已有测试运行中, packNum={}, testType={}",
                        opt.getPackNum(), opt.getTestType());
                return;
            }
            AjaxResult result = controlBattery.executeBatteryOpt(opt, BatteryOptExecuteType.SCHEDULED);
            if (isSuccess(result)) {
                updateNextSchedule(opt, now);
            } else {
                log.warn("蓄电池测试计划到点执行失败, packNum={}, testType={}, result={}",
                        opt.getPackNum(), opt.getTestType(), result);
            }
        } catch (Exception e) {
            log.warn("蓄电池测试计划到点执行异常, packNum={}, testType={}, 原因={}",
                    opt.getPackNum(), opt.getTestType(), e.getMessage());
        } finally {
            runningKeys.remove(key);
        }
    }

    /** 检查是否已有运行中的测试（opt_log 或采集模块工作模式）。 */
    private boolean hasRunningOptLog(DevBatteryOpt opt) {
        OptLog running = optLogService.getRunningOptLog(opt.getPackNum(), opt.getTestType());
        if (running != null) {
            return true;
        }
        Integer expectedMode = resolveCollectorMode(opt.getTestType());
        if (expectedMode == null || batteryModeStatusService == null) {
            return false;
        }
        BatteryModeInfo modeInfo = batteryModeStatusService.get(opt.getPackNum());
        return modeInfo != null
                && Objects.equals(modeInfo.getPackNum(), opt.getPackNum())
                && Objects.equals(modeInfo.getStatus(), 1)
                && Objects.equals(modeInfo.getMode(), expectedMode);
    }

    /** 将测试类型映射为采集模块工作模式。 */
    private Integer resolveCollectorMode(Integer testType) {
        if (Objects.equals(testType, BatteryTestEnum._2.getDictValue())) {
            return BatteryModeStatusService.MODE_CONNECT_RESISTANCE;
        }
        if (Objects.equals(testType, BatteryTestEnum._6.getDictValue())) {
            return BatteryModeStatusService.MODE_INTERNAL_RESISTANCE;
        }
        return null;
    }

    /** 判断执行结果是否成功。 */
    private boolean isSuccess(AjaxResult result) {
        if (result == null) {
            return false;
        }
        Object code = result.get(AjaxResult.CODE_TAG);
        return Objects.equals(code, AjaxResult.Type.SUCCESS.value())
                || Objects.equals(String.valueOf(code), String.valueOf(AjaxResult.Type.SUCCESS.value()));
    }

    /** 更新下次调度时间，无间隔的计划执行后禁用。 */
    private void updateNextSchedule(DevBatteryOpt opt, Date now) {
        Integer execCount = opt.getExecCount() == null ? 0 : opt.getExecCount();
        opt.setExecCount(execCount + 1);
        if (opt.getIntervalDays() != null && opt.getIntervalDays() > 0) {
            long intervalMillis = Math.max(MIN_INTERVAL_MILLIS, opt.getIntervalDays() * 24L * 60L * 60L * 1000L);
            opt.setTestTime(new Date(Math.max(now.getTime(), opt.getTestTime().getTime()) + intervalMillis));
        } else {
            opt.setIsEnabled(YesNoEnum.NO.getDictValue());
        }
        devBatteryOptService.updateDevBatteryOpt(opt);
    }
}