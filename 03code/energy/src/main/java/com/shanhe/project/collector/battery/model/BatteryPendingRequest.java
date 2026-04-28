package com.shanhe.project.collector.battery.model;

import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatteryPendingRequest {

    private int requestCode;

    private int responseCode;

    private int requestAddress;

    private byte[] payload;

    private String name;

    private boolean autoPoll;

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
