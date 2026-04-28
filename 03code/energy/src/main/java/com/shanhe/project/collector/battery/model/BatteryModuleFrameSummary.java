package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.Builder;
import lombok.Data;

/**
 * 600节采集模块端帧摘要。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryModuleFrameSummary {

    /**
     * 匹配到的 600 节模块端协议码。
     */
    private BatteryDeviceProtocolCode protocolCode;

    /**
     * 是否已知协议。
     */
    private boolean known;

    /**
     * 响应是否成功。
     */
    private boolean success;

    /**
     * 原始应答标志。
     */
    private int responseFlag;

    /**
     * 模块地址。
     */
    private Integer moduleAddress;

    /**
     * 信息域长度。
     */
    private Integer payloadLength;

    /**
     * 信息域十六进制内容。
     */
    private String payloadHex;

    /**
     * 摘要说明。
     */
    private String description;
}
