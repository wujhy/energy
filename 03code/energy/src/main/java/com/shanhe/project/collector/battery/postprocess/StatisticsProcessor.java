package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.project.manage.stat.service.IStatBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 统计数据后处理器。
 * <p>
 * 直接以标准实时模型（组实时 + 单体实时）调用统计 service，
 * 写入 stat_battery_bat / stat_battery_pack，不再经过旧上报日志结构。
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
            statBatteryPackService.insertRealtime(packNum, context.getGroup(), context.getCells());
        } catch (Exception e) {
            log.warn("统计后处理失败, packNum={}", packNum, e);
        }
    }

}
