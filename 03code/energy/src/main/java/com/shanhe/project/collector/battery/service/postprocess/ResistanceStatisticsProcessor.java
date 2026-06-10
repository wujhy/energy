package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 内阻统计后处理器。
 * <p>
 * 仅在标准实时模型明确提供内阻测试状态时复用旧统计服务；
 * 当前不推断 91 连接条结果，也不把普通采集内阻误判为测试结束。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class ResistanceStatisticsProcessor implements BatteryRealtimePostProcessor {

    private static final String RESISTANCE_TEST_STATUS = "resistanceTestStatus";

    @Resource
    private IStatBatteryResService statBatteryResService;

    @Resource
    private BatteryReportLogService batteryReportLogService;

    @Override
    public String getName() {
        return "resistanceStatistics";
    }

    @Override
    public int getOrder() {
        return 350;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && context.getGroup() != null
                && context.getCells() != null
                && !context.getCells().isEmpty()
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryReportLog report = RealtimeToReportLogAdapter.adapt(
                packNum, context.getGroup(), context.getCells());
        Map<String, Object> packParam = report.getPackParam();
        if (!packParam.containsKey(RESISTANCE_TEST_STATUS)) {
            log.debug("内阻统计后处理跳过：缺少内阻测试状态, packNum={}", packNum);
            return;
        }

        try {
            BatteryReportLog oldInfo = batteryReportLogService.lastCache(packNum);
            statBatteryResService.init(packNum, packParam, report.getBatteryList(), oldInfo);
        } catch (Exception e) {
            log.warn("内阻统计后处理失败, packNum={}", packNum, e);
        }
    }

}
