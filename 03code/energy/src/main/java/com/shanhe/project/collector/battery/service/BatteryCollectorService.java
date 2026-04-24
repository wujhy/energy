package com.shanhe.project.collector.battery.service;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.common.utils.Threads;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.protocol.BatteryPollingCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 独立蓄电池下行采集模块。
 */
@Slf4j
@Order(4)
@Component
public class BatteryCollectorService implements ApplicationRunner, DisposableBean {

    @Resource
    private BatteryCollectorProperties properties;
    @Resource
    private BatteryCollectorFrameCodec frameCodec;
    @Resource
    private BatteryCollectorAdapter batteryAdapter;

    private final List<BatteryCollectorChannelState> channelStates = new ArrayList<>();
    private ExecutorService executorService;
    private volatile boolean running;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("battery collector disabled");
            return;
        }

        List<BatteryCollectorChannelConfig> enabledChannels = new ArrayList<>();
        for (BatteryCollectorChannelConfig channel : properties.getChannels()) {
            if (channel != null && Boolean.TRUE.equals(channel.getEnabled())) {
                enabledChannels.add(channel);
            }
        }
        if (enabledChannels.isEmpty()) {
            log.warn("battery collector enabled but no active channel config found");
            return;
        }

        running = true;
        executorService = Executors.newFixedThreadPool(enabledChannels.size());
        for (BatteryCollectorChannelConfig channel : enabledChannels) {
            BatteryCollectorChannelState state = new BatteryCollectorChannelState(channel);
            channelStates.add(state);
            executorService.submit(() -> runChannel(state));
        }
    }

    private void runChannel(BatteryCollectorChannelState state) {
        while (running) {
            try {
                ensurePortOpen(state);
                readOnce(state);
                if (Boolean.TRUE.equals(properties.getAutoPollEnabled())) {
                    pollIfNecessary(state);
                    checkTimeout(state);
                }
            } catch (Exception e) {
                log.error("battery collector channel error, channel={}, port={}",
                        state.getConfig().getName(),
                        state.getConfig().getPortName(),
                        e);
                closeQuietly(state);
                Threads.sleep(1000);
            }
            Threads.sleep(Math.toIntExact(properties.getLoopDelayMs()));
        }
        closeQuietly(state);
    }

    private void ensurePortOpen(BatteryCollectorChannelState state) {
        if (Boolean.TRUE.equals(state.getOpened().get()) && state.getSerialPort() != null && state.getSerialPort().isOpen()) {
            return;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        SerialPort serialPort = SerialPort.getCommPort(config.getPortName());
        serialPort.setComPortParameters(config.getBaudRate(), config.getDataBits(), config.getStopBits(), config.getParity());
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                config.getTimeoutMs(), config.getTimeoutMs());
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!serialPort.openPort()) {
            throw new IllegalStateException("open serial port failed: " + config.getPortName());
        }
        state.setSerialPort(serialPort);
        state.getOpened().set(true);
        log.info("battery collector channel opened, channel={}, port={}", config.getName(), config.getPortName());
    }

    private void pollIfNecessary(BatteryCollectorChannelState state) {
        if (state.getPendingCommand() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long interval = state.getConfig().getPollIntervalMs();
        if (state.getLastPollTime() > 0 && now - state.getLastPollTime() < interval) {
            return;
        }
        pollOnce(state);
        state.setLastPollTime(now);
    }

    private void pollOnce(BatteryCollectorChannelState state) {
        for (BatteryPollingCommand pollingCommand : BatteryPollingCommand.defaults()) {
            sendCommand(state, pollingCommand);
            Threads.sleep(Math.toIntExact(properties.getRequestGapMs()));
            readOnce(state);
            if (state.getPendingCommand() != null) {
                break;
            }
        }
    }

    private void sendCommand(BatteryCollectorChannelState state, BatteryPollingCommand pollingCommand) {
        BatteryCollectorFrame request = frameCodec.buildRequest(
                state.getConfig().getDeviceAddress(),
                pollingCommand.getRequestCode(),
                pollingCommand.buildPayload(state.getConfig()));
        writeFrame(state, request, pollingCommand);
    }

    private void writeFrame(BatteryCollectorChannelState state, BatteryCollectorFrame frame, BatteryPollingCommand pollingCommand) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort == null || !serialPort.isOpen()) {
            return;
        }
        byte[] bytes = frame.toByteArray();
        int written = serialPort.writeBytes(bytes, bytes.length);
        state.setLastSendTime(System.currentTimeMillis());
        if (written != bytes.length) {
            log.warn("battery command write incomplete, channel={}, request={}, expect={}, actual={}",
                    state.getConfig().getName(),
                    String.format("%02X", pollingCommand.getRequestCode()),
                    bytes.length,
                    written);
        }
        state.setPendingCommand(pollingCommand);
        state.setLastRequestCode(pollingCommand.getRequestCode());
        state.setExpectedResponseCode(pollingCommand.getResponseCode());
    }

    private void readOnce(BatteryCollectorChannelState state) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort == null || !serialPort.isOpen() || serialPort.bytesAvailable() <= 0) {
            return;
        }
        int available = serialPort.bytesAvailable();
        int size = Math.max(available, state.getConfig().getReadBufferSize());
        byte[] buffer = new byte[size];
        int read = serialPort.readBytes(buffer, Math.min(size, available));
        if (read <= 0) {
            return;
        }
        state.setLastReceiveTime(System.currentTimeMillis());
        ByteArrayOutputStream receiveBuffer = state.getReceiveBuffer();
        receiveBuffer.write(buffer, 0, read);
        trimReceiveBufferIfNecessary(state);

        byte[] source = receiveBuffer.toByteArray();
        BatteryCollectorFrameCodec.DecodeResult decodeResult = frameCodec.decode(source, source.length);
        receiveBuffer.reset();
        byte[] remaining = decodeResult.getRemaining();
        if (remaining.length > 0) {
            receiveBuffer.write(remaining, 0, remaining.length);
        }
        trimReceiveBufferIfNecessary(state);

        for (BatteryCollectorFrame frame : decodeResult.getFrames()) {
            batteryAdapter.dispatch(state.getConfig(), frame);
            if (frame.getCommand() == state.getExpectedResponseCode()) {
                state.setPendingCommand(null);
                state.setExpectedResponseCode(0);
                state.setCurrentRetryCount(0);
            } else {
                log.debug("battery collector unexpected response, channel={}, request={}, expect={}, actual={}",
                        state.getConfig().getName(),
                        String.format("%02X", state.getLastRequestCode()),
                        String.format("%02X", state.getExpectedResponseCode()),
                        String.format("%02X", frame.getCommand()));
            }
        }
    }

    private void trimReceiveBufferIfNecessary(BatteryCollectorChannelState state) {
        int limit = state.getConfig().getReceiveBufferLimit();
        ByteArrayOutputStream receiveBuffer = state.getReceiveBuffer();
        if (receiveBuffer.size() <= limit) {
            return;
        }
        byte[] data = receiveBuffer.toByteArray();
        int keep = Math.min(limit / 2, data.length);
        byte[] tail = new byte[keep];
        System.arraycopy(data, data.length - keep, tail, 0, keep);
        receiveBuffer.reset();
        receiveBuffer.write(tail, 0, tail.length);
        log.warn("battery collector receive buffer trimmed, channel={}, limit={}, keep={}",
                state.getConfig().getName(),
                limit,
                keep);
    }

    private void checkTimeout(BatteryCollectorChannelState state) {
        if (state.getPendingCommand() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long timeoutMs = state.getConfig().getResponseTimeoutMs();
        if (now - state.getLastSendTime() < timeoutMs) {
            return;
        }

        int retryCount = state.getCurrentRetryCount();
        int maxRetryCount = state.getConfig().getMaxRetryCount();
        if (retryCount < maxRetryCount) {
            state.setCurrentRetryCount(retryCount + 1);
            BatteryPollingCommand retryCommand = state.getPendingCommand();
            log.warn("battery collector response timeout, retrying, channel={}, request={}, response={}, retry={}/{}",
                    state.getConfig().getName(),
                    String.format("%02X", state.getLastRequestCode()),
                    String.format("%02X", state.getExpectedResponseCode()),
                    state.getCurrentRetryCount(),
                    maxRetryCount);
            sendCommand(state, retryCommand);
            state.getReceiveBuffer().reset();
            return;
        }

        log.warn("battery collector response timeout, channel={}, request={}, response={}, timeoutCount={}",
                state.getConfig().getName(),
                String.format("%02X", state.getLastRequestCode()),
                String.format("%02X", state.getExpectedResponseCode()),
                state.getTimeoutCount() + 1);
        state.setPendingCommand(null);
        state.setExpectedResponseCode(0);
        state.setCurrentRetryCount(0);
        state.setTimeoutCount(state.getTimeoutCount() + 1);
        state.setLastTimeoutTime(now);
        state.getReceiveBuffer().reset();
    }

    private void closeQuietly(BatteryCollectorChannelState state) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort != null) {
            try {
                if (serialPort.isOpen()) {
                    serialPort.closePort();
                }
            } catch (Exception ignored) {
            }
        }
        state.getOpened().set(false);
        state.setSerialPort(null);
        state.setPendingCommand(null);
        state.setExpectedResponseCode(0);
        state.setCurrentRetryCount(0);
        state.getReceiveBuffer().reset();
    }

    @Override
    public void destroy() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        for (BatteryCollectorChannelState state : channelStates) {
            closeQuietly(state);
        }
    }
}
