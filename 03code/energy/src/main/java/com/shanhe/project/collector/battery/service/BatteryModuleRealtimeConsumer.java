package com.shanhe.project.collector.battery.service;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.realtime.BatteryModuleGroupCalculationService;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.model.BatteryRealtimePostProcessRequest;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessContext;
import com.shanhe.project.collector.battery.postprocess.BatteryRealtimePostProcessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 600节模块端实时数据入库消费器。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Component
public class BatteryModuleRealtimeConsumer implements BatteryModuleFrameConsumer {

    private static final AtomicInteger POST_PROCESS_THREAD_INDEX = new AtomicInteger(1);

    /**
     * 轮询外后处理线程池，避免兼容历史和告警上下文占用采集轮询线程。
     */
    private final ExecutorService postProcessExecutor = new ThreadPoolExecutor(2, 2,
            0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), postProcessThreadFactory());

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
     * 实时数据后处理流水线服务。
     */
    @Resource
    private BatteryRealtimePostProcessService postProcessService;
    @Resource
    private BatteryRealtimePostProcessContextFactory postProcessContextFactory = new BatteryRealtimePostProcessContextFactory();

    /**
     * 单体兼容字段缓存填充服务。
     */
    @Resource
    private BatteryModuleCellCompatibilityFillService compatibilityFillService;

    /**
     * 标准实时有效快照服务。
     */
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;

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
            log.warn("保存蓄电池模块实时数据失败, 通道={}, 类型={}",
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
                com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot realtimeSnapshot =
                        refreshRealtimeSnapshot(channelConfig, context, calculation);
                refreshBatteryOnlineCache(channelConfig);
                submitPostProcess(BatteryRealtimePostProcessRequest.builder()
                        .channelConfig(channelConfig)
                        .pollContext(context)
                        .calculation(calculation)
                        .realtimeSnapshot(realtimeSnapshot)
                        .build());
            }
        } catch (Exception e) {
            log.warn("刷新蓄电池模块实时数据批次失败, 通道={}, 批次={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    context.getPollBatchNo(),
                    e);
        }
    }

    void submitPostProcess(BatteryRealtimePostProcessRequest request) {
        BatteryRealtimePostProcessRequest snapshotRequest =
                postProcessContextFactory.snapshotRequest(request);
        if (snapshotRequest == null) {
            return;
        }
        postProcessExecutor.execute(() -> runPostProcess(snapshotRequest));
    }

    void runPostProcess(BatteryRealtimePostProcessRequest request) {
        executePostProcessPipeline(request);
    }

    /** 执行后处理流水线。 */
    private void executePostProcessPipeline(BatteryRealtimePostProcessRequest request) {
        BatteryModulePollContext context = request == null ? null : request.getPollContext();
        if (postProcessService == null || context == null) {
            return;
        }
        try {
            BatteryRealtimePostProcessContext postContext =
                    postProcessContextFactory.buildContext(request);
            postProcessService.execute(postContext);
            context.setAlarmContext(postContext.getAlarmContext());
        } catch (Exception e) {
            BatteryCollectorChannelConfig channelConfig = request.getChannelConfig();
            log.warn("后处理流水线执行失败, 通道={}", channelConfig == null ? null : channelConfig.getName(), e);
        }
    }

    private com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot refreshRealtimeSnapshot(
            BatteryCollectorChannelConfig channelConfig,
            BatteryModulePollContext context,
            BatteryModuleGroupRealtime calculation) {
        if (snapshotService == null || channelConfig == null || channelConfig.getBatteryGroup() == null) {
            return null;
        }
        try {
            return snapshotService.refreshAfterPoll(channelConfig.getBatteryGroup(), context, calculation);
        } catch (Exception e) {
            log.warn("刷新蓄电池标准实时快照失败, 通道={}, 电池组={}",
                    channelConfig.getName(), channelConfig.getBatteryGroup(), e);
            return null;
        }
    }

    void refreshBatteryOnlineCache(BatteryCollectorChannelConfig channelConfig) {
        if (channelConfig == null || channelConfig.getBatteryGroup() == null) {
            return;
        }
        String key = String.format(CacheKeyEnum.BATTERY_ONLINE.getKey(), channelConfig.getBatteryGroup());
        CacheUtils.put(CacheKeyEnum.BATTERY_ONLINE.getCache(), key, new Date());
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
            log.warn("计算蓄电池模块组数据失败, 通道={}, 电池组={}",
                    channelConfig.getName(),
                    channelConfig.getBatteryGroup(),
                    e);
        }
        return null;
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
        if (data.isSuccess()) {
            realtime.setPackCurrent(data.getChargeDischargeCurrent());
            realtime.setBatteryPackFloatCurrent(data.getFloatCurrent());
            realtime.setBatteryPackOuterVoltage(data.getExternalVoltage());
            realtime.setChargeDischargeCurrent(data.getChargeDischargeCurrent());
            realtime.setFloatCurrent(data.getFloatCurrent());
            realtime.setExternalVoltage(data.getExternalVoltage());
            realtime.setEnvironmentTemperature1(data.getEnvironmentTemperature1());
            realtime.setEnvironmentTemperature2(data.getEnvironmentTemperature2());
        }
        realtime.setGroupModuleFresh(data.isSuccess());
        applyPollContext(realtime);
        return realtime;
    }

    /** 为单体实时数据设置轮询批次上下文。 */
    private void applyPollContext(BatteryModuleCellRealtime realtime) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        realtime.setPollBatchNo(context.getPollBatchNo());
        realtime.setPollStartedAt(context.getPollStartedAt());
    }

    /** 为电池组实时数据设置轮询批次上下文。 */
    private void applyPollContext(BatteryModuleGroupRealtime realtime) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        realtime.setPollBatchNo(context.getPollBatchNo());
        realtime.setPollStartedAt(context.getPollStartedAt());
    }

    @PreDestroy
    public void destroy() {
        postProcessExecutor.shutdownNow();
    }

    private static ThreadFactory postProcessThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable,
                    "battery-module-post-process-" + POST_PROCESS_THREAD_INDEX.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }
}
