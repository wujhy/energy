package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.framework.enums.ResistanceTestStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.opt.service.OptLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 操作日志后处理器。
 * <p>
 * 测试计划执行入口已负责创建运行日志；采集后处理只在状态确认离开测试态时关闭已有日志。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class OperationLogProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private OptLogService optLogService;

    @Override
    public String getName() {
        return "operationLog";
    }

    @Override
    public int getOrder() {
        return 250;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryModuleGroupRealtime group = context.getGroup();
        if (group == null) {
            return;
        }
        closeBatteryPackLogsIfEnded(packNum, group.getBatteryPackStatus());
        closeResistanceLogIfEnded(packNum, group.getResistanceTestStatus());
    }

    private void closeBatteryPackLogsIfEnded(Integer packNum, Integer batteryPackStatusValue) {
        String batteryPackStatus = Objects.toString(batteryPackStatusValue, null);
        if (!isKnownBatteryPackStatus(batteryPackStatus)) {
            log.debug("操作日志后处理跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }
        if (isActiveBatteryPackStatus(batteryPackStatus)) {
            return;
        }
        closeIfRunning(packNum, BatteryTestEnum._3.getDictValue());
        closeIfRunning(packNum, BatteryTestEnum._5.getDictValue());
        closeIfRunning(packNum, BatteryTestEnum._7.getDictValue());
    }

    private void closeResistanceLogIfEnded(Integer packNum, Integer resistanceTestStatusValue) {
        String resistanceTestStatus = Objects.toString(resistanceTestStatusValue, null);
        if (ResistanceTestStatusEnum.find(resistanceTestStatus) == null) {
            return;
        }
        if (ResistanceTestStatusEnum.isCode(resistanceTestStatus, ResistanceTestStatusEnum.TESTING)) {
            return;
        }
        closeIfRunning(packNum, BatteryTestEnum._1.getDictValue());
    }

    private void closeIfRunning(Integer packNum, Integer type) {
        try {
            optLogService.doStopTest(packNum, type);
        } catch (Exception e) {
            log.warn("操作日志后处理关闭失败, packNum={}, type={}", packNum, type, e);
        }
    }

    private boolean isKnownBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.find(status) != null;
    }

    private boolean isActiveBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.CHARGE)
                || BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.CAPACITY_TEST)
                || BatteryPackStatusEnum.isCode(status, BatteryPackStatusEnum.BACKUP);
    }
}
