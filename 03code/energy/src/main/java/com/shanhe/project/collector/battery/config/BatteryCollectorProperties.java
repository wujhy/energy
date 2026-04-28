package com.shanhe.project.collector.battery.config;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Battery collector config.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "battery-collector.collector")
public class BatteryCollectorProperties {

    private Boolean enabled = Boolean.FALSE;

    /**
     * 第一阶段默认关闭主动轮询，避免与旧链路冲突。
     */
    private Boolean autoPollEnabled = Boolean.FALSE;

    private Long loopDelayMs = 300L;

    private Long requestGapMs = 120L;

    /**
     * 是否开启协议级调试日志。
     */
    private Boolean debugEnabled = Boolean.FALSE;

    /**
     * 是否保存 600 节模块端原始帧日志；联调或追溯时打开。
     */
    private Boolean rawFrameLogEnabled = Boolean.FALSE;

    /**
     * 是否写入 600 节模块端标准实时数据表。
     */
    private Boolean realtimeDataEnabled = Boolean.FALSE;

    /**
     * Whether to calculate group metrics after realtime data is saved.
     */
    private Boolean groupCalculationEnabled = Boolean.FALSE;

    /**
     * Realtime cell data freshness threshold for group calculation.
     */
    private Long groupCalculationStaleThresholdMs = 180_000L;

    /**
     * Whether to poll only cached responsive 600-module addresses after discovery.
     */
    private Boolean moduleAddressCacheEnabled = Boolean.TRUE;

    /**
     * Consecutive no-response count before removing a cached module address.
     */
    private Integer moduleAddressMissThreshold = 3;

    /**
     * Optional periodic full discovery interval. 0 means only startup/manual reset.
     */
    private Long moduleAddressFullDiscoveryIntervalMs = 0L;

    /**
     * 指定需要输出协议日志的通道名称；为空时表示全部通道。
     */
    private List<String> debugChannels = new ArrayList<>();

    /**
     * 指定本轮实际运行的通道名称；为空时按 enabled 规则运行。
     */
    private List<String> activeChannels = new ArrayList<>();

    private List<BatteryCollectorChannelConfig> channels = new ArrayList<>();
}
