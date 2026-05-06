package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 600节模块端实时数据入库消费器。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Component
public class BatteryModuleRealtimeConsumer implements BatteryModuleFrameConsumer {

    /**
     * 采集模块配置。
     */
    @Resource
    private BatteryCollectorProperties properties;

    /**
     * 600 节模块端帧解析服务。
     */
    @Resource
    private BatteryModuleFrameDataParserService parserService;

    /**
     * 实时数据 Mapper。
     */
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;

    /**
     * 电池组指标计算服务。
     */
    @Resource
    private BatteryModuleGroupCalculationService calculationService;

    /**
     * 单体兼容字段缓存填充服务。
     */
    @Resource
    private BatteryModuleCellCompatibilityFillService compatibilityFillService;

    /**
     * 旧历史记录兼容同步服务。
     */
    @Resource
    private BatteryModuleCompatReportLogSyncService compatReportLogSyncService;

    /**
     * 告警适配服务。
     */
    @Resource
    private BatteryModuleAlarmAdaptService alarmAdaptService;

    @Override
    public void consume(BatteryCollectorChannelConfig channelConfig,
                        BatteryCollectorFrame frame,
                        BatteryModuleFrameSummary summary) {
        if (!Boolean.TRUE.equals(properties.getRealtimeDataEnabled())) {
            return;
        }
        BatteryModuleFrameData data = parserService.parse(frame);
        if (data == null || data.getType() == null) {
            return;
        }
        try {
            boolean saved = false;
            if (data.getType() == BatteryModuleDataType.SINGLE_MODULE_INFO) {
                if (!data.isSuccess()) {
                    return;
                }
                BatteryModuleCellRealtime cell = buildCell(channelConfig, data);
                BatteryModulePollContext context = BatteryModulePollContextHolder.get();
                if (context == null) {
                    realtimeMapper.upsertCell(cell);
                } else {
                    context.getCells().add(cell);
                }
                saved = true;
            } else if (data.getType() == BatteryModuleDataType.ARRAY_MODULE_INFO) {
                BatteryModuleGroupRealtime group = buildGroup(channelConfig, data);
                BatteryModulePollContext context = BatteryModulePollContextHolder.get();
                if (context == null) {
                    realtimeMapper.upsertGroup(group);
                } else {
                    context.getGroups().add(group);
                }
                saved = true;
            }
            if (saved && BatteryModulePollContextHolder.get() == null && shouldCalculateAfterSave(data.getType())) {
                calculateIfEnabled(channelConfig, null);
            }
        } catch (Exception e) {
            log.warn("save battery module realtime failed, channel={}, type={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    data.getType(),
                    e);
        }
    }

    boolean shouldCalculateAfterSave(BatteryModuleDataType dataType) {
        return dataType == BatteryModuleDataType.ARRAY_MODULE_INFO;
    }

    /**
     * 批量刷写当前轮询批次内缓存的实时数据。
     *
     * @param channelConfig 通道配置
     */
    public void flushCurrentPollBatch(BatteryCollectorChannelConfig channelConfig) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        try {
            // 轮询线程内先聚合，再批量写库，避免 600 节单体逐条放大写入压力。
            if (!context.getCells().isEmpty()) {
                realtimeMapper.upsertCells(context.getCells());
            }
            if (!context.getGroups().isEmpty()) {
                realtimeMapper.upsertGroups(context.getGroups());
            }
            if (!context.getCells().isEmpty() || !context.getGroups().isEmpty()) {
                BatteryModuleGroupRealtime calculation = calculateIfEnabled(channelConfig, context);
                adaptAlarmContext(channelConfig, context, calculation);
                syncCompatReportLogIfEnabled(channelConfig, context, calculation);
            }
        } catch (Exception e) {
            log.warn("flush battery module realtime batch failed, channel={}, batch={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    context.getPollBatchNo(),
                    e);
        }
    }

    BatteryModuleGroupRealtime calculateIfEnabled(BatteryCollectorChannelConfig channelConfig,
                                                  BatteryModulePollContext context) {
        if (!Boolean.TRUE.equals(properties.getGroupCalculationEnabled())) {
            return null;
        }
        if (calculationService == null || channelConfig == null
                || channelConfig.getName() == null || channelConfig.getBatteryGroup() == null) {
            return null;
        }
        try {
            if (context == null) {
                return calculationService.calculateAndSave(channelConfig,
                        channelConfig.getBatteryGroup(),
                        null,
                        null,
                        resolveCalculationStaleThresholdMs());
            } else {
                return calculationService.calculateAndSave(channelConfig,
                        channelConfig.getBatteryGroup(),
                        context.getPollBatchNo(),
                        context.getPollStartedAt(),
                        resolveCalculationStaleThresholdMs());
            }
        } catch (Exception e) {
            log.warn("calculate battery module group failed, channel={}, group={}",
                    channelConfig.getName(),
                    channelConfig.getBatteryGroup(),
                    e);
        }
        return null;
    }

    void adaptAlarmContext(BatteryCollectorChannelConfig channelConfig,
                           BatteryModulePollContext context,
                           BatteryModuleGroupRealtime calculation) {
        if (alarmAdaptService == null || context == null) {
            return;
        }
        try {
            BatteryModuleAlarmContext alarmContext = alarmAdaptService.buildContext(calculation, context.getCells());
            context.setAlarmContext(alarmContext);
        } catch (Exception e) {
            log.warn("adapt battery module alarm context failed, channel={}, group={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    channelConfig == null ? null : channelConfig.getBatteryGroup(),
                    e);
        }
    }

    void syncCompatReportLogIfEnabled(BatteryCollectorChannelConfig channelConfig,
                                      BatteryModulePollContext context,
                                      BatteryModuleGroupRealtime calculation) {
        if (!Boolean.TRUE.equals(properties.getCompatReportLogEnabled())
                || compatReportLogSyncService == null
                || context == null
                || calculation == null) {
            return;
        }
        try {
            compatReportLogSyncService.sync(channelConfig, calculation, context.getCells());
        } catch (Exception e) {
            log.warn("sync battery module compat report log failed, channel={}, group={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    channelConfig == null ? null : channelConfig.getBatteryGroup(),
                    e);
        }
    }

    long resolveCalculationStaleThresholdMs() {
        Long threshold = properties.getGroupCalculationStaleThresholdMs();
        return threshold == null || threshold <= 0 ? 180_000L : threshold;
    }

    BatteryModuleCellRealtime buildCell(BatteryCollectorChannelConfig channelConfig, BatteryModuleFrameData data) {
        BatteryModuleCellRealtime realtime = new BatteryModuleCellRealtime();
        realtime.setPackNum(channelConfig == null ? null : channelConfig.getBatteryGroup());
        realtime.setBatNum(data.getModuleAddress());
        realtime.setVoltage(data.getCellVoltage());
        realtime.setResistance(data.getInternalResistance());
        realtime.setTemperature(data.getCellTemperature());
        realtime.setSwollenVoltage(data.getSwollenVoltage());
        realtime.setLeakageStatus(data.getLeakageStatus());
        compatibilityFillService.fillFromCache(channelConfig, realtime);
        applyPollContext(realtime);
        return realtime;
    }

    BatteryModuleGroupRealtime buildGroup(BatteryCollectorChannelConfig channelConfig, BatteryModuleFrameData data) {
        BatteryModuleGroupRealtime realtime = new BatteryModuleGroupRealtime();
        realtime.setPackNum(channelConfig == null ? null : channelConfig.getBatteryGroup());
        realtime.setPackCurrent(data.getChargeDischargeCurrent());
        realtime.setBatteryPackFloatCurrent(data.getFloatCurrent());
        realtime.setBatteryPackOuterVoltage(data.getExternalVoltage());
        realtime.setChargeDischargeCurrent(data.getChargeDischargeCurrent());
        realtime.setFloatCurrent(data.getFloatCurrent());
        realtime.setExternalVoltage(data.getExternalVoltage());
        realtime.setEnvironmentTemperature1(data.getEnvironmentTemperature1());
        realtime.setEnvironmentTemperature2(data.getEnvironmentTemperature2());
        realtime.setGroupModuleFresh(data.isSuccess());
        applyPollContext(realtime);
        return realtime;
    }

    private void applyPollContext(BatteryModuleCellRealtime realtime) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        realtime.setPollBatchNo(context.getPollBatchNo());
        realtime.setPollStartedAt(context.getPollStartedAt());
    }

    private void applyPollContext(BatteryModuleGroupRealtime realtime) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        realtime.setPollBatchNo(context.getPollBatchNo());
        realtime.setPollStartedAt(context.getPollStartedAt());
    }
}
