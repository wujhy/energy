package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;

/**
 * 600节采集模块端帧消费扩展点。
 *
 * <p>入库、计算、980 兼容视图、调试记录等后续能力应各自实现 consumer，
 * 不应把 600 节原始帧直接转交旧 980 聚合处理器。
 */
public interface BatteryModuleFrameConsumer {

    void consume(BatteryCollectorChannelConfig channelConfig,
                 BatteryCollectorFrame frame,
                 BatteryModuleFrameSummary summary);
}
