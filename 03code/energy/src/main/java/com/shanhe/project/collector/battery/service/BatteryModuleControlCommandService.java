package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.GROUP_MODULE_ADDRESS;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.UNSIGNED_BYTE_MAX;
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
        validateCellModuleAddress(moduleAddress);
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
        validateCellModuleAddress(moduleAddress);
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
        validateCellModuleAddress(moduleAddress);
        validateCellModuleAddress(newAddress);
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
        validateCellModuleAddress(moduleAddress);
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
        validateCellModuleAddress(moduleAddress);
        boolean invalidLength = payloadBytes == null || (payloadBytes.length != 4 && payloadBytes.length != 6);
        if (invalidLength) {
            throw new IllegalArgumentException("校准参数载荷长度必须为4或6字节");
        }
        return command(BatteryDeviceProtocolCode.SET_CALIBRATION_PARAMETER, moduleAddress, payloadBytes);
    }

    /** 根据协议编码和地址构造模块控制命令。 */
    private BatteryModuleControlCommand command(BatteryDeviceProtocolCode protocolCode, int address, int... payloadBytes) {
        if (protocolCode == null) {
            throw new IllegalArgumentException("协议编码不能为空");
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

    /** 解析响应码，广播设置内阻系数时无响应码。 */
    private Integer resolveResponseCode(BatteryDeviceProtocolCode protocolCode, int address) {
        if (protocolCode == BatteryDeviceProtocolCode.SET_INTERNAL_RESISTANCE_COEFFICIENT && address == 0) {
            return null;
        }
        return protocolCode.getResponseCode();
    }

    /** 将int数组转换为byte数组载荷。 */
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

    /** 校验载荷长度是否符合协议要求。 */
    private void validatePayloadLength(BatteryDeviceProtocolCode protocolCode, int[] payloadBytes, int length) {
        if (payloadBytes == null || payloadBytes.length != length) {
            throw new IllegalArgumentException(protocolCode.name() + " 载荷长度必须为 " + length);
        }
    }

    /** 校验普通单体模块地址（1-245，不含 246 组模块地址）。 */
    private void validateCellModuleAddress(int address) {
        if (address < 1 || address >= GROUP_MODULE_ADDRESS) {
            throw new IllegalArgumentException("单体模块地址必须在1到245之间，不允许使用246");
        }
    }

    /** 校验模块地址是否在0到246之间（含广播地址0）。 */
    private void validateModuleAddressOrBroadcast(int address) {
        if (address < 0 || address > GROUP_MODULE_ADDRESS) {
            throw new IllegalArgumentException("模块地址必须在0到246之间");
        }
    }

    private int validateByte(int value) {
        if (value < 0 || value > UNSIGNED_BYTE_MAX) {
            throw new IllegalArgumentException("载荷字节值必须在0到255之间");
        }
        return value;
    }
}
