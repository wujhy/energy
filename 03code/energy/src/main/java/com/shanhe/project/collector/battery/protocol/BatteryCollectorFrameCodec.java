package com.shanhe.project.collector.battery.protocol;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 蓄电池 RS485 帧编解码器。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Component
public class BatteryCollectorFrameCodec {

    /**
     * 协议固定帧头。
     */
    private static final byte[] START = "START".getBytes(StandardCharsets.US_ASCII);

    /**
     * 无载荷时的最小完整帧长度。
     */
    private static final int FIXED_LENGTH = 10;

    /**
     * 构造 600 节模块端请求帧。
     *
     * @param address 模块地址
     * @param command 请求命令码
     * @param payload 信息域
     * @return 请求帧
     */
    public BatteryCollectorFrame buildRequest(int address, int command, byte[] payload) {
        byte[] value = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        return BatteryCollectorFrame.builder()
                .start(Arrays.copyOf(START, START.length))
                .address(address & 0xFF)
                .command(command & 0xFF)
                .length(value.length)
                .payload(value)
                .check(countCheck(address & 0xFF, command & 0xFF, value.length, value))
                .end(0x0D)
                .build();
    }

    /**
     * 从接收缓冲中解析完整帧，保留半包残留。
     *
     * @param source 接收字节
     * @param length 有效长度
     * @return 解码结果
     */
    public DecodeResult decode(byte[] source, int length) {
        List<BatteryCollectorFrame> frames = new ArrayList<>();
        if (source == null || length <= 0) {
            return DecodeResult.builder()
                    .frames(frames)
                    .remaining(new byte[0])
                    .build();
        }

        int limit = Math.min(source.length, length);
        if (limit < FIXED_LENGTH) {
            return DecodeResult.builder()
                    .frames(frames)
                    .remaining(Arrays.copyOf(source, limit))
                    .build();
        }

        int index = 0;
        int remainingStart = limit;
        while (index <= limit - FIXED_LENGTH) {
            if (!matchStart(source, index, limit)) {
                index++;
                continue;
            }

            int cursor = index + START.length;
            int address = source[cursor++] & 0xFF;
            int command = source[cursor++] & 0xFF;
            int payloadLength = source[cursor++] & 0xFF;
            int frameLength = START.length + 1 + 1 + 1 + payloadLength + 1 + 1;
            if (index + frameLength > limit) {
                remainingStart = index;
                break;
            }

            byte[] payload = new byte[payloadLength];
            if (payloadLength > 0) {
                System.arraycopy(source, cursor, payload, 0, payloadLength);
            }
            cursor += payloadLength;
            int check = source[cursor++] & 0xFF;
            int end = source[cursor] & 0xFF;

            if (end == 0x0D && check == countCheck(address, command, payloadLength, payload)) {
                frames.add(BatteryCollectorFrame.builder()
                        .start(Arrays.copyOf(START, START.length))
                        .address(address)
                        .command(command)
                        .length(payloadLength)
                        .payload(payload)
                        .check(check)
                        .end(end)
                        .build());
                index += frameLength;
                remainingStart = index;
            } else {
                index++;
            }
        }

        if (remainingStart == limit && index < limit && limit - index < FIXED_LENGTH) {
            remainingStart = index;
        }
        byte[] remaining = remainingStart >= limit ? new byte[0] : Arrays.copyOfRange(source, remainingStart, limit);
        return DecodeResult.builder()
                .frames(frames)
                .remaining(remaining)
                .build();
    }

    private boolean matchStart(byte[] source, int index, int limit) {
        if (index + START.length > limit) {
            return false;
        }
        for (int i = 0; i < START.length; i++) {
            if (source[index + i] != START[i]) {
                return false;
            }
        }
        return true;
    }

    private int countCheck(int address, int command, int length, byte[] payload) {
        int result = address + command + length;
        for (byte value : payload) {
            result += value & 0xFF;
        }
        return result & 0xFF;
    }

    @Getter
    @Builder
    public static class DecodeResult {

        /**
         * 已解析出的完整帧。
         */
        private final List<BatteryCollectorFrame> frames;

        /**
         * 未形成完整帧的残留字节。
         */
        private final byte[] remaining;
    }
}
