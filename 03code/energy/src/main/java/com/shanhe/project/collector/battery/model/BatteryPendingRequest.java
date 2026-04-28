package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.Builder;
import lombok.Data;

/**
 * 蓄电池采集待响应请求。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryPendingRequest {

    /**
     * 请求命令码。
     */
    private int requestCode;

    /**
     * 期望响应命令码。
     */
    private int responseCode;

    /**
     * 本次请求发送的模块地址。
     */
    private int requestAddress;

    /**
     * 请求信息域。
     */
    private byte[] payload;

    /**
     * 请求名称。
     */
    private String name;

    /**
     * 是否自动轮询产生。
     */
    private boolean autoPoll;

    /**
     * 根据 600 节模块端协议码创建待响应请求。
     *
     * @param protocolCode 协议码
     * @param requestAddress 请求模块地址
     * @param payload 请求信息域
     * @param autoPoll 是否自动轮询
     * @return 待响应请求
     */
    public static BatteryPendingRequest fromProtocolCode(BatteryDeviceProtocolCode protocolCode, int requestAddress, byte[] payload, boolean autoPoll) {
        return BatteryPendingRequest.builder()
                .requestCode(protocolCode.getRequestCode())
                .responseCode(protocolCode.getResponseCode() == null ? 0 : protocolCode.getResponseCode())
                .requestAddress(requestAddress)
                .payload(payload)
                .name(protocolCode.name())
                .autoPoll(autoPoll)
                .build();
    }

    /**
     * 创建显式命令待响应请求。
     *
     * @param requestCode 请求命令码
     * @param responseCode 期望响应命令码
     * @param payload 请求信息域
     * @param name 请求名称
     * @return 待响应请求
     */
    public static BatteryPendingRequest command(int requestCode, int responseCode, byte[] payload, String name) {
        return BatteryPendingRequest.builder()
                .requestCode(requestCode)
                .responseCode(responseCode)
                .requestAddress(0)
                .payload(payload)
                .name(name)
                .autoPoll(false)
                .build();
    }
}
