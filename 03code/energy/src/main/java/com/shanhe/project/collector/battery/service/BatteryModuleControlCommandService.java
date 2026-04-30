package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.springframework.stereotype.Service;

/**
 * 600节模块端显式控制命令构造服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Service
public class BatteryModuleControlCommandService {

    /**
     * 构造单体内阻测试命令。
     *
     * @param moduleAddress 单体模块地址
     * @return 控制命令
     */
    public BatteryModuleControlCommand singleBatteryInternalResistanceTest(int moduleAddress) {
        validateModuleAddress(moduleAddress);
        return command(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST, moduleAddress);
    }

    /**
     * 构造单体均衡控制命令。
     *
     * @param moduleAddress 单体模块地址
     * @param balanceValue 均衡控制值
     * @return 控制命令
     */
    public BatteryModuleControlCommand singleBatteryBalance(int moduleAddress, int balanceValue) {
        validateModuleAddress(moduleAddress);
        return command(BatteryDeviceProtocolCode.SINGLE_BATTERY_BALANCE, moduleAddress, balanceValue);
    }

    /**
     * 构造设置模块地址命令。
     *
     * @param moduleAddress 当前模块地址
     * @param newAddress 新模块地址
     * @return 控制命令
     */
    public BatteryModuleControlCommand setModuleAddress(int moduleAddress, int newAddress) {
        validateModuleAddress(moduleAddress);
        validateModuleAddress(newAddress);
        return command(BatteryDeviceProtocolCode.SET_MODULE_ADDRESS, moduleAddress, newAddress);
    }

    /**
     * 构造清除单体调试数据命令。
     *
     * @param parameter 协议定义的1字节参数
     * @return 控制命令
     */
    public BatteryModuleControlCommand clearSingleDebugData(int parameter) {
        return command(BatteryDeviceProtocolCode.CLEAR_SINGLE_DEBUG_DATA, 0, parameter);
    }

    /**
     * 构造连接条电阻测试启动命令。
     *
     * @return 控制命令
     */
    public BatteryModuleControlCommand connectStripResistanceTest() {
        return command(BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST, 0);
    }

    /**
     * 构造读取连接条电阻测试电压命令。
     *
     * @param moduleAddress 单体模块地址
     * @return 控制命令
     */
    public BatteryModuleControlCommand getConnectResistanceVoltage(int moduleAddress) {
        validateModuleAddress(moduleAddress);
        return command(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE, moduleAddress);
    }

    /**
     * 构造设置内阻系数命令。
     *
     * @param moduleAddress 模块地址；广播使用0
     * @param payloadBytes 4字节浮点数原始协议参数，字节序需由协议和现场联调确认
     * @return 控制命令
     */
    public BatteryModuleControlCommand setInternalResistanceCoefficient(int moduleAddress, int... payloadBytes) {
        validatePayloadLength(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, payloadBytes, 4);
        return command(BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT, moduleAddress, payloadBytes);
    }

    /**
     * 构造自动设置模块地址命令。
     *
     * @param address 目标地址或广播地址
     * @param payloadBytes 7字节协议参数
     * @return 控制命令
     */
    public BatteryModuleControlCommand autoSetModuleAddress(int address, int... payloadBytes) {
        validatePayloadLength(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS, payloadBytes, 7);
        return command(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS, address, payloadBytes);
    }

    /**
     * 构造校准参数命令。
     *
     * @param moduleAddress 模块地址
     * @param payloadBytes 4字节或6字节校准参数
     * @return 控制命令
     */
    public BatteryModuleControlCommand setCalibrationParameter(int moduleAddress, int... payloadBytes) {
        validateModuleAddress(moduleAddress);
        if (payloadBytes == null || (payloadBytes.length != 4 && payloadBytes.length != 6)) {
            throw new IllegalArgumentException("SET_CALIBRATION_PARAMETER payload length must be 4 or 6");
        }
        return command(BatteryDeviceProtocolCode.SET_CALIBRATION_PARAMETER, moduleAddress, payloadBytes);
    }

    private BatteryModuleControlCommand command(BatteryDeviceProtocolCode protocolCode, int address, int... payloadBytes) {
        if (protocolCode == null) {
            throw new IllegalArgumentException("protocolCode must not be null");
        }
        validateModuleAddressOrBroadcast(address);
        return BatteryModuleControlCommand.builder()
                .protocolCode(protocolCode)
                .address(address)
                .payload(toPayload(payloadBytes))
                .requestCode(protocolCode.getRequestCode())
                .responseCode(resolveResponseCode(protocolCode, address))
                .description(protocolCode.getDescription())
                .build();
    }

    private Integer resolveResponseCode(BatteryDeviceProtocolCode protocolCode, int address) {
        if (protocolCode == BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT && address == 0) {
            return null;
        }
        return protocolCode.getResponseCode();
    }

    private byte[] toPayload(int... payloadBytes) {
        if (payloadBytes == null || payloadBytes.length == 0) {
            return new byte[0];
        }
        byte[] payload = new byte[payloadBytes.length];
        for (int i = 0; i < payloadBytes.length; i++) {
            payload[i] = (byte) validateByte(payloadBytes[i]);
        }
        return payload;
    }

    private void validatePayloadLength(BatteryDeviceProtocolCode protocolCode, int[] payloadBytes, int length) {
        if (payloadBytes == null || payloadBytes.length != length) {
            throw new IllegalArgumentException(protocolCode.name() + " payload length must be " + length);
        }
    }

    private void validateModuleAddress(int address) {
        if (address < 1 || address > 246) {
            throw new IllegalArgumentException("module address must be between 1 and 246");
        }
    }

    private void validateModuleAddressOrBroadcast(int address) {
        if (address < 0 || address > 246) {
            throw new IllegalArgumentException("module address must be between 0 and 246");
        }
    }

    private int validateByte(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("payload byte must be between 0 and 255");
        }
        return value;
    }
}
