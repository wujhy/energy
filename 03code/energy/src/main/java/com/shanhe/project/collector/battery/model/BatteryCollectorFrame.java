package com.shanhe.project.collector.battery.model;

import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;

/**
 * Battery RS485 协议帧
 */
@Data
@Builder
public class BatteryCollectorFrame {

    private byte[] start;

    private int address;

    private int command;

    private int length;

    private byte[] payload;

    private int check;

    private int end;

    public byte[] toByteArray() {
        byte[] value = payload == null ? new byte[0] : payload;
        byte[] frame = new byte[5 + 1 + 1 + 1 + value.length + 1 + 1];
        int offset = 0;
        System.arraycopy(start, 0, frame, offset, start.length);
        offset += start.length;
        frame[offset++] = (byte) address;
        frame[offset++] = (byte) command;
        frame[offset++] = (byte) length;
        System.arraycopy(value, 0, frame, offset, value.length);
        offset += value.length;
        frame[offset++] = (byte) check;
        frame[offset] = (byte) end;
        return frame;
    }

    public String toHex() {
        return CodingUtil.bytesToHexString(toByteArray()).toUpperCase();
    }

    public byte[] getPayloadSafe() {
        return payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }
}
