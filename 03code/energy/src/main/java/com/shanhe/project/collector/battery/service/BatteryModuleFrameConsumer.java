package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;

/**
 * 600节采集模块端帧消费扩展点。
 *
 * @author wjh
 * @since 2026-04-28
 */
public interface BatteryModuleFrameConsumer {

    void consume(BatteryCollectorChannelConfig channelConfig,
                 BatteryCollectorFrame frame,
                 BatteryModuleFrameSummary summary);
}
