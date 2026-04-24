package com.shanhe.project.collector.battery.config;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Battery 采集模块配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "battery-collector.collector")
public class BatteryCollectorProperties {

    private Boolean enabled = Boolean.FALSE;

    /**
     * 第一阶段先保留模块边界，默认不主动抢占旧链路。
     */
    private Boolean autoPollEnabled = Boolean.FALSE;

    private Long loopDelayMs = 300L;

    private Long requestGapMs = 120L;

    private List<BatteryCollectorChannelConfig> channels = new ArrayList<>();
}
