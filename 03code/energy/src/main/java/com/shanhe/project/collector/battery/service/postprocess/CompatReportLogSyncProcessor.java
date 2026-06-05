package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleCompatReportLogSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 兼容历史报告同步处理器。
 *
 * <p>将标准实时模型同步到旧 dev_battery_report_log 过渡表，统一纳入后处理流水线。</p>
 */
@Slf4j
@Component
public class CompatReportLogSyncProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryCollectorProperties properties;

    @Resource
    private BatteryModuleCompatReportLogSyncService compatReportLogSyncService;

    @Override
    public String getName() {
        return "compatReportLogSync";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return properties != null
                && Boolean.TRUE.equals(properties.getCompatReportLogEnabled())
                && compatReportLogSyncService != null
                && context.getChannelConfig() != null
                && context.getGroup() != null
                && context.getCells() != null
                && !context.getCells().isEmpty();
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        try {
            compatReportLogSyncService.sync(
                    context.getChannelConfig(),
                    context.getGroup(),
                    context.getCells());
        } catch (Exception e) {
            log.warn("同步蓄电池模块兼容报告日志失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
    }
}
