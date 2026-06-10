package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 统计数据后处理器。
 * <p>
 * 将标准实时模型数据适配为旧 BatteryReportLog 格式，
 * 供统计 service 写入 stat_battery_bat / stat_battery_pack / stat_battery_res。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class StatisticsProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private IStatBatteryPackService statBatteryPackService;

    @Override
    public String getName() {
        return "statistics";
    }

    @Override
    public int getOrder() {
        return 300;
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
        try {
            BatteryReportLog report = RealtimeToReportLogAdapter.adapt(
                    packNum, context.getGroup(), context.getCells());
            statBatteryPackService.insertList(packNum, report.getPackParam(), report.getBatteryList());
        } catch (Exception e) {
            log.warn("统计后处理失败, packNum={}", packNum, e);
        }
    }

}
