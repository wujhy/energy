package com.shanhe.project.modbus.rtu;

import com.fazecast.jSerialComm.SerialPort;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.shanhe.project.collector.battery.service.BatteryModuleModbusReadMappingService;
import com.shanhe.project.modbus.config.ModbusRtuProperties;
import com.shanhe.project.modbus.service.ModbusIllegalDataAddressException;
import com.shanhe.project.modbus.service.ModbusIllegalDataValueException;
import com.shanhe.project.modbus.service.ModbusWriteMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Modbus RTU 从站服务。
 * <p>
 * 支持读保持寄存器（功能码 0x03）和已映射的写单寄存器（功能码 0x06），
 * 写操作必须经 ModbusWriteMappingService 转为内部控制服务调用。
 * 默认关闭，通过 battery-collector.modbus-rtu.enabled 启用。
 *
 * @author wjh
 * @since 2026-06-03
 */
@Slf4j
@Order(10)
@Component
public class ModbusRtuServer implements ApplicationRunner {

    /** Modbus 功能码：读保持寄存器。 */
    private static final int FUNC_READ_HOLDING_REGISTERS = 0x03;
    /** Modbus 功能码：写单个寄存器。 */
    private static final int FUNC_WRITE_SINGLE_REGISTER = 0x06;
    private static final int FUNC_WRITE_MULTIPLE_REGISTERS = 0x10;

    /** Modbus 异常码：非法功能码。 */
    private static final int EXCEPTION_ILLEGAL_FUNCTION = 0x01;
    /** Modbus 异常码：非法数据地址。 */
    private static final int EXCEPTION_ILLEGAL_DATA_ADDRESS = 0x02;
    /** Modbus 异常码：非法数据值。 */
    private static final int EXCEPTION_ILLEGAL_DATA_VALUE = 0x03;
    /** Modbus 异常码：从站设备故障。 */
    private static final int EXCEPTION_SLAVE_DEVICE_FAILURE = 0x04;

    @Resource
    private ModbusRtuProperties modbusRtuProperties;

    @Resource
    private BatteryModuleModbusReadMappingService readMappingService;

    @Resource
    private ModbusWriteMappingService writeMappingService;

    private volatile boolean running;
    private ExecutorService serverExecutor;
    private SerialPort serialPort;
    private ModbusRtuFrameParser frameParser;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(modbusRtuProperties.getEnabled())) {
            log.info("Modbus RTU 从站未启用");
            return;
        }
        if (modbusRtuProperties.getPortName() == null || modbusRtuProperties.getPortName().trim().isEmpty()) {
            log.warn("Modbus RTU 从站串口名称未配置");
            return;
        }

        running = true;
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("modbus-rtu-server").setDaemon(true).build();
        serverExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1), threadFactory, new ThreadPoolExecutor.AbortPolicy());
        serverExecutor.submit(this::runServer);
        log.info("Modbus RTU 从站已启动, 串口={}, 站号={}, 波特率={}",
                modbusRtuProperties.getPortName(),
                modbusRtuProperties.getStationAddress(),
                modbusRtuProperties.getBaudRate());
    }

    private void runServer() {
        try {
            serialPort = SerialPort.getCommPort(modbusRtuProperties.getPortName());
            serialPort.setComPortParameters(
                    modbusRtuProperties.getBaudRate(),
                    modbusRtuProperties.getDataBits(),
                    modbusRtuProperties.getStopBits(),
                    modbusRtuProperties.getParity());
            serialPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                    modbusRtuProperties.getReadTimeoutMs(),
                    modbusRtuProperties.getReadTimeoutMs());
            if (!serialPort.openPort()) {
                log.error("Modbus RTU 从站打开串口失败: {}", modbusRtuProperties.getPortName());
                return;
            }
            log.info("Modbus RTU 从站串口已打开: {}", modbusRtuProperties.getPortName());

            frameParser = new ModbusRtuFrameParser(modbusRtuProperties.getMaxFrameBufferSize());
            byte[] buffer = new byte[256];
            while (running) {
                int available = serialPort.bytesAvailable();
                if (available <= 0) {
                    Thread.sleep(10);
                    continue;
                }
                int read = serialPort.readBytes(buffer, Math.min(available, buffer.length));
                if (read > 0) {
                    List<byte[]> frames = frameParser.append(buffer, read);
                    for (byte[] frame : frames) {
                        processFrame(frame, frame.length);
                    }
                }
            }
        } catch (Exception e) {
            if (running) {
                log.error("Modbus RTU 从站异常", e);
            }
        } finally {
            closePort();
        }
    }

    private void processFrame(byte[] buffer, int length) {
        if (length < 4) {
            return; // 最小帧：站号 + 功能码 + CRC(2)
        }
        if (!ModbusRtuFrameParser.isValidCrc(buffer)) {
            return;
        }
        int stationAddress = buffer[0] & 0xFF;
        if (stationAddress == 0) {
            return;
        }
        if (stationAddress != modbusRtuProperties.getStationAddress()) {
            return; // 非本站请求
        }
        int functionCode = buffer[1] & 0xFF;
        Integer packNum = resolvePackNumFromStation(stationAddress);

        switch (functionCode) {
            case FUNC_READ_HOLDING_REGISTERS:
                processReadHoldingRegisters(stationAddress, functionCode, packNum, buffer, length);
                break;
            case FUNC_WRITE_SINGLE_REGISTER:
                processWriteSingleRegister(stationAddress, functionCode, packNum, buffer, length);
                break;
            case FUNC_WRITE_MULTIPLE_REGISTERS:
                sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_FUNCTION);
                break;
            default:
                sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_FUNCTION);
                break;
        }
    }

    private void processReadHoldingRegisters(int stationAddress, int functionCode,
                                              Integer packNum, byte[] buffer, int length) {
        if (length < 8) {
            return;
        }
        int startAddress = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int quantity = ((buffer[4] & 0xFF) << 8) | (buffer[5] & 0xFF);
        if (quantity <= 0 || quantity > 125) {
            sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_VALUE);
            return;
        }
        int referenceAddress = startAddress + 1;

        try {
            int[] values = readMappingService.readHoldingRegisters(packNum, referenceAddress, quantity);
            sendReadResponse(stationAddress, values);
        } catch (IllegalStateException e) {
            sendException(stationAddress, functionCode, EXCEPTION_SLAVE_DEVICE_FAILURE);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("不支持")) {
                sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_ADDRESS);
            } else {
                sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_VALUE);
            }
        } catch (Exception e) {
            log.warn("Modbus RTU 读取异常: {}", e.getMessage());
            sendException(stationAddress, functionCode, EXCEPTION_SLAVE_DEVICE_FAILURE);
        }
    }

    private void processWriteSingleRegister(int stationAddress, int functionCode,
                                             Integer packNum, byte[] buffer, int length) {
        if (length < 8) {
            return;
        }
        int registerAddress = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int value = ((buffer[4] & 0xFF) << 8) | (buffer[5] & 0xFF);
        int referenceAddress = registerAddress + 1;

        try {
            boolean success = writeMappingService.writeSingleRegister(packNum, referenceAddress, value);
            if (success) {
                // 写成功：回显请求帧
                sendWriteResponse(stationAddress, functionCode, registerAddress, value);
            } else {
                sendException(stationAddress, functionCode, EXCEPTION_SLAVE_DEVICE_FAILURE);
            }
        } catch (ModbusIllegalDataValueException e) {
            sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_VALUE);
        } catch (ModbusIllegalDataAddressException e) {
            sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_ADDRESS);
        } catch (IllegalArgumentException e) {
            sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_DATA_VALUE);
        } catch (Exception e) {
            log.warn("Modbus RTU 写入异常: {}", e.getMessage());
            sendException(stationAddress, functionCode, EXCEPTION_SLAVE_DEVICE_FAILURE);
        }
    }

    private void sendWriteResponse(int stationAddress, int functionCode, int registerAddress, int value) {
        // 站号 + 功能码 + 寄存器地址(2) + 值(2) + CRC(2)
        byte[] response = new byte[8];
        response[0] = (byte) stationAddress;
        response[1] = (byte) functionCode;
        response[2] = (byte) ((registerAddress >> 8) & 0xFF);
        response[3] = (byte) (registerAddress & 0xFF);
        response[4] = (byte) ((value >> 8) & 0xFF);
        response[5] = (byte) (value & 0xFF);
        int crc = ModbusRtuFrameParser.calculateCrc(response, 6);
        response[6] = (byte) (crc & 0xFF);
        response[7] = (byte) ((crc >> 8) & 0xFF);
        writeResponse(response);
    }

    /** 从站号解析电池组号：低 4 位为电池组号。 */
    private Integer resolvePackNumFromStation(int stationAddress) {
        if (modbusRtuProperties.getStationPackMap() != null
                && modbusRtuProperties.getStationPackMap().containsKey(stationAddress)) {
            return modbusRtuProperties.getStationPackMap().get(stationAddress);
        }
        int packNum = stationAddress & 0x0F;
        return packNum > 0 ? packNum : null;
    }

    private void sendReadResponse(int stationAddress, int[] values) {
        int byteCount = values.length * 2;
        // 站号 + 功能码 + 字节数 + 数据 + CRC
        byte[] response = new byte[3 + byteCount + 2];
        response[0] = (byte) stationAddress;
        response[1] = (byte) FUNC_READ_HOLDING_REGISTERS;
        response[2] = (byte) byteCount;
        for (int i = 0; i < values.length; i++) {
            response[3 + i * 2] = (byte) ((values[i] >> 8) & 0xFF);
            response[4 + i * 2] = (byte) (values[i] & 0xFF);
        }
        int crc = ModbusRtuFrameParser.calculateCrc(response, response.length - 2);
        response[response.length - 2] = (byte) (crc & 0xFF);
        response[response.length - 1] = (byte) ((crc >> 8) & 0xFF);
        writeResponse(response);
    }

    private void sendException(int stationAddress, int functionCode, int exceptionCode) {
        // 站号 + 功能码+0x80 + 异常码 + CRC(2)
        byte[] response = new byte[5];
        response[0] = (byte) stationAddress;
        response[1] = (byte) (functionCode | 0x80);
        response[2] = (byte) exceptionCode;
        int crc = ModbusRtuFrameParser.calculateCrc(response, 3);
        response[3] = (byte) (crc & 0xFF);
        response[4] = (byte) ((crc >> 8) & 0xFF);
        writeResponse(response);
    }

    private void writeResponse(byte[] response) {
        if (serialPort == null || !serialPort.isOpen()) {
            return;
        }
        try {
            serialPort.writeBytes(response, response.length);
        } catch (Exception e) {
            log.warn("Modbus RTU 写入响应失败: {}", e.getMessage());
        }
    }

    private void closePort() {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.closePort();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 停止 Modbus RTU 从站。
     */
    public void stop() {
        running = false;
        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }
        closePort();
    }
}
