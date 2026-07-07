package com.shanhe.project.manage.opt.service;

import cn.hutool.core.util.StrUtil;
import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.opt.config.BackupExternalModuleProperties;
import com.shanhe.project.modbus.rtu.ModbusRtuFrameParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * `_5` 备电时长外部模块直控服务。
 *
 * <p>本服务是 energy 主动下发到外部备电/开关模块的 Modbus RTU 主站能力，
 * 不复用 M460 `genCmd30/_E0` 代理链路，也不进入 600 单体采集命令队列。</p>
 *
 * @author wjh
 * @since 2026-07-07
 */
@Slf4j
@Service
public class BackupExternalModuleControlService {

    private static final int FUNC_WRITE_SINGLE_REGISTER = 0x06;
    private static final int WRITE_RESPONSE_LENGTH = 8;
    private static final int UNSIGNED_SHORT_MAX = 0xFFFF;

    /** 外部备电模块直控配置。 */
    @Resource
    private BackupExternalModuleProperties properties;

    /** 启动备电运行。 */
    public AjaxResult startBackup(Integer packNum) {
        return writeMode(packNum, true);
    }

    /** 停止备电运行。 */
    public AjaxResult stopBackup(Integer packNum) {
        return writeMode(packNum, false);
    }

    private AjaxResult writeMode(Integer packNum, boolean start) {
        String validation = validate(packNum);
        if (validation != null) {
            return AjaxResult.error(validation, 0);
        }
        int stationAddress = resolveStationAddress(packNum);
        int registerAddress = start
                ? properties.getBackupRunRegisterAddress()
                : properties.getBackupStopRegisterAddress();
        int writeValue = properties.getWriteValue();

        byte[] request = writeSingleRegisterFrame(stationAddress, registerAddress, writeValue);
        int attempts = Math.max(0, safe(properties.getRetryTimes(), 0)) + 1;
        for (int i = 1; i <= attempts; i++) {
            AjaxResult result = sendAndAwaitResponse(request, stationAddress, registerAddress, writeValue, start, i);
            if (isSuccess(result) || i == attempts) {
                return result;
            }
        }
        return AjaxResult.error("下发外部备电模块命令失败", 0);
    }

    private AjaxResult sendAndAwaitResponse(byte[] request,
                                            int stationAddress,
                                            int registerAddress,
                                            int writeValue,
                                            boolean start,
                                            int attempt) {
        SerialPort serialPort = null;
        try {
            serialPort = openSerialPort();
            int written = serialPort.writeBytes(request, request.length);
            if (written != request.length) {
                log.warn("外部备电模块命令写入不完整, station={}, register=0x{}, attempt={}, expected={}, actual={}",
                        stationAddress, Integer.toHexString(registerAddress), attempt, request.length, written);
                return AjaxResult.error("外部备电模块命令写入不完整", 0);
            }

            byte[] response = readResponse(serialPort);
            String error = validateWriteResponse(response, stationAddress, registerAddress, writeValue);
            if (error != null) {
                log.warn("外部备电模块响应无效, action={}, station={}, register=0x{}, attempt={}, error={}, response={}",
                        start ? "start" : "stop",
                        stationAddress,
                        Integer.toHexString(registerAddress),
                        attempt,
                        error,
                        bytesToHex(response));
                return AjaxResult.error(error, 0);
            }

            log.info("外部备电模块命令执行成功, action={}, station={}, register=0x{}, value=0x{}",
                    start ? "start" : "stop",
                    stationAddress,
                    Integer.toHexString(registerAddress),
                    Integer.toHexString(writeValue));
            return AjaxResult.success();
        } catch (Exception e) {
            log.warn("外部备电模块命令执行异常, action={}, station={}, register=0x{}, attempt={}, error={}",
                    start ? "start" : "stop",
                    stationAddress,
                    Integer.toHexString(registerAddress),
                    attempt,
                    e.getMessage());
            return AjaxResult.error("外部备电模块命令执行异常：" + e.getMessage(), 0);
        } finally {
            if (serialPort != null && serialPort.isOpen()) {
                serialPort.closePort();
            }
        }
    }

    private String validate(Integer packNum) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            return "外部备电模块直控未启用";
        }
        if (packNum == null || packNum <= 0) {
            return "电池组编号无效";
        }
        if (StrUtil.isBlank(properties.getPortName())) {
            return "外部备电模块串口未配置";
        }
        if (!validUnsignedShort(properties.getBackupRunRegisterAddress())
                || !validUnsignedShort(properties.getBackupStopRegisterAddress())
                || !validUnsignedShort(properties.getWriteValue())) {
            return "外部备电模块寄存器配置无效";
        }
        int stationAddress = resolveStationAddress(packNum);
        if (stationAddress < 1 || stationAddress > 247) {
            return "外部备电模块站号超出范围";
        }
        return null;
    }

    private SerialPort openSerialPort() {
        SerialPort serialPort = SerialPort.getCommPort(properties.getPortName());
        serialPort.setComPortParameters(
                safe(properties.getBaudRate(), 9600),
                safe(properties.getDataBits(), 8),
                safe(properties.getStopBits(), 1),
                safe(properties.getParity(), 0));
        int timeoutMs = safe(properties.getTimeoutMs(), 1000);
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                timeoutMs,
                timeoutMs);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!serialPort.openPort()) {
            throw new IllegalStateException("打开外部备电模块串口失败: " + properties.getPortName());
        }
        return serialPort;
    }

    private byte[] readResponse(SerialPort serialPort) {
        byte[] buffer = new byte[WRITE_RESPONSE_LENGTH];
        int read = serialPort.readBytes(buffer, buffer.length);
        if (read <= 0) {
            return null;
        }
        return read == buffer.length ? buffer : Arrays.copyOf(buffer, read);
    }

    private String validateWriteResponse(byte[] response, int stationAddress, int registerAddress, int writeValue) {
        if (response == null || response.length == 0) {
            return "外部备电模块无响应";
        }
        if (response.length >= 5 && (response[1] & 0x80) != 0) {
            return "外部备电模块返回异常码：" + (response[2] & 0xFF);
        }
        if (response.length != WRITE_RESPONSE_LENGTH) {
            return "外部备电模块响应长度无效";
        }
        if (!ModbusRtuFrameParser.isValidCrc(response)) {
            return "外部备电模块响应 CRC 校验失败";
        }
        if ((response[0] & 0xFF) != stationAddress || (response[1] & 0xFF) != FUNC_WRITE_SINGLE_REGISTER) {
            return "外部备电模块响应站号或功能码不匹配";
        }
        int actualRegister = ((response[2] & 0xFF) << 8) | (response[3] & 0xFF);
        int actualValue = ((response[4] & 0xFF) << 8) | (response[5] & 0xFF);
        if (actualRegister != registerAddress || actualValue != writeValue) {
            return "外部备电模块响应寄存器或写入值不匹配";
        }
        return null;
    }

    private byte[] writeSingleRegisterFrame(int stationAddress, int registerAddress, int value) {
        byte[] frame = new byte[] {
                (byte) stationAddress,
                (byte) FUNC_WRITE_SINGLE_REGISTER,
                (byte) ((registerAddress >> 8) & 0xFF),
                (byte) (registerAddress & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
        return ModbusRtuFrameParser.appendCrc(frame);
    }

    private int resolveStationAddress(Integer packNum) {
        if (Boolean.TRUE.equals(properties.getUseM460StationMapping())) {
            return 0x6E + packNum - 1;
        }
        return safe(properties.getStationAddress(), 0x6E);
    }

    private boolean isSuccess(AjaxResult result) {
        return result != null && Objects.equals(result.get(AjaxResult.CODE_TAG), AjaxResult.Type.SUCCESS.value());
    }

    private boolean validUnsignedShort(Integer value) {
        return value != null && value >= 0 && value <= UNSIGNED_SHORT_MAX;
    }

    private int safe(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02X", value & 0xFF));
        }
        return builder.toString();
    }
}
