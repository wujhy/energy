package com.shanhe.project.modbus.rtu;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Modbus RTU request frame parser.
 *
 * @author wjh
 * @since 2026-06-15
 */
public class ModbusRtuFrameParser {

    /** Modbus 读保持寄存器功能码（0x03）。 */
    private static final int FUNC_READ_HOLDING_REGISTERS = 0x03;
    /** Modbus 写单个寄存器功能码（0x06）。 */
    private static final int FUNC_WRITE_SINGLE_REGISTER = 0x06;
    /** Modbus 写多个寄存器功能码（0x10）。 */
    private static final int FUNC_WRITE_MULTIPLE_REGISTERS = 0x10;
    /** 读保持寄存器和写单个寄存器请求帧的固定长度（字节）。 */
    private static final int FIXED_REQUEST_LENGTH = 8;
    /** Modbus RTU 帧最小长度（站号 + 功能码 + CRC）。 */
    private static final int MIN_FRAME_LENGTH = 4;

    private final int maxBufferSize;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public ModbusRtuFrameParser(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize <= 0 ? 256 : maxBufferSize;
    }

    public List<byte[]> append(byte[] data, int length) {
        if (data == null || length <= 0) {
            return new ArrayList<>();
        }
        int writeLength = Math.min(length, data.length);
        buffer.write(data, 0, writeLength);
        if (buffer.size() > maxBufferSize) {
            byte[] bytes = buffer.toByteArray();
            reset(Arrays.copyOfRange(bytes, bytes.length - maxBufferSize, bytes.length));
        }
        return drainFrames();
    }

    public void clear() {
        buffer.reset();
    }

    private List<byte[]> drainFrames() {
        List<byte[]> frames = new ArrayList<>();
        byte[] bytes = buffer.toByteArray();
        int offset = 0;
        while (bytes.length - offset >= MIN_FRAME_LENGTH) {
            int frameLength = resolveFrameLength(bytes, offset, bytes.length - offset);
            if (frameLength < 0) {
                offset++;
                continue;
            }
            if (frameLength == 0 || bytes.length - offset < frameLength) {
                break;
            }
            byte[] frame = Arrays.copyOfRange(bytes, offset, offset + frameLength);
            if (isValidCrc(frame)) {
                frames.add(frame);
                offset += frameLength;
            } else {
                offset++;
            }
        }
        reset(Arrays.copyOfRange(bytes, offset, bytes.length));
        return frames;
    }

    private int resolveFrameLength(byte[] bytes, int offset, int available) {
        int functionCode = bytes[offset + 1] & 0xFF;
        if (functionCode == FUNC_READ_HOLDING_REGISTERS
                || functionCode == FUNC_WRITE_SINGLE_REGISTER) {
            return FIXED_REQUEST_LENGTH;
        }
        if (functionCode == FUNC_WRITE_MULTIPLE_REGISTERS) {
            if (available < 7) {
                return 0;
            }
            int byteCount = bytes[offset + 6] & 0xFF;
            return 9 + byteCount;
        }
        return available >= FIXED_REQUEST_LENGTH ? FIXED_REQUEST_LENGTH : 0;
    }

    public static boolean isValidCrc(byte[] frame) {
        if (frame == null || frame.length < MIN_FRAME_LENGTH) {
            return false;
        }
        int expected = calculateCrc(frame, frame.length - 2);
        int actual = (frame[frame.length - 2] & 0xFF) | ((frame[frame.length - 1] & 0xFF) << 8);
        return expected == actual;
    }

    public static byte[] appendCrc(byte[] frameWithoutCrc) {
        byte[] frame = Arrays.copyOf(frameWithoutCrc, frameWithoutCrc.length + 2);
        int crc = calculateCrc(frame, frameWithoutCrc.length);
        frame[frame.length - 2] = (byte) (crc & 0xFF);
        frame[frame.length - 1] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    public static int calculateCrc(byte[] data, int length) {
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

    private void reset(byte[] remaining) {
        buffer.reset();
        if (remaining != null && remaining.length > 0) {
            buffer.write(remaining, 0, remaining.length);
        }
    }
}
