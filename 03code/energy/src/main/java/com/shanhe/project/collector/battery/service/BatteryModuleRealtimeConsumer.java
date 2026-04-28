package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
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
 * Optional consumer that stores parsed 600-cell module data into standard realtime tables.
 */
@Slf4j
@Component
public class BatteryModuleRealtimeConsumer implements BatteryModuleFrameConsumer {

    @Resource
    private BatteryCollectorProperties properties;
    @Resource
    private BatteryModuleFrameDataParserService parserService;
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;
    @Resource
    private BatteryModuleGroupCalculationService calculationService;

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
                calculateIfEnabled(channelConfig);
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

    public void flushCurrentPollBatch(BatteryCollectorChannelConfig channelConfig) {
        BatteryModulePollContext context = BatteryModulePollContextHolder.get();
        if (context == null) {
            return;
        }
        try {
            if (!context.getCells().isEmpty()) {
                realtimeMapper.upsertCells(context.getCells());
            }
            if (!context.getGroups().isEmpty()) {
                realtimeMapper.upsertGroups(context.getGroups());
                calculateIfEnabled(channelConfig);
            }
        } catch (Exception e) {
            log.warn("flush battery module realtime batch failed, channel={}, batch={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    context.getPollBatchNo(),
                    e);
        }
    }

    void calculateIfEnabled(BatteryCollectorChannelConfig channelConfig) {
        if (!Boolean.TRUE.equals(properties.getGroupCalculationEnabled())) {
            return;
        }
        if (calculationService == null || channelConfig == null
                || channelConfig.getName() == null || channelConfig.getBatteryGroup() == null) {
            return;
        }
        try {
            calculationService.calculateAndSave(channelConfig.getName(),
                    channelConfig.getBatteryGroup(),
                    resolveCalculationStaleThresholdMs());
        } catch (Exception e) {
            log.warn("calculate battery module group failed, channel={}, group={}",
                    channelConfig.getName(),
                    channelConfig.getBatteryGroup(),
                    e);
        }
    }

    long resolveCalculationStaleThresholdMs() {
        Long threshold = properties.getGroupCalculationStaleThresholdMs();
        return threshold == null || threshold <= 0 ? 180_000L : threshold;
    }

    BatteryModuleCellRealtime buildCell(BatteryCollectorChannelConfig channelConfig, BatteryModuleFrameData data) {
        BatteryModuleCellRealtime realtime = new BatteryModuleCellRealtime();
        realtime.setChannelName(channelConfig == null ? null : channelConfig.getName());
        realtime.setPortName(channelConfig == null ? null : channelConfig.getPortName());
        realtime.setBatteryGroup(channelConfig == null ? null : channelConfig.getBatteryGroup());
        realtime.setModuleAddress(data.getModuleAddress());
        realtime.setCellVoltage(data.getCellVoltage());
        realtime.setInternalResistance(data.getInternalResistance());
        realtime.setCellTemperature(data.getCellTemperature());
        realtime.setLeakageStatus(data.getLeakageStatus());
        realtime.setSwollenVoltage(data.getSwollenVoltage());
        realtime.setSuccess(data.isSuccess());
        realtime.setResponseFlag(data.getResponseFlag());
        applyPollContext(realtime);
        return realtime;
    }

    BatteryModuleGroupRealtime buildGroup(BatteryCollectorChannelConfig channelConfig, BatteryModuleFrameData data) {
        BatteryModuleGroupRealtime realtime = new BatteryModuleGroupRealtime();
        realtime.setChannelName(channelConfig == null ? null : channelConfig.getName());
        realtime.setPortName(channelConfig == null ? null : channelConfig.getPortName());
        realtime.setBatteryGroup(channelConfig == null ? null : channelConfig.getBatteryGroup());
        realtime.setModuleAddress(data.getModuleAddress());
        realtime.setChargeDischargeCurrent(data.getChargeDischargeCurrent());
        realtime.setFloatCurrent(data.getFloatCurrent());
        realtime.setExternalVoltage(data.getExternalVoltage());
        realtime.setEnvironmentTemperature1(data.getEnvironmentTemperature1());
        realtime.setEnvironmentTemperature2(data.getEnvironmentTemperature2());
        realtime.setSuccess(data.isSuccess());
        realtime.setResponseFlag(data.getResponseFlag());
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
