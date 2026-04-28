package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleFrameLogMapper;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameLog;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 600节模块端原始帧日志消费器。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Component
public class BatteryModuleFrameLogConsumer implements BatteryModuleFrameConsumer {

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
     * 原始帧日志 Mapper。
     */
    @Resource
    private BatteryModuleFrameLogMapper frameLogMapper;

    @Override
    public void consume(BatteryCollectorChannelConfig channelConfig,
                        BatteryCollectorFrame frame,
                        BatteryModuleFrameSummary summary) {
        if (!Boolean.TRUE.equals(properties.getRawFrameLogEnabled())) {
            return;
        }
        BatteryModuleFrameData data = parserService.parse(frame);
        BatteryModuleFrameLog frameLog = buildLog(channelConfig, frame, summary, data);
        try {
            frameLogMapper.insertOne(frameLog);
        } catch (Exception e) {
            log.warn("save battery module frame log failed, channel={}, command={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    frame == null ? null : String.format("%02X", frame.getCommand()),
                    e);
        }
    }

    BatteryModuleFrameLog buildLog(BatteryCollectorChannelConfig channelConfig,
                                   BatteryCollectorFrame frame,
                                   BatteryModuleFrameSummary summary,
                                   BatteryModuleFrameData data) {
        BatteryModuleFrameLog log = new BatteryModuleFrameLog();
        log.setChannelName(channelConfig == null ? null : channelConfig.getName());
        log.setPortName(channelConfig == null ? null : channelConfig.getPortName());
        log.setBatteryGroup(channelConfig == null ? null : channelConfig.getBatteryGroup());
        log.setModuleAddress(frame == null ? null : frame.getAddress());
        log.setCommandCode(frame == null ? null : String.format("%02X", frame.getCommand()));
        log.setFrameHex(frame == null ? null : frame.toHex());
        if (summary != null) {
            log.setKnown(summary.isKnown());
            log.setSuccess(summary.isSuccess());
            log.setResponseFlag(summary.getResponseFlag());
            log.setPayloadLength(summary.getPayloadLength());
            log.setPayloadHex(summary.getPayloadHex());
        }
        if (data != null) {
            log.setParsedType(data.getType() == null ? null : data.getType().name());
            log.setCellVoltage(data.getCellVoltage());
            log.setInternalResistance(data.getInternalResistance());
            log.setCellTemperature(data.getCellTemperature());
            log.setLeakageStatus(data.getLeakageStatus());
            log.setSwollenVoltage(data.getSwollenVoltage());
            log.setChargeDischargeCurrent(data.getChargeDischargeCurrent());
            log.setFloatCurrent(data.getFloatCurrent());
            log.setExternalVoltage(data.getExternalVoltage());
            log.setEnvironmentTemperature1(data.getEnvironmentTemperature1());
            log.setEnvironmentTemperature2(data.getEnvironmentTemperature2());
            log.setConnectBatteryVoltage(data.getConnectBatteryVoltage());
            log.setConnectTestVoltage(data.getConnectTestVoltage());
        }
        return log;
    }
}
