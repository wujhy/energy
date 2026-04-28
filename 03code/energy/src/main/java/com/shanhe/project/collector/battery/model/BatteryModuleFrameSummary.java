package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.Builder;
import lombok.Data;

/**
 * 600节采集模块端帧摘要。
 *
 * <p>该对象只描述模块端原始帧，不等同于 980 节电池组聚合数据。
 */
@Data
@Builder
public class BatteryModuleFrameSummary {

    private BatteryDeviceProtocolCode protocolCode;

    private boolean known;

    private boolean success;

    private int responseFlag;

    private Integer moduleAddress;

    private Integer payloadLength;

    private String payloadHex;

    private String description;
}
