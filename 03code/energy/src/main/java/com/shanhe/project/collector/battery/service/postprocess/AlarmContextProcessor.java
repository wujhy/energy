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
        return alarmAdaptService != null
                && context.getPackNum() != null
                && context.getCells() != null
                && !context.getCells().isEmpty();
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        try {
            BatteryModuleAlarmContext alarmContext =
                    alarmAdaptService.buildContext(context.getGroup(), context.getCells());
            context.setAlarmContext(alarmContext);
        } catch (Exception e) {
            log.warn("适配蓄电池模块告警上下文失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
    }
}
