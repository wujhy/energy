package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.springframework.stereotype.Service;

/**
 * 600节模块端帧标准数据解析服务。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Service
public class BatteryModuleFrameDataParserService {

    /**
     * 电流温度模块固定地址。
     */
    private static final int ARRAY_MODULE_ADDRESS = 246;

    /**
     * 将 600 节模块端响应帧解析为标准数据对象。
     *
     * @param frame 响应帧
     * @return 标准解析数据；无法解析时返回 null
     */
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
        // 01/81 中地址 246 是电流温度模块，1..245 才是单体模块。
        if (address == ARRAY_MODULE_ADDRESS) {
            if (payload.length < 11) {
                return null;
            }
            int responseFlag = u8(payload, 0);
            BatteryModuleFrameData.BatteryModuleFrameDataBuilder builder = BatteryModuleFrameData.builder()
                    .type(BatteryModuleDataType.ARRAY_MODULE_INFO)
                    .moduleAddress(address)
                    .responseFlag(responseFlag)
                    .success(responseFlag == 0);
            if (responseFlag != 0) {
                return builder.build();
            }
            return builder
                    .chargeDischargeCurrent(scale(s16(payload, 1), 10.0d))
                    .floatCurrent(scale(s16(payload, 3), 1000.0d))
                    .externalVoltage(scale(nonNegativeS16(payload, 5), 100.0d))
                    .environmentTemperature1(scale(s16(payload, 7), 10.0d))
                    .environmentTemperature2(scale(s16(payload, 9), 10.0d))
                    .build();
        }
        if (address < 1 || address > 245 || payload.length < 10) {
            return null;
        }
        int responseFlag = u8(payload, 0);
        BatteryModuleFrameData.BatteryModuleFrameDataBuilder builder = BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.SINGLE_MODULE_INFO)
                .moduleAddress(address)
                .responseFlag(responseFlag)
                .success(responseFlag == 0);
        if (responseFlag != 0) {
            return builder.build();
        }
        return builder
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

    private int nonNegativeS16(byte[] payload, int offset) {
        return Math.max(s16(payload, offset), 0);
    }

    private long u32(byte[] payload, int offset) {
        // 协议字段按高字节在前解析。
        return ((long) (payload[offset] & 0xFF) << 24)
                | ((long) (payload[offset + 1] & 0xFF) << 16)
                | ((long) (payload[offset + 2] & 0xFF) << 8)
                | (long) (payload[offset + 3] & 0xFF);
    }

    private Double scale(long raw, double divisor) {
        return raw / divisor;
    }
}
