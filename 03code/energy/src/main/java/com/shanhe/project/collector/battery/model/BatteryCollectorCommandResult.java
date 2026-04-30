package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;

/**
 * 蓄电池采集命令执行结果。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryCollectorCommandResult {

    /**
     * 980 聚合命令定义。
     */
    private BatteryAggregateCommandDefinition commandDefinition;

    /**
     * 是否已映射为明确的600节模块端命令。
     */
    private boolean mappedToModuleCommand;

    /**
     * 映射后的600节模块端命令。
     */
    private BatteryModuleControlCommand moduleControlCommand;

    /**
     * 命令是否成功。
     */
    private boolean success;

    /**
     * 是否响应超时。
     */
    private boolean timeout;

    /**
     * 通道名称。
     */
    private String channelName;

    /**
     * 请求命令码。
     */
    private Integer requestCode;

    /**
     * 期望响应命令码。
     */
    private Integer responseCode;

    /**
     * 实际响应命令码。
     */
    private Integer actualResponseCode;

    /**
     * 结果消息。
     */
    private String message;
}
