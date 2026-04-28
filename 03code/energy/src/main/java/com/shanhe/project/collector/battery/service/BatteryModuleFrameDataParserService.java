package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.springframework.stereotype.Service;

/**
 * Parses 600-cell module frames into the internal battery data model.
 */
@Service
public class BatteryModuleFrameDataParserService {

    private static final int ARRAY_MODULE_ADDRESS = 246;

    public BatteryModuleFrameData parse(BatteryCollectorFrame frame) {
        if (frame == null) {
            return null;
        }
        if (frame.getCommand() == BatteryDeviceProtocolCode.MODULE_INFO.getResponseCode()) {
            return parseModuleInfo(frame);
        }
        if (frame.getCommand() == BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.getResponseCode()) {
            return parseConnectResistanceVoltage(frame);
        }
        BatteryDeviceProtocolCode protocolCode = BatteryDeviceProtocolCode.find(frame.getCommand());
        if (protocolCode != null && protocolCode.isStatusResponse()) {
            return parseStatusResponse(frame, protocolCode);
        }
        if (protocolCode == BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS) {
            return parseAutoSetAddressResponse(frame, protocolCode);
        }
        return null;
    }

    private BatteryModuleFrameData parseModuleInfo(BatteryCollectorFrame frame) {
        byte[] payload = frame.getPayloadSafe();
        int address = frame.getAddress();
        if (address == ARRAY_MODULE_ADDRESS) {
            if (payload.length < 11) {
                return null;
            }
            int responseFlag = u8(payload, 0);
            return BatteryModuleFrameData.builder()
                    .type(BatteryModuleDataType.ARRAY_MODULE_INFO)
                    .moduleAddress(address)
                    .responseFlag(responseFlag)
                    .success(responseFlag == 0)
                    .chargeDischargeCurrent(scale(s16(payload, 1), 10.0d))
                    .floatCurrent(scale(s16(payload, 3), 1000.0d))
                    .externalVoltage(scale(s16(payload, 5), 100.0d))
                    .environmentTemperature1(scale(s16(payload, 7), 10.0d))
                    .environmentTemperature2(scale(s16(payload, 9), 10.0d))
                    .build();
        }
        if (address < 1 || address > 245 || payload.length < 10) {
            return null;
        }
        int responseFlag = u8(payload, 0);
        return BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.SINGLE_MODULE_INFO)
                .moduleAddress(address)
                .responseFlag(responseFlag)
                .success(responseFlag == 0)
                .cellVoltage(scale(u16(payload, 1), 1000.0d))
                .internalResistance(u16(payload, 3))
                .cellTemperature(scale(s16(payload, 5), 10.0d))
                .leakageStatus(u8(payload, 7))
                .swollenVoltage(scale(u16(payload, 8), 10.0d))
                .build();
    }

    private BatteryModuleFrameData parseConnectResistanceVoltage(BatteryCollectorFrame frame) {
        byte[] payload = frame.getPayloadSafe();
        if (payload.length < 8) {
            return null;
        }
        return BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.CONNECT_RESISTANCE_VOLTAGE)
                .moduleAddress(frame.getAddress())
                .success(true)
                .connectBatteryVoltage(scale(u32(payload, 0), 10000.0d))
                .connectTestVoltage(scale(u32(payload, 4), 10000.0d))
                .build();
    }

    private BatteryModuleFrameData parseStatusResponse(BatteryCollectorFrame frame, BatteryDeviceProtocolCode protocolCode) {
        byte[] payload = frame.getPayloadSafe();
        if (payload.length < 1) {
            return null;
        }
        int statusCode = u8(payload, 0);
        return BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.STATUS_RESPONSE)
                .moduleAddress(frame.getAddress())
                .protocolCode(protocolCode.name())
                .responseFlag(statusCode)
                .statusCode(statusCode)
                .success(statusCode == 0)
                .build();
    }

    private BatteryModuleFrameData parseAutoSetAddressResponse(BatteryCollectorFrame frame, BatteryDeviceProtocolCode protocolCode) {
        byte[] payload = frame.getPayloadSafe();
        if (payload.length < 3) {
            return null;
        }
        int statusCode = u8(payload, 0);
        return BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.AUTO_SET_ADDRESS_RESPONSE)
                .moduleAddress(frame.getAddress())
                .protocolCode(protocolCode.name())
                .responseFlag(statusCode)
                .statusCode(statusCode)
                .success(statusCode == 0)
                .assignedModuleAddress(u8(payload, 1))
                .autoSetAddressStep(u8(payload, 2))
                .build();
    }

    private int u8(byte[] payload, int offset) {
        return payload[offset] & 0xFF;
    }

    private int u16(byte[] payload, int offset) {
        return ((payload[offset] & 0xFF) << 8) | (payload[offset + 1] & 0xFF);
    }

    private short s16(byte[] payload, int offset) {
        return (short) u16(payload, offset);
    }

    private long u32(byte[] payload, int offset) {
        return ((long) (payload[offset] & 0xFF) << 24)
                | ((long) (payload[offset + 1] & 0xFF) << 16)
                | ((long) (payload[offset + 2] & 0xFF) << 8)
                | (long) (payload[offset + 3] & 0xFF);
    }

    private Double scale(long raw, double divisor) {
        return raw / divisor;
    }
}
