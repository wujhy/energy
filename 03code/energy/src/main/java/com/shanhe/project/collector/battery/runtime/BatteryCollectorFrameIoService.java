package com.shanhe.project.collector.battery.runtime;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;

/**
 * 蓄电池采集串口帧收发协调服务。
 *
 * <p>只负责与串口/帧的底层交互：帧发送、字节接收、接收缓冲管理。
 * 不负责业务数据解释、命令队列、超时判断或状态持久化。</p>
 *
 * @author wjh
 * @since 2026-06-18
 */
@Slf4j
@Component
public class BatteryCollectorFrameIoService {

    @Resource
    private BatteryCollectorFrameCodec frameCodec;

    /**
     * 将帧序列化后写入串口。
     *
     * @param serialPort 已打开的串口
     * @param frame 待发送的帧
     * @return 实际写入字节数，失败返回 -1
     */
    public int writeFrameBytes(SerialPort serialPort, BatteryCollectorFrame frame) {
        if (serialPort == null || !serialPort.isOpen() || frame == null) {
            return -1;
        }
        byte[] bytes = frame.toByteArray();
        int written = serialPort.writeBytes(bytes, bytes.length);
        if (written != bytes.length) {
            log.warn("蓄电池帧写入不完整, 预期={}, 实际={}", bytes.length, written);
        }
        return written;
    }

    /**
     * 从串口读取所有可用字节。
     *
     * @param serialPort 已打开的串口
     * @param minBufferSize 最小读取缓冲区大小
     * @return 读取到的字节，无数据返回 null
     */
    public byte[] readAvailableBytes(SerialPort serialPort, int minBufferSize) {
        if (serialPort == null || !serialPort.isOpen() || serialPort.bytesAvailable() <= 0) {
            return null;
        }
        int available = serialPort.bytesAvailable();
        int size = Math.max(available, minBufferSize);
        byte[] buffer = new byte[size];
        int read = serialPort.readBytes(buffer, Math.min(size, available));
        if (read <= 0) {
            return null;
        }
        if (read == buffer.length) {
            return buffer;
        }
        byte[] result = new byte[read];
        System.arraycopy(buffer, 0, result, 0, read);
        return result;
    }

    /**
     * 追加字节到接收缓冲区并在超过限制时裁剪。
     *
     * @param receiveBuffer 接收缓冲区
     * @param data 待追加的字节
     * @param bufferLimit 缓冲区最大长度
     */
    public void appendAndTrimBuffer(ByteArrayOutputStream receiveBuffer, byte[] data, int bufferLimit) {
        if (receiveBuffer == null || data == null || data.length == 0) {
            return;
        }
        receiveBuffer.write(data, 0, data.length);
        trimBuffer(receiveBuffer, bufferLimit);
    }

    /**
     * 裁剪接收缓冲区到限制范围内，保留尾部数据。
     *
     * @param receiveBuffer 接收缓冲区
     * @param bufferLimit 缓冲区最大长度
     */
    public void trimBuffer(ByteArrayOutputStream receiveBuffer, int bufferLimit) {
        if (receiveBuffer == null || bufferLimit <= 0) {
            return;
        }
        if (receiveBuffer.size() <= bufferLimit) {
            return;
        }
        byte[] all = receiveBuffer.toByteArray();
        int keep = Math.min(all.length, bufferLimit);
        receiveBuffer.reset();
        receiveBuffer.write(all, all.length - keep, keep);
        log.warn("蓄电池接收缓冲区已裁剪, 保留字节={}", keep);
    }

    /**
     * 从接收缓冲区解码帧，保留未消费的剩余字节。
     *
     * @param receiveBuffer 接收缓冲区
     * @return 解码结果（含帧列表和剩余字节）
     */
    public BatteryCollectorFrameCodec.DecodeResult decodeBuffer(ByteArrayOutputStream receiveBuffer) {
        byte[] source = receiveBuffer.toByteArray();
        BatteryCollectorFrameCodec.DecodeResult result = frameCodec.decode(source, source.length);
        receiveBuffer.reset();
        byte[] remaining = result.getRemaining();
        if (remaining.length > 0) {
            receiveBuffer.write(remaining, 0, remaining.length);
        }
        return result;
    }

    /**
     * 配置并打开串口。
     *
     * @param config 通道配置
     * @return 已打开的串口，失败抛出异常
     */
    public SerialPort openSerialPort(BatteryCollectorChannelConfig config) {
        SerialPort serialPort = SerialPort.getCommPort(config.getPortName());
        serialPort.setComPortParameters(
                config.getBaudRate() != null ? config.getBaudRate() : 9600,
                config.getDataBits() != null ? config.getDataBits() : 8,
                config.getStopBits() != null ? config.getStopBits() : 1,
                config.getParity() != null ? config.getParity() : 0);
        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                config.getTimeoutMs() != null ? config.getTimeoutMs() : 1000,
                config.getTimeoutMs() != null ? config.getTimeoutMs() : 1000);
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!serialPort.openPort()) {
            throw new IllegalStateException("打开串口失败: " + config.getPortName());
        }
        return serialPort;
    }

    /**
     * 检查串口是否已打开。
     *
     * @param serialPort 串口
     * @return 是否打开
     */
    public boolean isSerialPortOpen(SerialPort serialPort) {
        return serialPort != null && serialPort.isOpen();
    }

    /**
     * 静默关闭串口。
     *
     * @param serialPort 串口，可为 null
     */
    public void closeQuietly(SerialPort serialPort) {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.closePort();
            } catch (Exception e) {
                log.debug("关闭串口异常: {}", e.getMessage());
            }
        }
    }

    /**
     * 字节数组转十六进制字符串。
     *
     * @param bytes 字节数组
     * @param length 有效长度
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes, int length) {
        if (bytes == null || length <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = 0; i < length && i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return sb.toString();
    }
}
