package com.shanhe.project.collector.battery.protocol;

import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class BatteryCollectorFrameCodecTest {

    private final BatteryCollectorFrameCodec codec = new BatteryCollectorFrameCodec();

    @Test
    void shouldDecodeSingleFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x82, new byte[]{0x01, 0x02});

        BatteryCollectorFrameCodec.DecodeResult result = codec.decode(frame.toByteArray(), frame.toByteArray().length);

        Assertions.assertEquals(1, result.getFrames().size());
        Assertions.assertEquals(0, result.getRemaining().length);
        Assertions.assertEquals(0x82, result.getFrames().get(0).getCommand());
        Assertions.assertArrayEquals(new byte[]{0x01, 0x02}, result.getFrames().get(0).getPayloadSafe());
    }

    @Test
    void shouldKeepRemainingBytesForHalfFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x87, new byte[]{0x03});
        byte[] bytes = frame.toByteArray();
        byte[] partial = Arrays.copyOf(bytes, bytes.length - 2);

        BatteryCollectorFrameCodec.DecodeResult result = codec.decode(partial, partial.length);

        Assertions.assertTrue(result.getFrames().isEmpty());
        Assertions.assertArrayEquals(partial, result.getRemaining());
    }

    @Test
    void shouldDecodeStickyFramesAndKeepLastHalfFrame() {
        BatteryCollectorFrame first = codec.buildRequest(0x01, 0x82, new byte[]{0x01});
        BatteryCollectorFrame second = codec.buildRequest(0x01, 0x8D, new byte[]{0x02});
        BatteryCollectorFrame third = codec.buildRequest(0x01, 0x8E, new byte[]{0x00, 0x04, 0x0A});

        byte[] firstBytes = first.toByteArray();
        byte[] secondBytes = second.toByteArray();
        byte[] thirdBytes = third.toByteArray();
        byte[] thirdHalf = Arrays.copyOf(thirdBytes, thirdBytes.length - 3);

        byte[] source = new byte[firstBytes.length + secondBytes.length + thirdHalf.length];
        System.arraycopy(firstBytes, 0, source, 0, firstBytes.length);
        System.arraycopy(secondBytes, 0, source, firstBytes.length, secondBytes.length);
        System.arraycopy(thirdHalf, 0, source, firstBytes.length + secondBytes.length, thirdHalf.length);

        BatteryCollectorFrameCodec.DecodeResult result = codec.decode(source, source.length);

        Assertions.assertEquals(2, result.getFrames().size());
        Assertions.assertEquals(0x82, result.getFrames().get(0).getCommand());
        Assertions.assertEquals(0x8D, result.getFrames().get(1).getCommand());
        Assertions.assertArrayEquals(thirdHalf, result.getRemaining());
    }

    @Test
    void shouldSkipNoiseBeforeFrame() {
        BatteryCollectorFrame frame = codec.buildRequest(0x01, 0x86, new byte[]{0x01, 0x02, 0x03});
        byte[] bytes = frame.toByteArray();
        byte[] source = new byte[3 + bytes.length];
        source[0] = 0x11;
        source[1] = 0x22;
        source[2] = 0x33;
        System.arraycopy(bytes, 0, source, 3, bytes.length);

        BatteryCollectorFrameCodec.DecodeResult result = codec.decode(source, source.length);

        Assertions.assertEquals(1, result.getFrames().size());
        Assertions.assertEquals(0x86, result.getFrames().get(0).getCommand());
        Assertions.assertEquals(0, result.getRemaining().length);
    }
}
