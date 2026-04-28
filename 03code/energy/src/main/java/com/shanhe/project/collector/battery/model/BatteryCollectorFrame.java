package com.shanhe.project.collector.battery.model;

import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import lombok.Builder;
import lombok.Data;

import java.util.Arrays;

/**
 * 蓄电池 RS485 协议帧。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryCollectorFrame {

    /**
     * 帧头，固定为 ASCII START。
     */
    private byte[] start;

    /**
     * 模块地址。
     */
    private int address;

    /**
     * 命令码。
     */
    private int command;

    /**
     * 信息域长度。
     */
    private int length;

    /**
     * 信息域内容。
     */
    private byte[] payload;

    /**
     * 累加校验值。
     */
    private int check;

    /**
     * 帧尾，固定 0x0D。
     */
    private int end;

    /**
     * 转换为可直接写入串口的字节数组。
     *
     * @return 完整协议帧字节
     */
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

    /**
     * 转换为十六进制字符串，便于日志和联调。
     *
     * @return 十六进制帧内容
     */
    public String toHex() {
        return CodingUtil.bytesToHexString(toByteArray()).toUpperCase();
    }

    /**
     * 获取安全的载荷副本。
     *
     * @return 载荷副本
     */
    public byte[] getPayloadSafe() {
        return payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
    }
}
