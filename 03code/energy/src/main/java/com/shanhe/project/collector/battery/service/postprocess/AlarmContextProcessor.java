package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 告警上下文适配处理器。
 * <p>
 * 从标准实时模型构建告警候选上下文，供 AlarmLogService 消费。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class AlarmContextProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryModuleAlarmAdaptService alarmAdaptService;

    @Override
    public String getName() {
        return "alarmContext";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context.getPackNum() != null
                && context.getCells() != null
                && !context.getCells().isEmpty();
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        // 告警上下文已由 adaptAlarmContext 在 runPostProcess 中构建，
        // 此处理器预留为后续扩展点（如阈值告警引擎），当前不重复构建。
    }
}
