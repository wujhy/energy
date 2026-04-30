package com.shanhe.project.collector.battery.service;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.common.utils.Threads;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 独立蓄电池下行采集服务。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Order(4)
@Component
public class BatteryCollectorService implements ApplicationRunner, DisposableBean {

    /**
     * 采集模块配置。
     */
    @Resource
    private BatteryCollectorProperties properties;

    /**
     * 600 节模块端帧编解码器。
     */
    @Resource
    private BatteryCollectorFrameCodec frameCodec;

    /**
     * 600 节模块端帧分发器。
     */
    @Resource
    private BatteryModuleFrameDispatcher moduleFrameDispatcher;

    /**
     * 实时数据消费器。
     */
    @Resource
    private BatteryModuleRealtimeConsumer realtimeConsumer;

    /**
     * 当前运行的通道状态。
     */
    private final List<BatteryCollectorChannelState> channelStates = new ArrayList<>();

    /**
     * 每个启用通道独立线程运行。
     */
    private ExecutorService executorService;

    /**
     * 采集服务运行标志。
     */
    private volatile boolean running;

    @Override
    public void run(ApplicationArguments args) {
        if (!Boolean.TRUE.equals(properties.getEnabled())) {
            log.info("battery collector disabled");
            return;
        }

        List<BatteryCollectorChannelConfig> enabledChannels = new ArrayList<>();
        Set<Integer> enabledBatteryGroups = new HashSet<>();
        for (BatteryCollectorChannelConfig channel : properties.getChannels()) {
            if (channel != null
                    && Boolean.TRUE.equals(channel.getEnabled())
                    && shouldRunChannel(channel)
                    && validateChannel(channel)
                    && validateUniqueBatteryGroup(channel, enabledBatteryGroups)) {
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

    private boolean shouldRunChannel(BatteryCollectorChannelConfig channel) {
        List<String> activeChannels = properties.getActiveChannels();
        return activeChannels == null || activeChannels.isEmpty() || activeChannels.contains(channel.getName());
    }

    /**
     * 获取当前采集通道状态快照。
     *
     * @return 通道状态列表
     */
    public List<BatteryCollectorChannelSnapshot> getChannelSnapshots() {
        List<BatteryCollectorChannelSnapshot> snapshots = new ArrayList<>();
        for (BatteryCollectorChannelState state : new ArrayList<>(channelStates)) {
            snapshots.add(buildSnapshot(state));
        }
        return snapshots;
    }

    BatteryCollectorChannelSnapshot buildSnapshot(BatteryCollectorChannelState state) {
        BatteryCollectorChannelSnapshot snapshot = new BatteryCollectorChannelSnapshot();
        if (state == null) {
            return snapshot;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        snapshot.setName(config == null ? null : config.getName());
        snapshot.setPortName(config == null ? null : config.getPortName());
        snapshot.setBatteryGroup(config == null ? null : config.getBatteryGroup());
        snapshot.setDeviceAddress(config == null ? null : config.getDeviceAddress());
        snapshot.setOpened(Boolean.TRUE.equals(state.getOpened().get())
                && state.getSerialPort() != null
                && state.getSerialPort().isOpen());
        snapshot.setRunState(state.getRunState());
        snapshot.setLastReceiveTime(state.getLastReceiveTime());
        snapshot.setLastSendTime(state.getLastSendTime());
        snapshot.setLastPollTime(state.getLastPollTime());
        snapshot.setLastTimeoutTime(state.getLastTimeoutTime());
        snapshot.setTimeoutCount(state.getTimeoutCount());
        snapshot.setCurrentRetryCount(state.getCurrentRetryCount());
        snapshot.setLastRequestCode(state.getLastRequestCode());
        snapshot.setExpectedResponseCode(state.getExpectedResponseCode());
        snapshot.setLastResponseCode(state.getLastResponseCode());
        snapshot.setLastPendingCompletedAt(state.getLastPendingCompletedAt());
        snapshot.setLastPendingTimedOut(state.isLastPendingTimedOut());
        snapshot.setCurrentPollBatchNo(state.getCurrentPollBatchNo());
        snapshot.setCurrentPollStartedAt(state.getCurrentPollStartedAt());
        snapshot.setCurrentPollAddress(state.getCurrentPollAddress());
        snapshot.setPollRoundCount(state.getPollRoundCount());
        snapshot.setCurrentFullDiscovery(state.isCurrentFullDiscovery());
        snapshot.setLastFullDiscoveryTime(state.getLastFullDiscoveryTime());
        List<Integer> activeAddresses = sortedActiveModuleAddresses(state);
        snapshot.setActiveModuleAddressCount(activeAddresses.size());
        snapshot.setActiveModuleAddresses(activeAddresses.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        snapshot.setReceiveBufferSize(state.getReceiveBuffer().size());
        BatteryPendingRequest pendingRequest = state.getPendingCommand();
        if (pendingRequest != null) {
            snapshot.setPendingCommandName(pendingRequest.getName());
            snapshot.setPendingRequestCode(pendingRequest.getRequestCode());
            snapshot.setPendingResponseCode(pendingRequest.getResponseCode());
            snapshot.setPendingAutoPoll(pendingRequest.isAutoPoll());
        }
        return snapshot;
    }

    private boolean validateChannel(BatteryCollectorChannelConfig channel) {
        if (isBlank(channel.getName())) {
            log.warn("battery collector channel ignored because name is blank");
            return false;
        }
        if (isBlank(channel.getPortName())) {
            log.warn("battery collector channel ignored because portName is blank, channel={}", channel.getName());
            return false;
        }
        if (channel.getDeviceAddress() == null || channel.getDeviceAddress() < 0 || channel.getDeviceAddress() > 255) {
            log.warn("battery collector channel ignored because deviceAddress is invalid, channel={}, address={}",
                    channel.getName(),
                    channel.getDeviceAddress());
            return false;
        }
        if (channel.getBatteryGroup() == null || channel.getBatteryGroup() <= 0) {
            log.warn("battery collector channel ignored because batteryGroup is invalid, channel={}, batteryGroup={}",
                    channel.getName(),
                    channel.getBatteryGroup());
            return false;
        }
        return true;
    }

    private boolean validateUniqueBatteryGroup(BatteryCollectorChannelConfig channel, Set<Integer> enabledBatteryGroups) {
        Integer batteryGroup = channel.getBatteryGroup();
        if (enabledBatteryGroups.add(batteryGroup)) {
            return true;
        }
        log.warn("battery collector channel ignored because batteryGroup is duplicated, channel={}, batteryGroup={}",
                channel.getName(),
                batteryGroup);
        return false;
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
            Threads.sleep(resolveLoopDelayMs());
        }
        closeQuietly(state);
    }

    private void ensurePortOpen(BatteryCollectorChannelState state) {
        if (Boolean.TRUE.equals(state.getOpened().get()) && state.getSerialPort() != null && state.getSerialPort().isOpen()) {
            return;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        SerialPort serialPort = SerialPort.getCommPort(config.getPortName());
        serialPort.setComPortParameters(resolveBaudRate(config), resolveDataBits(config),
                resolveStopBits(config), resolveParity(config));
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING,
                resolvePortTimeoutMs(config), resolvePortTimeoutMs(config));
        serialPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        if (!serialPort.openPort()) {
            throw new IllegalStateException("open serial port failed: " + config.getPortName());
        }
        state.setSerialPort(serialPort);
        state.getOpened().set(true);
        state.setRunState(BatteryCollectorRunState.READ);
        log.info("battery collector channel opened, channel={}, port={}", config.getName(), config.getPortName());
    }

    private void pollIfNecessary(BatteryCollectorChannelState state) {
        if (state.getPendingCommand() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        long interval = resolvePollIntervalMs(state.getConfig());
        if (state.getLastPollTime() > 0 && now - state.getLastPollTime() < interval) {
            return;
        }
        pollOnce(state);
        state.setLastPollTime(now);
    }

    private void pollOnce(BatteryCollectorChannelState state) {
        List<String> polledCommands = new ArrayList<>();
        List<String> completedCommands = new ArrayList<>();
        long startedAt = System.currentTimeMillis();
        boolean fullDiscovery = shouldRunFullDiscovery(state, startedAt);
        String batchNo = buildPollBatchNo(state, startedAt);
        state.setCurrentPollBatchNo(batchNo);
        state.setCurrentPollStartedAt(startedAt);
        state.setPollRoundCount(state.getPollRoundCount() + 1);
        state.setCurrentFullDiscovery(fullDiscovery);
        if (fullDiscovery) {
            state.setLastFullDiscoveryTime(startedAt);
        }
        // 同一轮 01/81 采集共享批次号，用于关联单体行和 246 组信息行。
        BatteryModulePollContextHolder.set(BatteryModulePollContext.builder()
                .pollBatchNo(batchNo)
                .pollStartedAt(new Date(startedAt))
                .build());
        try {
            // 默认自动轮询只允许 600 节模块端 01/81，不引入 980 聚合命令。
            BatteryDeviceProtocolCode pollingCommand = BatteryDeviceProtocolCode.MODULE_INFO;
            for (Integer address : resolvePollingAddresses(state, fullDiscovery)) {
                if (!running) {
                    break;
                }
                state.setCurrentPollAddress(address);
                polledCommands.add(String.format("%02X:%02X/%02X",
                        address,
                        pollingCommand.getRequestCode(),
                        pollingCommand.getResponseCode()));
                sendCommand(state, pollingCommand, address);
                waitForPendingComplete(state);
                boolean responded = state.getPendingCommand() == null && !state.isLastPendingTimedOut();
                updateModuleAddressCache(state, address, responded);
                if (responded) {
                    completedCommands.add(String.format("%02X:%02X/%02X",
                            address,
                            pollingCommand.getRequestCode(),
                            pollingCommand.getResponseCode()));
                }
            }
            realtimeConsumer.flushCurrentPollBatch(state.getConfig());
        } finally {
            BatteryModulePollContextHolder.clear();
            state.setCurrentPollAddress(0);
            state.setCurrentFullDiscovery(false);
        }
        logPollSummary(state, fullDiscovery, polledCommands, completedCommands);
    }

    private void sendCommand(BatteryCollectorChannelState state, BatteryDeviceProtocolCode pollingCommand) {
        sendCommand(state, pollingCommand, state.getConfig().getDeviceAddress());
    }

    private void sendCommand(BatteryCollectorChannelState state, BatteryDeviceProtocolCode pollingCommand, int address) {
        byte[] payload = new byte[0];
        BatteryCollectorFrame request = frameCodec.buildRequest(
                address,
                pollingCommand.getRequestCode(),
                payload);
        writeFrame(state, request, BatteryPendingRequest.fromProtocolCode(pollingCommand, address, payload, true), BatteryCollectorRunState.WAIT_RESPONSE);
    }

    private void waitForPendingComplete(BatteryCollectorChannelState state) {
        while (running && state.getPendingCommand() != null) {
            Threads.sleep(Math.max(10, Math.min(resolveRequestGapMs(), 100)));
            readOnce(state);
            checkTimeout(state);
        }
    }

    private void writeFrame(BatteryCollectorChannelState state, BatteryCollectorFrame frame,
                            BatteryPendingRequest pendingRequest, BatteryCollectorRunState waitingState) {
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
                    String.format("%02X", pendingRequest.getRequestCode()),
                    bytes.length,
                    written);
        }
        logProtocol(state, "tx", "cmd=" + String.format("%02X", pendingRequest.getRequestCode())
                + ", expect=" + String.format("%02X", pendingRequest.getResponseCode())
                + ", retry=" + state.getCurrentRetryCount()
                + ", mode=" + waitingState
                + ", hex=" + frame.toHex());
        state.setPendingCommand(pendingRequest);
        state.setLastRequestCode(pendingRequest.getRequestCode());
        state.setExpectedResponseCode(pendingRequest.getResponseCode());
        state.setLastPendingTimedOut(false);
        state.setRunState(waitingState);
    }

    private void readOnce(BatteryCollectorChannelState state) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort == null || !serialPort.isOpen() || serialPort.bytesAvailable() <= 0) {
            return;
        }
        int available = serialPort.bytesAvailable();
        int size = Math.max(available, resolveReadBufferSize(state.getConfig()));
        byte[] buffer = new byte[size];
        int read = serialPort.readBytes(buffer, Math.min(size, available));
        if (read <= 0) {
            return;
        }
        state.setLastReceiveTime(System.currentTimeMillis());
        logProtocol(state, "rx-bytes", "len=" + read + ", hex=" + bytesToHex(buffer, read));
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
            state.setLastResponseCode(frame.getCommand());
            logProtocol(state, "rx-frame", "cmd=" + String.format("%02X", frame.getCommand())
                    + ", expect=" + String.format("%02X", state.getExpectedResponseCode())
                    + ", hex=" + frame.toHex());
            moduleFrameDispatcher.dispatch(state.getConfig(), frame);
            if (isCurrentPendingResponse(state, frame)) {
                state.setPendingCommand(null);
                state.setExpectedResponseCode(0);
                state.setCurrentRetryCount(0);
                state.setLastPendingCompletedAt(System.currentTimeMillis());
                state.setLastPendingTimedOut(false);
                state.setRunState(BatteryCollectorRunState.READ);
            } else if (isKnownModuleResponse(frame.getCommand())) {
                log.debug("battery collector response frame out of current wait, channel={}, request={}, expect={}, actual={}",
                        state.getConfig().getName(),
                        String.format("%02X", state.getLastRequestCode()),
                        String.format("%02X", state.getExpectedResponseCode()),
                        String.format("%02X", frame.getCommand()));
            } else {
                log.info("battery collector unsolicited frame, channel={}, request={}, expect={}, actual={}",
                        state.getConfig().getName(),
                        String.format("%02X", state.getLastRequestCode()),
                        String.format("%02X", state.getExpectedResponseCode()),
                        String.format("%02X", frame.getCommand()));
            }
        }
    }

    private boolean isKnownModuleResponse(int commandCode) {
        BatteryDeviceProtocolCode protocolCode = BatteryDeviceProtocolCode.find(commandCode);
        return protocolCode != null && protocolCode.isResponse(commandCode);
    }

    private boolean isCurrentPendingResponse(BatteryCollectorChannelState state, BatteryCollectorFrame frame) {
        BatteryPendingRequest pendingRequest = state.getPendingCommand();
        // 迟到帧可能仍是 81 响应，必须同时匹配模块地址才完成当前等待。
        return pendingRequest != null
                && frame.getCommand() == state.getExpectedResponseCode()
                && frame.getAddress() == pendingRequest.getRequestAddress();
    }

    private void trimReceiveBufferIfNecessary(BatteryCollectorChannelState state) {
        int limit = resolveReceiveBufferLimit(state.getConfig());
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
        long timeoutMs = resolveResponseTimeoutMs(state.getConfig());
        if (now - state.getLastSendTime() < timeoutMs) {
            return;
        }

        int retryCount = state.getCurrentRetryCount();
        int maxRetryCount = resolveMaxRetryCount(state.getConfig());
        if (retryCount < maxRetryCount) {
            state.setCurrentRetryCount(retryCount + 1);
            BatteryPendingRequest retryRequest = state.getPendingCommand();
            log.warn("battery collector response timeout, retrying, channel={}, request={}, response={}, retry={}/{}",
                    state.getConfig().getName(),
                    String.format("%02X", state.getLastRequestCode()),
                    String.format("%02X", state.getExpectedResponseCode()),
                    state.getCurrentRetryCount(),
                    maxRetryCount);
            logProtocol(state, "retry", "request=" + String.format("%02X", state.getLastRequestCode())
                    + ", expect=" + String.format("%02X", state.getExpectedResponseCode())
                    + ", retry=" + state.getCurrentRetryCount());
            BatteryCollectorFrame request = frameCodec.buildRequest(
                    retryRequest.getRequestAddress(),
                    retryRequest.getRequestCode(),
                    retryRequest.getPayload());
            writeFrame(state, request, retryRequest, retryRequest.isAutoPoll()
                    ? BatteryCollectorRunState.WAIT_RESPONSE
                    : BatteryCollectorRunState.WAIT_COMMAND_RESPONSE);
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
        state.setLastPendingCompletedAt(System.currentTimeMillis());
        state.setLastPendingTimedOut(true);
        state.setRunState(BatteryCollectorRunState.READ);
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
        state.setLastPendingTimedOut(false);
        state.setRunState(BatteryCollectorRunState.READ);
        state.getReceiveBuffer().reset();
    }

    private void logProtocol(BatteryCollectorChannelState state, String stage, String message) {
        if (!Boolean.TRUE.equals(properties.getDebugEnabled())) {
            return;
        }
        List<String> debugChannels = properties.getDebugChannels();
        if (debugChannels != null && !debugChannels.isEmpty() && !debugChannels.contains(state.getConfig().getName())) {
            return;
        }
        log.info("battery collector protocol, channel={}, port={}, stage={}, {}",
                state.getConfig().getName(),
                state.getConfig().getPortName(),
                stage,
                message);
    }

    private void logPollSummary(BatteryCollectorChannelState state, boolean fullDiscovery,
                                List<String> polledCommands, List<String> completedCommands) {
        if (polledCommands.isEmpty()) {
            return;
        }
        String waiting = state.getPendingCommand() == null
                ? "-"
                : String.format("%02X/%02X",
                state.getPendingCommand().getRequestCode(),
                state.getPendingCommand().getResponseCode());
        log.info("battery collector poll summary, channel={}, runState={}, fullDiscovery={}, polledCount={}, completedCount={}, activeAddressCount={}, completed={}, waiting={}, timeoutCount={}",
                state.getConfig().getName(),
                state.getRunState(),
                fullDiscovery,
                polledCommands.size(),
                completedCommands.size(),
                state.getActiveModuleAddresses().size(),
                summarizeCommands(completedCommands),
                waiting,
                state.getTimeoutCount());
    }

    private String summarizeCommands(List<String> commands) {
        if (commands.isEmpty()) {
            return "-";
        }
        int limit = 32;
        if (commands.size() <= limit) {
            return String.join(",", commands);
        }
        return String.join(",", commands.subList(0, limit)) + ",...+" + (commands.size() - limit);
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder builder = new StringBuilder(length * 2);
        for (int i = 0; i < length; i++) {
            builder.append(String.format("%02X", bytes[i]));
        }
        return builder.toString();
    }

    int resolveLoopDelayMs() {
        return resolvePositiveInt(properties.getLoopDelayMs(), 300);
    }

    int resolveRequestGapMs() {
        return resolvePositiveInt(properties.getRequestGapMs(), 120);
    }

    int resolveBaudRate(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getBaudRate();
        return resolvePositiveInt(value, 9600);
    }

    int resolveDataBits(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getDataBits();
        return value == null || value < 5 || value > 8 ? 8 : value;
    }

    int resolveStopBits(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getStopBits();
        return value == null || value < 1 || value > 3 ? 1 : value;
    }

    int resolveParity(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getParity();
        return value == null || value < 0 || value > 4 ? 0 : value;
    }

    int resolvePortTimeoutMs(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getTimeoutMs();
        return resolvePositiveInt(value, 1000);
    }

    int resolveModuleAddressStart(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getModuleAddressStart();
        int start = value == null || value < 1 || value > 246 ? 1 : value;
        return Math.min(start, resolveModuleAddressEnd(config));
    }

    int resolveModuleAddressEnd(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getModuleAddressEnd();
        return value == null || value < 1 || value > 246 ? 246 : value;
    }

    long resolvePollIntervalMs(BatteryCollectorChannelConfig config) {
        Long value = config == null ? null : config.getPollIntervalMs();
        return resolvePositiveLong(value, 3000L);
    }

    int resolveReadBufferSize(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getReadBufferSize();
        return resolvePositiveInt(value, 2048);
    }

    int resolveReceiveBufferLimit(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getReceiveBufferLimit();
        return Math.max(resolvePositiveInt(value, 8192), 64);
    }

    long resolveResponseTimeoutMs(BatteryCollectorChannelConfig config) {
        Long value = config == null ? null : config.getResponseTimeoutMs();
        return resolvePositiveLong(value, 1500L);
    }

    int resolveMaxRetryCount(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getMaxRetryCount();
        return value == null || value < 0 ? 2 : value;
    }

    int resolveModuleAddressMissThreshold() {
        Integer value = properties.getModuleAddressMissThreshold();
        return value == null || value <= 0 ? 3 : value;
    }

    private int resolvePositiveInt(Number value, int defaultValue) {
        if (value == null || value.longValue() <= 0 || value.longValue() > Integer.MAX_VALUE) {
            return defaultValue;
        }
        return value.intValue();
    }

    private long resolvePositiveLong(Long value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildPollBatchNo(BatteryCollectorChannelState state, long startedAt) {
        String channelName = state.getConfig() == null ? "channel" : state.getConfig().getName();
        return channelName + "-" + startedAt;
    }

    private List<Integer> resolvePollingAddresses(BatteryCollectorChannelState state, boolean fullDiscovery) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled()) || fullDiscovery) {
            return fullModuleAddressRange(state.getConfig());
        }
        List<Integer> activeAddresses = sortedActiveModuleAddresses(state);
        if (activeAddresses.isEmpty()) {
            state.getFullDiscoveryRequested().set(true);
            return fullModuleAddressRange(state.getConfig());
        }
        appendRequiredGroupModuleAddress(activeAddresses, state.getConfig());
        return activeAddresses;
    }

    private void appendRequiredGroupModuleAddress(List<Integer> addresses, BatteryCollectorChannelConfig config) {
        int groupModuleAddress = 246;
        if (resolveModuleAddressStart(config) > groupModuleAddress
                || resolveModuleAddressEnd(config) < groupModuleAddress
                || addresses.contains(groupModuleAddress)) {
            return;
        }
        addresses.add(groupModuleAddress);
        Collections.sort(addresses);
    }

    private List<Integer> fullModuleAddressRange(BatteryCollectorChannelConfig config) {
        List<Integer> addresses = new ArrayList<>();
        for (int address = resolveModuleAddressStart(config); address <= resolveModuleAddressEnd(config); address++) {
            addresses.add(address);
        }
        return addresses;
    }

    private List<Integer> sortedActiveModuleAddresses(BatteryCollectorChannelState state) {
        List<Integer> addresses = new ArrayList<>(state.getActiveModuleAddresses());
        Collections.sort(addresses);
        return addresses;
    }

    private boolean shouldRunFullDiscovery(BatteryCollectorChannelState state, long now) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return true;
        }
        // 启动、人工重置、缓存为空或周期到期时回到 1..246 全量发现。
        if (state.getFullDiscoveryRequested().getAndSet(false)) {
            return true;
        }
        if (state.getActiveModuleAddresses().isEmpty()) {
            return true;
        }
        Long interval = properties.getModuleAddressFullDiscoveryIntervalMs();
        return interval != null && interval > 0
                && state.getLastFullDiscoveryTime() > 0
                && now - state.getLastFullDiscoveryTime() >= interval;
    }

    private void updateModuleAddressCache(BatteryCollectorChannelState state, int address, boolean responded) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return;
        }
        // 只缓存有响应地址；稳定运行后避免每轮扫描不存在的模块。
        if (responded) {
            state.getActiveModuleAddresses().add(address);
            state.getModuleAddressMissCounts().remove(address);
            return;
        }
        if (!state.getActiveModuleAddresses().contains(address)) {
            return;
        }
        int misses = state.getModuleAddressMissCounts().merge(address, 1, Integer::sum);
        if (misses >= resolveModuleAddressMissThreshold()) {
            state.getActiveModuleAddresses().remove(address);
            state.getModuleAddressMissCounts().remove(address);
            log.warn("battery module address removed from cache after consecutive misses, channel={}, address={}, misses={}",
                    state.getConfig().getName(),
                    address,
                    misses);
        }
    }

    /**
     * 重置模块地址缓存，下轮轮询恢复全量发现。
     *
     * @param channelName 通道名称；为空时重置全部通道
     * @return 是否匹配到通道
     */
    public boolean resetModuleAddressCache(String channelName) {
        boolean matched = false;
        for (BatteryCollectorChannelState state : new ArrayList<>(channelStates)) {
            if (channelName == null || channelName.trim().isEmpty()
                    || channelName.equals(state.getConfig().getName())) {
                state.getActiveModuleAddresses().clear();
                state.getModuleAddressMissCounts().clear();
                state.getFullDiscoveryRequested().set(true);
                matched = true;
            }
        }
        return matched;
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
