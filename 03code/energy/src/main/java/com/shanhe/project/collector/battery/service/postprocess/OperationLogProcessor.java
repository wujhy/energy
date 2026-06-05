package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.opt.service.OptLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;

/**
 * 操作日志后处理器。
 * <p>
 * 将标准实时模型适配为旧操作日志服务入参，仅在电池组状态已明确时触发。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class OperationLogProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private OptLogService optLogService;

    @Resource
    private BatteryReportLogService batteryReportLogService;

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
        return context.getPackNum() != null && context.getGroup() != null;
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryReportLog report = RealtimeToReportLogAdapter.adapt(
                packNum, context.getGroup(), context.getCells());
        Map<String, Object> packParam = report.getPackParam();
        String batteryPackStatus = Objects.toString(packParam.get("batteryPackStatus"), null);
        if (!isKnownBatteryPackStatus(batteryPackStatus)) {
            log.debug("操作日志后处理跳过：电池组状态未确认, packNum={}, status={}", packNum, batteryPackStatus);
            return;
        }

        try {
            BatteryReportLog oldInfo = batteryReportLogService.lastCache(packNum);
            optLogService.insertBattery(packNum, packParam, oldInfo);
        } catch (Exception e) {
            log.warn("操作日志后处理失败, packNum={}", packNum, e);
        }
    }

    private boolean isKnownBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.find(status) != null;
    }
}
