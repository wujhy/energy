package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.config.domain.BatteryMonitor;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.collector.battery.service.BatteryStorageIntervalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 兼容历史报告同步处理器。
 *
 * <p>将标准实时模型同步到旧 dev_battery_report_log 过渡表，统一纳入后处理流水线。</p>
 *
 * @author wjh
 * @since 2026-06-18
 */
@Slf4j
@Component
public class CompatReportLogSyncProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryCollectorProperties properties;

    @Resource
    private BatteryReportLogService batteryReportLogService;

    @Resource
    private BatteryStorageIntervalService storageIntervalService;

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
        return context != null
                && properties != null
                && Boolean.TRUE.equals(properties.getCompatReportLogEnabled())
                && context.getChannelConfig() != null
                && context.getGroup() != null
                && context.getCells() != null
                && !context.getCells().isEmpty()
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        try {
            sync(context.getChannelConfig(), context.getGroup(), context.getCells());
        } catch (Exception e) {
            log.warn("同步蓄电池模块兼容报告日志失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
    }

    /** 同步本轮采集结果到旧 dev_battery_report_log 历史链路。 */
    private void sync(BatteryCollectorChannelConfig channelConfig,
                      BatteryModuleGroupRealtime group,
                      List<BatteryModuleCellRealtime> cells) {
        if (channelConfig == null || channelConfig.getBatteryGroup() == null) {
            return;
        }
        BatteryReportLog reportLog = RealtimeToReportLogAdapter.adapt(
                channelConfig.getBatteryGroup(), group, cells);
        if (reportLog == null) {
            return;
        }
        List<BatteryMonitor> batteryList = reportLog.getBatteryList();
        if (batteryList == null || batteryList.isEmpty()
                || reportLog.getPackParam() == null || reportLog.getPackParam().isEmpty()) {
            return;
        }
        boolean isInsert = storageIntervalService.shouldInsert(channelConfig.getBatteryGroup());
        if (isInsert) {
            batteryReportLogService.insert(channelConfig.getBatteryGroup(), reportLog.getPackParam(), batteryList);
        }
        log.debug("同步蓄电池模块实时数据到历史记录, 电池组={}, 是否插入={}",
                channelConfig.getBatteryGroup(), isInsert);
    }
}
