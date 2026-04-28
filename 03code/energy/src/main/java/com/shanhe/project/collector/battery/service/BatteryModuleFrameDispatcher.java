package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 600节采集模块端帧分发器。
 *
 * <p>当前只分发 600 节模块端帧到显式注册的 consumer，不默认桥接旧 BatteryHandler。
 */
@Slf4j
@Component
public class BatteryModuleFrameDispatcher {

    @Autowired
    private BatteryModuleFrameSummaryService summaryService;

    @Autowired(required = false)
    private List<BatteryModuleFrameConsumer> consumers = new ArrayList<>();

    public void dispatch(BatteryCollectorChannelConfig channelConfig, BatteryCollectorFrame frame) {
        BatteryModuleFrameSummary summary = summaryService.summarize(frame);
        if (summary != null) {
            log.debug("battery module frame, channel={}, code={}, known={}, success={}, moduleAddress={}, len={}",
                    channelConfig == null ? null : channelConfig.getName(),
                    formatCode(frame, summary),
                    summary.isKnown(),
                    summary.isSuccess(),
                    summary.getModuleAddress(),
                    summary.getPayloadLength());
        }

        for (BatteryModuleFrameConsumer consumer : consumers) {
            consumer.consume(channelConfig, frame, summary);
        }
    }

    private String formatCode(BatteryCollectorFrame frame, BatteryModuleFrameSummary summary) {
        if (summary.getProtocolCode() == null || summary.getProtocolCode().isResponse(frame.getCommand())) {
            return String.format("%02X", frame.getCommand());
        }
        return String.format("%02X", summary.getProtocolCode().getRequestCode());
    }
}
