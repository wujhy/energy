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
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Component
public class BatteryModuleFrameDispatcher {

    /**
     * 帧摘要服务。
     */
    @Autowired
    private BatteryModuleFrameSummaryService summaryService;

    /**
     * 显式注册的帧消费者。
     */
    @Autowired(required = false)
    private List<BatteryModuleFrameConsumer> consumers = new ArrayList<>();

    /**
     * 分发 600 节模块端帧。
     *
     * @param channelConfig 通道配置
     * @param frame 协议帧
     */
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

        // 后续入库、计算、兼容输出都从 consumer 扩展，不在这里耦合旧业务。
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
