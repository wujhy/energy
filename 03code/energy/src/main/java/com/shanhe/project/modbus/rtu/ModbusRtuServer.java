package com.shanhe.project.modbus.rtu;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.project.collector.battery.service.BatteryModuleModbusReadMappingService;
import com.shanhe.project.modbus.config.ModbusRtuProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Modbus RTU 从站服务。
 * <p>
 * 只支持读保持寄存器（功能码 0x03），不开放写寄存器。
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

    private volatile boolean running;
    private Thread serverThread;
    private SerialPort serialPort;

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
        serverThread = new Thread(this::runServer, "modbus-rtu-server");
        serverThread.setDaemon(true);
        serverThread.start();
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

            byte[] buffer = new byte[256];
            while (running) {
                int available = serialPort.bytesAvailable();
                if (available <= 0) {
                    Thread.sleep(10);
                    continue;
                }
                int read = serialPort.readBytes(buffer, Math.min(available, buffer.length));
                if (read > 0) {
                    processFrame(buffer, read);
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
        int stationAddress = buffer[0] & 0xFF;
        if (stationAddress != modbusRtuProperties.getStationAddress()) {
            return; // 非本站请求
        }
        int functionCode = buffer[1] & 0xFF;
        if (functionCode != FUNC_READ_HOLDING_REGISTERS) {
            sendException(stationAddress, functionCode, EXCEPTION_ILLEGAL_FUNCTION);
            return;
        }
        if (length < 8) {
            return; // 读保持寄存器请求：站号 + 功能码 + 起始地址(2) + 数量(2) + CRC(2)
        }
        int startAddress = ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF);
        int quantity = ((buffer[4] & 0xFF) << 8) | (buffer[5] & 0xFF);

        // Modbus 寄存器地址转文档参考号：寄存器地址 + 1 = 文档参考号
        int referenceAddress = startAddress + 1;

        // 从站号解析 packNum：低 4 位为电池组号
        Integer packNum = resolvePackNumFromStation(stationAddress);

        try {
            int[] values = readMappingService.readHoldingRegisters(packNum, referenceAddress, quantity);
            sendReadResponse(stationAddress, values);
        } catch (IllegalStateException e) {
            // 数据未就绪
            sendException(stationAddress, functionCode, EXCEPTION_SLAVE_DEVICE_FAILURE);
        } catch (IllegalArgumentException e) {
            // 非法地址或数量
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

    /** 从站号解析电池组号：低 4 位为电池组号。 */
    private Integer resolvePackNumFromStation(int stationAddress) {
        int packNum = stationAddress & 0x0F;
        return packNum > 0 ? packNum : null;
    }

    private void sendReadResponse(int stationAddress, int[] values) {
        int byteCount = values.length * 2;
        byte[] response = new byte[3 + byteCount + 2]; // 站号 + 功能码 + 字节数 + 数据 + CRC
        response[0] = (byte) stationAddress;
        response[1] = (byte) FUNC_READ_HOLDING_REGISTERS;
        response[2] = (byte) byteCount;
        for (int i = 0; i < values.length; i++) {
            response[3 + i * 2] = (byte) ((values[i] >> 8) & 0xFF);
            response[4 + i * 2] = (byte) (values[i] & 0xFF);
        }
        int crc = calculateCrc(response, response.length - 2);
        response[response.length - 2] = (byte) (crc & 0xFF);
        response[response.length - 1] = (byte) ((crc >> 8) & 0xFF);
        writeResponse(response);
    }

    private void sendException(int stationAddress, int functionCode, int exceptionCode) {
        byte[] response = new byte[5]; // 站号 + 功能码+0x80 + 异常码 + CRC(2)
        response[0] = (byte) stationAddress;
        response[1] = (byte) (functionCode | 0x80);
        response[2] = (byte) exceptionCode;
        int crc = calculateCrc(response, 3);
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

    /** Modbus CRC-16 计算。 */
    private int calculateCrc(byte[] data, int length) {
        int crc = 0xFFFF;
        for (int i = 0; i < length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
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
        if (serverThread != null) {
            serverThread.interrupt();
        }
        closePort();
    }
}
