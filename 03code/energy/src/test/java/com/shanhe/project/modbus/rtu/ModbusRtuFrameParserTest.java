package com.shanhe.project.modbus.rtu;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class ModbusRtuFrameParserTest {

    @Test
    void shouldParseValidReadRequest() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02});

        List<byte[]> frames = parser.append(frame, frame.length);

        Assertions.assertEquals(1, frames.size());
        Assertions.assertArrayEquals(frame, frames.get(0));
    }

    @Test
    void shouldDropFrameWithInvalidCrc() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02});
        frame[frame.length - 1] ^= 0x01;

        List<byte[]> frames = parser.append(frame, frame.length);

        Assertions.assertTrue(frames.isEmpty());
    }

    @Test
    void shouldWaitForPartialFrame() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] frame = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02});

        Assertions.assertTrue(parser.append(frame, 3).isEmpty());
        List<byte[]> frames = parser.append(Arrays.copyOfRange(frame, 3, frame.length), frame.length - 3);

        Assertions.assertEquals(1, frames.size());
        Assertions.assertArrayEquals(frame, frames.get(0));
    }

    @Test
    void shouldSplitStickyFrames() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] first = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02});
        byte[] second = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x06, 0x00, 0x10, 0x00, 0x01});
        byte[] sticky = new byte[first.length + second.length];
        System.arraycopy(first, 0, sticky, 0, first.length);
        System.arraycopy(second, 0, sticky, first.length, second.length);

        List<byte[]> frames = parser.append(sticky, sticky.length);

        Assertions.assertEquals(2, frames.size());
        Assertions.assertArrayEquals(first, frames.get(0));
        Assertions.assertArrayEquals(second, frames.get(1));
    }

    @Test
    void shouldParseVariableLengthWriteMultipleRequest() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] frame = ModbusRtuFrameParser.appendCrc(
                new byte[]{0x01, 0x10, 0x00, 0x10, 0x00, 0x02, 0x04, 0x00, 0x01, 0x00, 0x02});

        List<byte[]> frames = parser.append(frame, frame.length);

        Assertions.assertEquals(1, frames.size());
        Assertions.assertArrayEquals(frame, frames.get(0));
    }

    @Test
    void shouldRecoverAfterNoiseAndInvalidCrc() {
        ModbusRtuFrameParser parser = new ModbusRtuFrameParser(64);
        byte[] invalid = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x00, 0x00, 0x02});
        invalid[invalid.length - 1] ^= 0x01;
        byte[] valid = ModbusRtuFrameParser.appendCrc(new byte[]{0x01, 0x03, 0x00, 0x02, 0x00, 0x01});
        byte[] data = new byte[2 + invalid.length + valid.length];
        data[0] = 0x55;
        data[1] = 0x66;
        System.arraycopy(invalid, 0, data, 2, invalid.length);
        System.arraycopy(valid, 0, data, 2 + invalid.length, valid.length);

        List<byte[]> frames = parser.append(data, data.length);

        Assertions.assertEquals(1, frames.size());
        Assertions.assertArrayEquals(valid, frames.get(0));
    }
}
