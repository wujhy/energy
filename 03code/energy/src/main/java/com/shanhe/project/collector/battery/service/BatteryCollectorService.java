package com.shanhe.project.collector.battery.service;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.common.utils.Threads;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.*;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.runtime.BatteryCollectorFrameIoService;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.opt.domain.OptLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

    private static final int START_SET_ADDRESS = 1;
    private static final int STOP_SET_ADDRESS = 2;

    /** 设备状态码：串口状态。 */
    private static final String STATE_CODE_SERIAL_PORT = BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN;
    /** 设备状态码：轮询超时计数。 */
    private static final String STATE_CODE_POLL_TIMEOUT = BatteryDeviceStateConstants.StateCode.CHANNEL_TIMEOUT_COUNT;

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
     * 旧后台电池组配置服务，用于读取每组期望单体数量。
     */
    @Resource
    private IBatteryPackService batteryPackService;

    /**
     * 旧页面/测试接口工作模式缓存。
     */
    @Resource
    private BatteryModeStatusService batteryModeStatusService;

    /**
     * 操作日志 Mapper。
     */
    @Resource
    private com.shanhe.project.device.opt.mapper.OptLogMapper optLogMapper;

    /**
     * 设备状态服务，用于持久化通道运行状态。
     */
    @Resource
    private BatteryDeviceStateService batteryDeviceStateService;

    /**
     * 连接条电阻结果缓存服务。
     */
    @Resource
    private BatteryModuleCellCompatibilityFillService compatibilityFillService;

    /**
     * 标准实时有效快照服务。
     */
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;
    @Resource
    private BatteryCollectorRuntimeViewService runtimeViewService;
    @Resource
    private BatteryCollectorCacheService cacheService;
    @Resource
    private BatteryCollectorProtocolLogService protocolLogService;
    @Resource
    private BatteryCollectorCommandLogService commandLogService;
    @Resource
    private BatteryCollectorDeviceStateService collectorDeviceStateService;

    /**
     * 串口帧收发协调服务。
     */
    @Resource
    private BatteryCollectorFrameIoService frameIoService;

    /**
     * 轮询循环编排服务。
     */
    @Resource
    private com.shanhe.project.collector.battery.runtime.BatteryCollectorPollingService pollingService;

    /**
     * 命令队列执行服务。
     */
    @Resource
    private com.shanhe.project.collector.battery.command.BatteryCollectorCommandQueueService commandQueueService;

    /**
     * 600节模块端标准实时数据 Mapper。
     */
    @Resource
    private com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper realtimeMapper;

    /**
     * 状态去重缓存：key = scopeKey + stateCode，value = 上次写入的 stateValue。
     * 避免短时间内重复写入相同状态。
     */
    private final java.util.concurrent.ConcurrentHashMap<String, String> lastStateValues = new java.util.concurrent.ConcurrentHashMap<>();

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
            log.info("蓄电池采集服务未启用");
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
            log.warn("蓄电池采集服务已启用但未找到有效的通道配置");
            return;
        }

        running = true;
        executorService = new ThreadPoolExecutor(enabledChannels.size(), enabledChannels.size(),
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        for (BatteryCollectorChannelConfig channel : enabledChannels) {
            BatteryCollectorChannelState state = new BatteryCollectorChannelState(channel);
            channelStates.add(state);
            executorService.submit(() -> runChannel(state));
        }
    }

    /** 判断通道是否应在当前活跃通道列表中运行。 */
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
        return runtimeViewService.getChannelSnapshots(channelStates);
    }

    public BatteryCollectorMetrics getMetrics() {
        return runtimeViewService.getMetrics(channelStates, realtimeSnapshotService);
    }

    /**
     * 提交显式600节模块端控制命令，由对应采集通道线程在自动轮询空档下发。
     *
     * @param channelName 通道名称
     * @param command 控制命令
     * @return 是否已加入下发队列
     */
    public boolean submitModuleCommand(String channelName, BatteryModuleControlCommand command) {
        if (isBlank(channelName) || command == null || command.getProtocolCode() == null) {
            return false;
        }
        for (BatteryCollectorChannelState state : new ArrayList<>(channelStates)) {
            BatteryCollectorChannelConfig config = state.getConfig();
            if (config != null && channelName.equals(config.getName())) {
                applyCommandChannelContext(config, command);
                Long optLogId = commandLogService.createCommandOptLog(config, command);
                command.setOptLogId(optLogId);
                markModeRunning(command);
                if (!state.getQueuedModuleCommands().offer(command)) {
                    markModeStopped(command, false);
                    commandLogService.updateCommandOptLog(optLogId, BatteryDeviceStateConstants.CommandStatus.REJECTED, null, null);
                    return false;
                }
                log.info("蓄电池模块命令已加入队列, 通道={}, 命令={}, 地址={}, 响应={}",
                        channelName,
                        command.getProtocolCode(),
                        command.getAddress(),
                        command.getResponseCode());
                return true;
            }
        }
        log.warn("蓄电池模块命令队列拒绝，通道未激活, 通道={}, 命令={}",
                channelName,
                command.getProtocolCode());
        return false;
    }

    /** 填充兼容写表字段和电池组号到控制命令。 */
    private void applyCommandChannelContext(BatteryCollectorChannelConfig config, BatteryModuleControlCommand command) {
        if (config == null || command == null) {
            return;
        }
        if (command.getConfigId() == null) {
            command.setConfigId(config.getConfigId());
        }
        if (command.getBatteryGroup() == null) {
            command.setBatteryGroup(config.getBatteryGroup());
        }
    }

    /** 标记工作模式为运行中。 */
    private void markModeRunning(BatteryModuleControlCommand command) {
        if (command == null || command.getMode() == null) {
            return;
        }
        batteryModeStatusService.markRunning(
                command.getBatteryGroup(),
                command.getMode(),
                command.getAddress(),
                command.getOptLogId());
    }

    /** 校验通道配置的名称、串口、地址和电池组编号是否有效。 */
    private boolean validateChannel(BatteryCollectorChannelConfig channel) {
        if (isBlank(channel.getName())) {
            log.warn("蓄电池采集通道被忽略，通道名称为空");
            return false;
        }
        if (isBlank(channel.getPortName())) {
            log.warn("蓄电池采集通道被忽略，串口名称为空, 通道={}", channel.getName());
            return false;
        }
        if (channel.getDeviceAddress() == null || channel.getDeviceAddress() < 0 || channel.getDeviceAddress() > UNSIGNED_BYTE_MAX) {
            log.warn("蓄电池采集通道被忽略，设备地址无效, 通道={}, 地址={}",
                    channel.getName(),
                    channel.getDeviceAddress());
            return false;
        }
        if (channel.getBatteryGroup() == null || channel.getBatteryGroup() <= 0) {
            log.warn("蓄电池采集通道被忽略，电池组编号无效, 通道={}, 电池组={}",
                    channel.getName(),
                    channel.getBatteryGroup());
            return false;
        }
        return true;
    }

    /** 校验电池组编号是否重复。 */
    private boolean validateUniqueBatteryGroup(BatteryCollectorChannelConfig channel, Set<Integer> enabledBatteryGroups) {
        Integer batteryGroup = channel.getBatteryGroup();
        if (enabledBatteryGroups.add(batteryGroup)) {
            return true;
        }
        log.warn("蓄电池采集通道被忽略，电池组编号重复, 通道={}, 电池组={}",
                channel.getName(),
                batteryGroup);
        return false;
    }

    /** 单个采集通道的主循环：开串口、读数据、处理命令、轮询、超时检测。 */
    private void runChannel(BatteryCollectorChannelState state) {
        while (running) {
            try {
                ensurePortOpen(state);
                readOnce(state);
                processQueuedModuleCommand(state);
                if (Boolean.TRUE.equals(properties.getAutoPollEnabled())) {
                    pollIfNecessary(state);
                }
                checkTimeout(state);
            } catch (Exception e) {
                log.error("蓄电池采集通道异常, 通道={}, 串口={}",
                        state.getConfig().getName(),
                        state.getConfig().getPortName(),
                        e);
                collectorDeviceStateService.persistChannelError(state, e);
                closeQuietly(state);
                Threads.sleep(1000);
            }
            Threads.sleep(resolveLoopDelayMs());
        }
        closeQuietly(state);
    }

    /** 确保串口已打开，未打开则重新初始化。 */
    private void ensurePortOpen(BatteryCollectorChannelState state) {
        if (Boolean.TRUE.equals(state.getOpened().get()) && state.getSerialPort() != null && state.getSerialPort().isOpen()) {
            return;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        SerialPort serialPort = frameIoService.openSerialPort(config);
        state.setSerialPort(serialPort);
        state.getOpened().set(true);
        state.setRunState(BatteryCollectorRunState.READ);
        log.info("蓄电池采集通道已打开, 通道={}, 串口={}", config.getName(), config.getPortName());
        collectorDeviceStateService.persistSerialPortState(state, true);
    }

    /** 满足轮询间隔条件时触发一次轮询。已委托 pollingService。 */
    private void pollIfNecessary(BatteryCollectorChannelState state) {
        pollingService.pollIfNecessary(state,
                address -> sendCommand(state, BatteryDeviceProtocolCode.MODULE_INFO, address),
                () -> waitForPendingComplete(state),
                s -> processQueuedModuleCommand(s),
                () -> checkTimeout(state));
    }

    /** 执行一轮01/81全量或增量采集。 */
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
            int expectedCellCount = resolveExpectedCellCount(state.getConfig());
            int completedCellCount = 0;
            boolean skipRemainingCellAddresses = false;
            for (Integer address : resolvePollingAddresses(state, fullDiscovery)) {
                if (!running) {
                    break;
                }
                if (skipRemainingCellAddresses && isCellModuleAddress(address)) {
                    continue;
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
                    if (isCellModuleAddress(address)) {
                        completedCellCount++;
                    }
                }
                if (shouldSkipRemainingCellDiscovery(fullDiscovery, address, completedCellCount, expectedCellCount)) {
                    skipRemainingCellAddresses = true;
                }
                processQueuedModuleCommandsImmediately(state);
            }
            realtimeConsumer.flushCurrentPollBatch(state.getConfig());
        } finally {
            BatteryModulePollContextHolder.clear();
            state.setCurrentPollAddress(0);
            state.setCurrentFullDiscovery(false);
        }
        protocolLogService.logPollSummary(state, fullDiscovery, polledCommands, completedCommands);
    }

    private void processQueuedModuleCommandsImmediately(BatteryCollectorChannelState state) {
        while (running && state.getPendingCommand() == null && !state.getQueuedModuleCommands().isEmpty()) {
            if (!processQueuedModuleCommand(state)) {
                break;
            }
            waitForPendingComplete(state);
        }
    }

    /** 取出一条排队的控制命令并下发。 */
    private boolean processQueuedModuleCommand(BatteryCollectorChannelState state) {
        if (state.getPendingCommand() != null) {
            return false;
        }
        BatteryModuleControlCommand command = state.getQueuedModuleCommands().poll();
        if (command == null) {
            return false;
        }
        BatteryCollectorFrame request = frameCodec.buildRequest(
                command.getAddress(),
                command.getRequestCode(),
                command.getPayload() == null ? new byte[0] : command.getPayload());
        if (command.getResponseCode() == null) {
            if (!writeFrameWithoutPending(state, request, command)) {
                state.getQueuedModuleCommands().offer(command);
                return false;
            }
            return true;
        }
        if (!writeFrame(state, request, pendingFromCommand(command), BatteryCollectorRunState.WAIT_COMMAND_RESPONSE)) {
            state.getQueuedModuleCommands().offer(command);
            return false;
        }
        return true;
    }

    /** 将控制命令转换为等待响应的待处理请求。已委托 commandQueueService。 */
    private BatteryPendingRequest pendingFromCommand(BatteryModuleControlCommand command) {
        return commandQueueService.pendingFromCommand(command);
    }

    /** 向指定地址发送轮询命令并设置等待状态。 */
    private void sendCommand(BatteryCollectorChannelState state, BatteryDeviceProtocolCode pollingCommand, int address) {
        byte[] payload = new byte[0];
        BatteryCollectorFrame request = frameCodec.buildRequest(
                address,
                pollingCommand.getRequestCode(),
                payload);
        writeFrame(state, request, BatteryPendingRequest.fromProtocolCode(pollingCommand, address, payload, true), BatteryCollectorRunState.WAIT_RESPONSE);
    }

    /** 阻塞等待当前待处理命令完成或超时。 */
    private void waitForPendingComplete(BatteryCollectorChannelState state) {
        while (running && state.getPendingCommand() != null) {
            Threads.sleep(Math.max(10, Math.min(resolveRequestGapMs(), 100)));
            readOnce(state);
            checkTimeout(state);
        }
    }

    /** 写入帧到串口并设置等待响应状态。 */
    private boolean writeFrame(BatteryCollectorChannelState state, BatteryCollectorFrame frame,
                               BatteryPendingRequest pendingRequest, BatteryCollectorRunState waitingState) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort == null || !isSerialPortOpen(serialPort)) {
            return false;
        }
        byte[] bytes = frame.toByteArray();
        int written = writeSerialBytes(serialPort, bytes);
        if (written != bytes.length) {
            log.warn("蓄电池指令写入不完整, 通道={}, 请求={}, 预期={}, 实际={}",
                    state.getConfig().getName(),
                    String.format("%02X", pendingRequest.getRequestCode()),
                    bytes.length,
                    written);
            return false;
        }
        state.setLastSendTime(System.currentTimeMillis());
        protocolLogService.logProtocol(properties, state, "tx", "cmd=" + String.format("%02X", pendingRequest.getRequestCode())
                + ", expect=" + String.format("%02X", pendingRequest.getResponseCode())
                + ", retry=" + state.getCurrentRetryCount()
                + ", mode=" + waitingState
                + ", hex=" + frame.toHex());
        state.setPendingCommand(pendingRequest);
        state.setLastRequestCode(pendingRequest.getRequestCode());
        state.setExpectedResponseCode(pendingRequest.getResponseCode());
        state.setLastPendingTimedOut(false);
        state.setRunState(waitingState);
        return true;
    }

    /** 写入帧到串口但不等待响应。 */
    private boolean writeFrameWithoutPending(BatteryCollectorChannelState state,
                                             BatteryCollectorFrame frame,
                                             BatteryModuleControlCommand command) {
        SerialPort serialPort = state.getSerialPort();
        if (serialPort == null || !isSerialPortOpen(serialPort)) {
            return false;
        }
        byte[] bytes = frame.toByteArray();
        int written = writeSerialBytes(serialPort, bytes);
        if (written != bytes.length) {
            log.warn("蓄电池指令写入不完整, 通道={}, 请求={}, 预期={}, 实际={}",
                    state.getConfig().getName(),
                    String.format("%02X", command.getRequestCode()),
                    bytes.length,
                    written);
            return false;
        }
        state.setLastSendTime(System.currentTimeMillis());
        state.setLastRequestCode(command.getRequestCode());
        state.setExpectedResponseCode(0);
        state.setLastPendingTimedOut(false);
        state.setRunState(BatteryCollectorRunState.READ);
        markCompletedModuleCommand(state, command.getProtocolCode().name(), 0, true);
        // 连接条测试 0F 发送成功后，立即排队首个 11/91 电压读取命令
        if (command.getProtocolCode() == BatteryDeviceProtocolCode.CONNECT_STRIP_RESISTANCE_TEST
                && command.getConnectResistanceNextAddress() != null
                && command.getConnectResistanceMaxAddress() != null) {
            BatteryPendingRequest pendingFrom0F = BatteryPendingRequest.fromProtocolCode(
                    command.getProtocolCode(), command.getAddress(),
                    command.getPayload() == null ? new byte[0] : command.getPayload(), false);
            pendingFrom0F.setConfigId(command.getConfigId());
            pendingFrom0F.setBatteryGroup(command.getBatteryGroup());
            pendingFrom0F.setMode(command.getMode());
            pendingFrom0F.setOptLogId(command.getOptLogId());
            pendingFrom0F.setConnectResistanceNextAddress(command.getConnectResistanceNextAddress());
            pendingFrom0F.setConnectResistanceMaxAddress(command.getConnectResistanceMaxAddress());
            queueNextConnectResistanceVoltageRead(state, pendingFrom0F);
        }
        if (shouldStopModeAfterNoResponseCommand(command)) {
            markModeStopped(command, true);
        }
        protocolLogService.logProtocol(properties, state, "tx", "cmd=" + String.format("%02X", command.getRequestCode())
                + ", expect=-"
                + ", retry=0"
                + ", mode=" + BatteryCollectorRunState.READ
                + ", hex=" + frame.toHex());
        return true;
    }

    /** @deprecated 使用 {@code frameIoService.isSerialPortOpen} 替代。 */
    @Deprecated
    protected boolean isSerialPortOpen(SerialPort serialPort) {
        return frameIoService.isSerialPortOpen(serialPort);
    }

    /** @deprecated 使用 {@code frameIoService.writeFrameBytes} 替代。 */
    @Deprecated
    protected int writeSerialBytes(SerialPort serialPort, byte[] bytes) {
        return serialPort.writeBytes(bytes, bytes.length);
    }

    /** 从串口读取数据并解码分发响应帧。 */
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
        protocolLogService.logProtocol(properties, state, "rx-bytes", "len=" + read + ", hex=" + bytesToHex(buffer, read));
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
            protocolLogService.logProtocol(properties, state, "rx-frame", "cmd=" + String.format("%02X", frame.getCommand())
                    + ", expect=" + String.format("%02X", state.getExpectedResponseCode())
                    + ", hex=" + frame.toHex());
            moduleFrameDispatcher.dispatch(state.getConfig(), frame);
            if (isCurrentPendingResponse(state, frame)) {
                handleCompletedPendingResponse(state, frame, state.getPendingCommand());
                state.setPendingCommand(null);
                state.setExpectedResponseCode(0);
                state.setCurrentRetryCount(0);
                state.setLastPendingCompletedAt(System.currentTimeMillis());
                state.setLastPendingTimedOut(false);
                state.setRunState(BatteryCollectorRunState.READ);
            } else if (isKnownModuleResponse(frame.getCommand())) {
                log.debug("蓄电池采集响应帧不在当前等待范围内, 通道={}, 请求={}, 期望={}, 实际={}",
                        state.getConfig().getName(),
                        String.format("%02X", state.getLastRequestCode()),
                        String.format("%02X", state.getExpectedResponseCode()),
                        String.format("%02X", frame.getCommand()));
            } else {
                log.info("蓄电池采集收到非预期帧, 通道={}, 请求={}, 期望={}, 实际={}",
                        state.getConfig().getName(),
                        String.format("%02X", state.getLastRequestCode()),
                        String.format("%02X", state.getExpectedResponseCode()),
                        String.format("%02X", frame.getCommand()));
            }
        }
    }

    void handleCompletedPendingResponse(BatteryCollectorChannelState state,
                                        BatteryCollectorFrame frame,
                                        BatteryPendingRequest pendingRequest) {
        if (state == null || frame == null || pendingRequest == null || pendingRequest.isAutoPoll()) {
            return;
        }
        boolean success = isSuccessResponse(frame, pendingRequest);
        markCompletedModuleCommand(state, pendingRequest.getName(), frame.getCommand(), success);
        // 连接条测试 91 响应不在中间步骤更新日志状态，只在最终完成时更新
        if (!BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.name().equals(pendingRequest.getName())) {
            commandLogService.updateCommandOptLog(pendingRequest.getOptLogId(), success ? BatteryDeviceStateConstants.CommandStatus.SUCCESS : BatteryDeviceStateConstants.CommandStatus.FAILED, frame.getCommand(), bytesToHex(frame.getPayloadSafe()));
        }
        if (BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.name().equals(pendingRequest.getName())) {
            if (!success) {
                log.warn("自动编号失败, 通道={}, 地址={}, 响应={}",
                        state.getConfig() == null ? null : state.getConfig().getName(),
                        String.format("%02X", pendingRequest.getRequestAddress()),
                        String.format("%02X", frame.getCommand()));
                markModeStopped(pendingRequest, false);
                return;
            }
            if (queueNextAutoSetAddressStep(state, frame, pendingRequest)) {
                return;
            }
            markModeStopped(pendingRequest, true);
            cacheService.resetModuleAddressCache(state, realtimeSnapshotService);
            log.info("自动编号成功后蓄电池模块地址缓存已重置, 通道={}",
                    state.getConfig() == null ? null : state.getConfig().getName());
            return;
        }
        if (BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.name().equals(pendingRequest.getName())) {
            if (!success) {
                pendingRequest.setConnectResistanceFailed(true);
            }
            if (success) {
                storeConnectResistanceResult(pendingRequest, frame);
            }
            if (!queueNextConnectResistanceVoltageRead(state, pendingRequest)) {
                // 所有地址完成，按最后一个响应写最终状态
                boolean finalSuccess = success && !pendingRequest.isConnectResistanceFailed();
                String finalStatus = finalSuccess ? BatteryDeviceStateConstants.CommandStatus.SUCCESS : BatteryDeviceStateConstants.CommandStatus.FAILED;
                commandLogService.updateCommandOptLog(pendingRequest.getOptLogId(), finalStatus, frame.getCommand(), bytesToHex(frame.getPayloadSafe()));
                markModeStopped(pendingRequest, finalSuccess);
            }
            return;
        }
        markModeStopped(pendingRequest, success);
        if (success && shouldResetModuleAddressCacheAfterCommand(pendingRequest)) {
            cacheService.resetModuleAddressCache(state, realtimeSnapshotService);
            log.info("地址命令成功后蓄电池模块地址缓存已重置, 通道={}, 命令={}",
                    state.getConfig() == null ? null : state.getConfig().getName(),
                    pendingRequest.getName());
        }
    }

    /** 判断响应帧是否表示操作成功。已委托 commandQueueService。 */
    private boolean isSuccessResponse(BatteryCollectorFrame frame, BatteryPendingRequest pendingRequest) {
        return commandQueueService.isSuccessResponse(frame, pendingRequest);
    }

    /** 排队下一个连接条电阻测试电压读取命令。返回 true 表示已排队，false 表示测试完成。 */
    private boolean queueNextConnectResistanceVoltageRead(BatteryCollectorChannelState state,
                                                           BatteryPendingRequest pendingRequest) {
        Integer nextAddress = pendingRequest.getConnectResistanceNextAddress();
        Integer maxAddress = pendingRequest.getConnectResistanceMaxAddress();
        if (nextAddress == null || maxAddress == null || nextAddress > maxAddress) {
            return false;
        }
        int address = nextAddress;
        pendingRequest.setConnectResistanceNextAddress(address + 1);
        BatteryModuleControlCommand command = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE)
                .address(address)
                .requestCode(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.getRequestCode())
                .responseCode(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.getResponseCode())
                .payload(new byte[0])
                .description(BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.getDescription())
                .batteryGroup(pendingRequest.getBatteryGroup())
                .mode(pendingRequest.getMode())
                .optLogId(pendingRequest.getOptLogId())
                .connectResistanceNextAddress(address + 1)
                .connectResistanceMaxAddress(maxAddress)
                .connectResistanceFailed(pendingRequest.isConnectResistanceFailed())
                .build();
        return state.getQueuedModuleCommands().offer(command);
    }

    /**
     * 解析 91 响应帧中的连接条测试电压并计算电阻。
     * <p>
     * 91 响应 8 字节：BatteryVoltage(4B) + TestVoltage(4B)，大端序。
     * 原始值单位：0.1mV（与 BatteryModuleFrameDataParserService 的 ÷10000 换算一致）。
     */
    private void storeConnectResistanceResult(BatteryPendingRequest pendingRequest, BatteryCollectorFrame frame) {
        try {
            byte[] payload = frame.getPayloadSafe();
            if (payload.length < 8) {
                log.debug("连接条测试响应载荷不足8字节, 地址={}, 长度={}",
                        pendingRequest.getRequestAddress(), payload.length);
                return;
            }
            long batteryVoltageRaw = u32(payload, 0);
            long testVoltageRaw = u32(payload, 4);
            Integer batteryGroup = pendingRequest.getBatteryGroup();
            int address = pendingRequest.getRequestAddress();

            log.info("连接条测试电压记录, 电池组={}, 地址={}, 电池电压raw={}, 测试电压raw={}",
                     batteryGroup, address, batteryVoltageRaw, testVoltageRaw);

            // 获取实时组电流以计算真实连接条电阻
            Double current = null;
            if (realtimeMapper != null) {
                com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime group = realtimeMapper.selectGroup(batteryGroup);
                if (group != null) {
                    if (group.getChargeDischargeCurrent() != null) {
                        current = group.getChargeDischargeCurrent();
                    } else {
                        current = group.getPackCurrent();
                    }
                }
            }

            Double connectBatteryVoltage = batteryVoltageRaw / 10000.0d;
            Double connectTestVoltage = testVoltageRaw / 10000.0d;
            Double resistance = calculateConnectResistance(connectTestVoltage, connectBatteryVoltage, current);

            if (resistance != null) {
                compatibilityFillService.putConnectResistance(batteryGroup, address, resistance);
                // 同时把计算好的连接条电阻保存到 DB 中的单体实时数据表 (battery_module_cell_realtime) 中
                if (realtimeMapper != null) {
                    List<com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime> cells = realtimeMapper.selectCells(batteryGroup);
                    boolean cellFound = false;
                    if (cells != null) {
                        for (com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime cell : cells) {
                            if (cell.getBatNum() != null && cell.getBatNum() == address) {
                                cell.setResistanceRageSlip(resistance);
                                realtimeMapper.upsertCell(cell);
                                cellFound = true;
                                break;
                            }
                        }
                    }
                    if (!cellFound) {
                        com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime newCell = new com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime();
                        newCell.setPackNum(batteryGroup);
                        newCell.setBatNum(address);
                        newCell.setResistanceRageSlip(resistance);
                        newCell.setCreateTime(new java.util.Date());
                        realtimeMapper.upsertCell(newCell);
                    }
                }
                log.info("连接条电阻计算成功且已写入, 电池组={}, 地址={}, 电阻={} uΩ",
                        batteryGroup, address, resistance);
            } else {
                log.warn("连接条电阻计算跳过, 缺少输入或电流过低: 电池组={}, 地址={}, 电流={}",
                        batteryGroup, address, current);
            }
        } catch (Exception e) {
            log.warn("解析连接条电阻测试失败, 地址={}, 原因={}", pendingRequest.getRequestAddress(), e.getMessage());
        }
    }

    /**
     * 计算真实连接条电阻。
     *
     * @param testVoltage 测试电压 (V)
     * @param batteryVoltage 电池电压 (V)
     * @param current 充放电电流 (A)
     * @return 连接条电阻 (uΩ)，无法计算时返回 null
     */
    public static Double calculateConnectResistance(Double testVoltage, Double batteryVoltage, Double current) {
        if (testVoltage == null || batteryVoltage == null || current == null) {
            return null;
        }
        double absCurrent = Math.abs(current);
        if (absCurrent <= 0.1) {
            return null;
        }
        double deltaV = Math.abs(testVoltage - batteryVoltage);
        double resistance = (deltaV / absCurrent) * 1000000.0d;
        // 四舍五入保留四位小数以消除浮点数精度误差
        resistance = Math.round(resistance * 10000.0d) / 10000.0d;
        // 限制在合理连接条电阻范围（非负且小于1欧姆）
        if (resistance < 0 || resistance > 1000000.0d) {
            return null;
        }
        return resistance;
    }

    /** 无符号 32 位解析（大端序）。 */
    private long u32(byte[] payload, int offset) {
        return ((long) (payload[offset] & 0xFF) << 24)
                | ((long) (payload[offset + 1] & 0xFF) << 16)
                | ((long) (payload[offset + 2] & 0xFF) << 8)
                | (long) (payload[offset + 3] & 0xFF);
    }

    private boolean shouldResetModuleAddressCacheAfterCommand(BatteryPendingRequest pendingRequest) {
        String name = pendingRequest.getName();
        return BatteryDeviceProtocolCode.SET_MODULE_ADDRESS.name().equals(name);
    }

    /** 自动编号完成后排队下一步或发送停止命令。 */
    private boolean queueNextAutoSetAddressStep(BatteryCollectorChannelState state,
                                                BatteryCollectorFrame frame,
                                                BatteryPendingRequest pendingRequest) {
        Integer batteryCount = pendingRequest.getAutoAddressBatteryCount();
        Integer batterySpecification = pendingRequest.getAutoAddressBatterySpecification();
        if (batteryCount == null || batterySpecification == null) {
            return false;
        }
        int currentAddress = pendingRequest.getRequestAddress();
        if (currentAddress == GROUP_MODULE_ADDRESS) {
            return offerAutoSetAddressStep(state, pendingRequest, 1, firstAutoSetAddressCellPayload(batterySpecification));
        }
        if (currentAddress < batteryCount) {
            return offerAutoSetAddressStep(state, pendingRequest, currentAddress + 1, nextAutoSetAddressPayload(frame.getPayloadSafe()));
        }
        byte[] stopPayload = stopAutoSetAddressPayload(frame.getPayloadSafe());
        state.getQueuedModuleCommands().offer(autoSetAddressCommand(pendingRequest, currentAddress, stopPayload, false));
        BatteryModuleControlCommand stopGroupCommand = autoSetAddressCommand(pendingRequest, GROUP_MODULE_ADDRESS, stopPayload, false);
        stopGroupCommand.setMode(pendingRequest.getMode());
        state.getQueuedModuleCommands().offer(stopGroupCommand);
        cacheService.resetModuleAddressCache(state, realtimeSnapshotService);
        log.info("自动编号成功后蓄电池模块地址缓存已重置, 通道={}",
                state.getConfig() == null ? null : state.getConfig().getName());
        return true;
    }

    /** 构造并排队自动编号的下一步命令。 */
    private boolean offerAutoSetAddressStep(BatteryCollectorChannelState state,
                                            BatteryPendingRequest pendingRequest,
                                            int address,
                                            byte[] payload) {
        BatteryModuleControlCommand command = autoSetAddressCommand(pendingRequest, address, payload, true);
        markModeRunning(command);
        return state.getQueuedModuleCommands().offer(command);
    }

    /** 构造自动编号控制命令。 */
    private BatteryModuleControlCommand autoSetAddressCommand(BatteryPendingRequest pendingRequest,
                                                             int address,
                                                             byte[] payload,
                                                             boolean waitResponse) {
        return BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS)
                .address(address)
                .requestCode(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.getRequestCode())
                .responseCode(waitResponse ? BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.getResponseCode() : null)
                .payload(payload)
                .description(BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.getDescription())
                .batteryGroup(pendingRequest.getBatteryGroup())
                .mode(waitResponse ? pendingRequest.getMode() : null)
                .autoAddressBatteryCount(pendingRequest.getAutoAddressBatteryCount())
                .autoAddressBatterySpecification(pendingRequest.getAutoAddressBatterySpecification())
                .build();
    }

    /** 构造自动编号第一个单体的协议载荷。 */
    private byte[] firstAutoSetAddressCellPayload(int batterySpecification) {
        // 电压×10编码(分辨率0.1V)，拆为大端两字节
        int startVoltage = batterySpecificationToVoltage(batterySpecification) * 10;
        return new byte[]{
                (byte) ((startVoltage >> 8) & 0xFF),
                (byte) (startVoltage & 0xFF),
                (byte) (batterySpecification & 0xFF),
                0,
                0,
                0,
                START_SET_ADDRESS
        };
    }

    /** 根据上一步响应构造下一步载荷。 */
    private byte[] nextAutoSetAddressPayload(byte[] responsePayload) {
        return new byte[]{
                responsePayload[0],
                responsePayload[1],
                responsePayload[2],
                0,
                0,
                0,
                START_SET_ADDRESS
        };
    }

    /** 根据上一步响应构造停止载荷。 */
    private byte[] stopAutoSetAddressPayload(byte[] responsePayload) {
        return new byte[]{
                responsePayload[0],
                responsePayload[1],
                responsePayload[2],
                0,
                0,
                0,
                STOP_SET_ADDRESS
        };
    }

    private int batterySpecificationToVoltage(int batterySpecification) {
        if (batterySpecification == BATTERY_SPEC_2V) {
            return VOLTAGE_2V;
        }
        if (batterySpecification == BATTERY_SPEC_12V) {
            return VOLTAGE_12V;
        }
        throw new IllegalArgumentException("自动编号仅支持2V或12V电池规格");
    }

    private boolean isKnownModuleResponse(int commandCode) {
        BatteryDeviceProtocolCode protocolCode = BatteryDeviceProtocolCode.find(commandCode);
        return protocolCode != null && protocolCode.isResponse(commandCode);
    }

    /** 判断帧是否匹配当前等待的响应。 */
    private boolean isCurrentPendingResponse(BatteryCollectorChannelState state, BatteryCollectorFrame frame) {
        BatteryPendingRequest pendingRequest = state.getPendingCommand();
        // 迟到帧可能仍是 81 响应，必须同时匹配模块地址才完成当前等待。
        return pendingRequest != null
                && frame.getCommand() == state.getExpectedResponseCode()
                && frame.getAddress() == pendingRequest.getRequestAddress();
    }

    /** 接收缓冲区超限时截断保留尾部。已委托 frameIoService.trimBuffer。 */
    private void trimReceiveBufferIfNecessary(BatteryCollectorChannelState state) {
        frameIoService.trimBuffer(state.getReceiveBuffer(), resolveReceiveBufferLimit(state.getConfig()));
    }

    /** 检测当前等待命令是否超时，超时则重试或放弃。 */
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
            log.warn("蓄电池采集响应超时，正在重试, 通道={}, 请求={}, 响应={}, 重试={}/{}",
                    state.getConfig().getName(),
                    String.format("%02X", state.getLastRequestCode()),
                    String.format("%02X", state.getExpectedResponseCode()),
                    state.getCurrentRetryCount(),
                    maxRetryCount);
            protocolLogService.logProtocol(properties, state, "retry", "request=" + String.format("%02X", state.getLastRequestCode())
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

        log.warn("蓄电池采集响应超时, 通道={}, 请求={}, 响应={}, 超时次数={}",
                state.getConfig().getName(),
                String.format("%02X", state.getLastRequestCode()),
                String.format("%02X", state.getExpectedResponseCode()),
                state.getTimeoutCount() + 1);
        BatteryPendingRequest timedOutRequest = state.getPendingCommand();
        collectorDeviceStateService.persistModuleTimeout(state, timedOutRequest);
        handleTimedOutPendingRequest(state, timedOutRequest);
        state.setPendingCommand(null);
        state.setExpectedResponseCode(0);
        state.setCurrentRetryCount(0);
        state.setLastPendingCompletedAt(System.currentTimeMillis());
        state.setLastPendingTimedOut(true);
        state.setRunState(BatteryCollectorRunState.READ);
        state.setTimeoutCount(state.getTimeoutCount() + 1);
        state.setLastTimeoutTime(now);
        state.getReceiveBuffer().reset();
        collectorDeviceStateService.persistPollTimeout(state);
    }

    void handleTimedOutPendingRequest(BatteryCollectorChannelState state, BatteryPendingRequest pendingRequest) {
        if (state == null || pendingRequest == null || pendingRequest.isAutoPoll()) {
            return;
        }
        markCompletedModuleCommand(state, pendingRequest.getName(), pendingRequest.getResponseCode(), false);
        markModeStopped(pendingRequest, false);
        commandLogService.updateCommandOptLog(pendingRequest.getOptLogId(), BatteryDeviceStateConstants.CommandStatus.TIMEOUT, null, null);
    }

    /** 标记工作模式已停止。 */
    private void markModeStopped(BatteryModuleControlCommand command, boolean success) {
        if (command == null || command.getMode() == null) {
            return;
        }
        batteryModeStatusService.markStopped(
                command.getBatteryGroup(),
                command.getMode(),
                modeAddress(command),
                success,
                command.getOptLogId());
    }

    private boolean shouldStopModeAfterNoResponseCommand(BatteryModuleControlCommand command) {
        if (command == null || command.getMode() == null) {
            return false;
        }
        if (command.getMode() == BatteryModeStatusService.MODE_CONNECT_RESISTANCE) {
            return false;
        }
        return true;
    }

    /** 获取模式关联的模块地址，自动编号场景取电池数量。 */
    private Integer modeAddress(BatteryModuleControlCommand command) {
        if (command == null) {
            return null;
        }
        if (command.getProtocolCode() == BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS
                && command.getAddress() == GROUP_MODULE_ADDRESS
                && command.getAutoAddressBatteryCount() != null) {
            return command.getAutoAddressBatteryCount();
        }
        return command.getAddress();
    }

    /** 标记工作模式已停止（从待处理请求）。 */
    private void markModeStopped(BatteryPendingRequest pendingRequest, boolean success) {
        if (pendingRequest == null || pendingRequest.getMode() == null) {
            return;
        }
        batteryModeStatusService.markStopped(
                pendingRequest.getBatteryGroup(),
                pendingRequest.getMode(),
                pendingRequest.getRequestAddress(),
                success,
                pendingRequest.getOptLogId());
    }

    /** 记录已完成模块命令的状态。 */
    private void markCompletedModuleCommand(BatteryCollectorChannelState state,
                                            String commandName,
                                            int responseCode,
                                            boolean success) {
        state.setLastCompletedModuleCommandName(commandName);
        state.setLastCompletedModuleResponseCode(responseCode);
        state.setLastCompletedModuleCommandSuccess(success);
        state.setLastCompletedModuleCommandTime(System.currentTimeMillis());
    }

    /** 创建600模块命令操作日志。 */
    private Long createCommandOptLog(BatteryCollectorChannelConfig config, BatteryModuleControlCommand command) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String now = sdf.format(new Date());
            OptLog optLog = new OptLog();
            optLog.setId(com.shanhe.common.utils.uuid.IdUtils.getSnowflakeId());
            optLog.setConfigId(config == null ? null : config.getConfigId());
            optLog.setPackNum(command.getBatteryGroup());
            optLog.setType(BatteryTestEnum._99.getDictValue());
            optLog.setContent(command.getDescription());
            optLog.setCreateTimeStr(now);
            optLog.setSource(BatteryDeviceStateConstants.Source.COLLECTOR);
            optLog.setChannelName(config == null ? null : config.getName());
            optLog.setTargetType("module");
            optLog.setTargetAddress(command.getAddress());
            optLog.setMode(command.getMode());
            optLog.setStatus(BatteryDeviceStateConstants.CommandStatus.PENDING);
            optLog.setRequestCode(command.getRequestCode());
            optLog.setResponseCode(command.getResponseCode());
            optLog.setProtocolCode(command.getProtocolCode() == null ? null : command.getProtocolCode().name());
            optLog.setCommandName(command.getProtocolCode() == null ? null : command.getProtocolCode().getDescription());
            optLog.setRequestPayload(bytesToHex(command.getPayload()));
            optLog.setStartedAt(now);
            optLogMapper.insert(optLog);
            return optLog.getId();
        } catch (Exception e) {
            log.warn("创建600模块命令日志失败, 通道={}, 命令={}, 原因={}",
                    config == null ? null : config.getName(),
                    command.getProtocolCode(), e.getMessage());
            return null;
        }
    }

    /** 更新600模块命令操作日志状态。 */
    private void updateCommandOptLog(Long optLogId, String status, Integer responseCode, String responsePayload) {
        if (optLogId == null) {
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String now = sdf.format(new Date());
            String errorMessage = BatteryDeviceStateConstants.CommandStatus.TIMEOUT.equals(status) ? "命令响应超时" : null;
            optLogMapper.updateCommandStatus(optLogId, status, responseCode, now, errorMessage, responsePayload);
        } catch (Exception e) {
            log.warn("更新600模块命令日志失败, 日志ID={}, 原因={}", optLogId, e.getMessage());
        }
    }

    /** 持久化通道异常到 battery_device_state。 */
    private void persistChannelError(BatteryCollectorChannelState state, Exception e) {
        String channelName = state.getConfig().getName();
        String stateValue = e.getMessage() == null ? "unknown" : e.getMessage();
        BatteryDeviceState ds = buildChannelState(channelName, state.getConfig(),
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                stateValue,
                BatteryDeviceStateConstants.StateLevel.ERROR, null);
        persistIfChanged(channelName, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, stateValue, ds);
    }

    /** 通道重新打开时清除异常状态（更新为正常）。 */
    private void clearChannelError(String channelName, BatteryCollectorChannelConfig config) {
        String cacheKey = channelName + ":" + BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR;
        if (lastStateValues.containsKey(cacheKey)) {
            BatteryDeviceState ds = buildChannelState(channelName, config,
                    BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "cleared",
                    BatteryDeviceStateConstants.StateLevel.NORMAL, null);
            persistIfChanged(channelName, BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, "cleared", ds);
        }
    }

    /** 持久化通道串口状态到 battery_device_state（带去重）。 */
    private void persistSerialPortState(BatteryCollectorChannelState state, boolean opened) {
        String stateValue = opened ? "open" : "closed";
        String stateLevel = opened ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.ERROR;
        persistChannelStateThrottled(state.getConfig().getName(), state.getConfig(),
                STATE_CODE_SERIAL_PORT, stateValue, stateLevel, null);
        // 通道重新打开时，清除之前的异常状态
        if (opened) {
            clearChannelError(state.getConfig().getName(), state.getConfig());
        }
    }

    /** 持久化通道轮询超时计数到 battery_device_state（带去重）。 */
    private void persistPollTimeout(BatteryCollectorChannelState state) {
        String stateValue = String.valueOf(state.getTimeoutCount());
        String stateLevel = state.getTimeoutCount() > 0 ? BatteryDeviceStateConstants.StateLevel.WARN : BatteryDeviceStateConstants.StateLevel.NORMAL;
        persistChannelStateThrottled(state.getConfig().getName(), state.getConfig(),
                STATE_CODE_POLL_TIMEOUT, stateValue, stateLevel, null);
    }

    /** 持久化具体模块超时状态到 battery_device_state（带去重）。 */
    private void persistModuleTimeout(BatteryCollectorChannelState state, BatteryPendingRequest pendingRequest) {
        if (state == null || pendingRequest == null) {
            return;
        }
        BatteryCollectorChannelConfig config = state.getConfig();
        int address = pendingRequest.getRequestAddress();
        if (address < 0 || address > UNSIGNED_BYTE_MAX) {
            return;
        }
        String scopeKey = config.getName() + ":" + address;
        String stateValue = String.format("%02X/%02X",
                pendingRequest.getRequestCode(),
                pendingRequest.getResponseCode());
        BatteryDeviceState ds = buildModuleState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT,
                stateValue,
                BatteryDeviceStateConstants.StateLevel.WARN,
                pendingRequest.getOptLogId(),
                address);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, stateValue, ds);
    }

    /** 模块重新响应时清除超时状态（更新为已恢复）。 */
    private void clearModuleTimeout(String channelName, BatteryCollectorChannelConfig config, int address) {
        String scopeKey = channelName + ":" + address;
        String cacheKey = scopeKey + ":" + BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT;
        if (lastStateValues.containsKey(cacheKey)) {
            BatteryDeviceState ds = buildModuleState(scopeKey, config,
                    BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "recovered",
                    BatteryDeviceStateConstants.StateLevel.NORMAL, null, address);
            persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "recovered", ds);
        }
    }

    /** 持久化模块活跃状态到 battery_device_state（带去重）。 */
    private void persistModuleActive(String channelName, BatteryCollectorChannelConfig config,
                                     int address, boolean active) {
        String scopeKey = channelName + ":" + address;
        String stateValue = active ? "active" : "inactive";
        String stateLevel = active ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.WARN;
        BatteryDeviceState ds = buildModuleState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE,
                stateValue, stateLevel, null, address);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, stateValue, ds);
    }

    /** 持久化 246 组模块新鲜度到 battery_device_state（带去重）。 */
    private void persistGroup246Freshness(BatteryCollectorChannelConfig config, boolean fresh) {
        String scopeKey = String.valueOf(config.getBatteryGroup());
        String stateValue = fresh ? "fresh" : "stale";
        String stateLevel = fresh ? BatteryDeviceStateConstants.StateLevel.NORMAL : BatteryDeviceStateConstants.StateLevel.WARN;
        BatteryDeviceState ds = buildChannelState(scopeKey, config,
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, stateValue, stateLevel, null);
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
        persistIfChanged(scopeKey, BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, stateValue, ds);
    }

    /** 构造通道级 BatteryDeviceState 对象。 */
    private BatteryDeviceState buildChannelState(String scopeKey, BatteryCollectorChannelConfig config,
                                                  String stateCode, String stateValue, String stateLevel, Long optLogId) {
        BatteryDeviceState ds = new BatteryDeviceState();
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.CHANNEL);
        ds.setScopeKey(scopeKey);
        ds.setChannelName(config.getName());
        ds.setPackNum(config.getBatteryGroup());
        ds.setStateCode(stateCode);
        ds.setStateValue(stateValue);
        ds.setStateLevel(stateLevel);
        ds.setSource(BatteryDeviceStateConstants.Source.COLLECTOR);
        ds.setOptLogId(optLogId);
        ds.setFirstSeenTime(new Date());
        ds.setLastChangeTime(new Date());
        return ds;
    }

    /** 构造模块级 BatteryDeviceState 对象。 */
    private BatteryDeviceState buildModuleState(String scopeKey, BatteryCollectorChannelConfig config,
                                                String stateCode, String stateValue, String stateLevel,
                                                Long optLogId, int address) {
        BatteryDeviceState ds = buildChannelState(scopeKey, config, stateCode, stateValue, stateLevel, optLogId);
        ds.setScopeType(BatteryDeviceStateConstants.ScopeType.MODULE);
        ds.setSourceRefId(String.valueOf(address));
        if (isCellModuleAddress(address)) {
            ds.setModelNum(address);
        }
        return ds;
    }

    /** 通道级状态写入（带去重），相同 scopeKey+stateCode+stateValue 不重复写库。 */
    private void persistChannelStateThrottled(String channelName, BatteryCollectorChannelConfig config,
                                               String stateCode, String stateValue, String stateLevel, Long optLogId) {
        String scopeKey = channelName;
        BatteryDeviceState ds = buildChannelState(scopeKey, config, stateCode, stateValue, stateLevel, optLogId);
        persistIfChanged(scopeKey, stateCode, stateValue, ds);
    }

    /** 仅当 stateValue 变化时写库，避免重复写入。 */
    private void persistIfChanged(String scopeKey, String stateCode, String stateValue, BatteryDeviceState ds) {
        String cacheKey = scopeKey + ":" + stateCode;
        String previous = lastStateValues.get(cacheKey);
        if (stateValue.equals(previous)) {
            return;
        }
        try {
            batteryDeviceStateService.upsert(ds);
            lastStateValues.put(cacheKey, stateValue);
            // 防止缓存无限增长：超过阈值时清理模块/pack 级高基数条目，保留通道级去重状态。
            if (lastStateValues.size() > 1000) {
                lastStateValues.keySet().removeIf(this::isHighCardinalityStateCacheKey);
            }
        } catch (Exception e) {
            log.warn("持久化设备状态失败, scopeKey={}, stateCode={}, 原因={}", scopeKey, stateCode, e.getMessage());
        }
    }

    private boolean isHighCardinalityStateCacheKey(String cacheKey) {
        if (cacheKey == null) {
            return false;
        }
        return cacheKey.contains(":" + BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT)
                || cacheKey.contains(":" + BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE)
                || cacheKey.endsWith(":" + BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
    }

    /** 静默关闭串口并重置通道状态。 */
    private void closeQuietly(BatteryCollectorChannelState state) {
        frameIoService.closeQuietly(state.getSerialPort());
        boolean wasOpened = state.getOpened().get();
        state.getOpened().set(false);
        state.setSerialPort(null);
        state.setPendingCommand(null);
        state.setExpectedResponseCode(0);
        state.setCurrentRetryCount(0);
        state.setLastPendingTimedOut(false);
        state.setRunState(BatteryCollectorRunState.READ);
        state.getReceiveBuffer().reset();
        if (wasOpened) {
            collectorDeviceStateService.persistSerialPortState(state, false);
        }
    }

    /** 按调试配置输出协议收发日志。 */
    private void logProtocol(BatteryCollectorChannelState state, String stage, String message) {
        if (!Boolean.TRUE.equals(properties.getDebugEnabled())) {
            return;
        }
        List<String> debugChannels = properties.getDebugChannels();
        if (debugChannels != null && !debugChannels.isEmpty() && !debugChannels.contains(state.getConfig().getName())) {
            return;
        }
        log.info("蓄电池采集协议, 通道={}, 串口={}, 阶段={}, {}",
                state.getConfig().getName(),
                state.getConfig().getPortName(),
                stage,
                message);
    }

    /** 输出本轮轮询汇总日志。 */
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
        log.info("蓄电池采集轮询汇总, 通道={}, 运行状态={}, 全量发现={}, 轮询数={}, 完成数={}, 活跃地址数={}, 已完成={}, 等待中={}, 超时次数={}",
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

    /** 将命令列表格式化为摘要字符串。 */
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

    /** 将字节数组转为十六进制字符串（null 安全）。 */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return BatteryCollectorFrameIoService.bytesToHex(bytes, bytes.length);
    }

    /** 将字节数组转为十六进制字符串。 */
    private String bytesToHex(byte[] bytes, int length) {
        return BatteryCollectorFrameIoService.bytesToHex(bytes, length);
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

    int resolveExpectedCellCount(BatteryCollectorChannelConfig config) {
        Integer configured = config == null ? null : config.getExpectedCellCount();
        int count = sanitizeExpectedCellCount(configured);
        if (count > 0) {
            return count;
        }
        if (batteryPackService == null || config == null || config.getBatteryGroup() == null) {
            return 0;
        }
        try {
            return sanitizeExpectedCellCount(batteryPackService.getBatteryMaxNumber(config.getBatteryGroup()));
        } catch (Exception e) {
            log.warn("获取电池组期望单体数量失败, 电池组={}",
                    config.getBatteryGroup(),
                    e);
            return 0;
        }
    }

    /** 规范化期望单体数量，上限245。 */
    private int sanitizeExpectedCellCount(Integer count) {
        if (count == null || count <= 0) {
            return 0;
        }
        return Math.min(count, 245);
    }

    /** 解析正整数配置值，无效时返回默认值。 */
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

    /** 构造本轮轮询批次号。 */
    private String buildPollBatchNo(BatteryCollectorChannelState state, long startedAt) {
        String channelName = state.getConfig() == null ? "channel" : state.getConfig().getName();
        return channelName + "-" + startedAt;
    }

    /** 解析本轮轮询的模块地址列表。 */
    private List<Integer> resolvePollingAddresses(BatteryCollectorChannelState state, boolean fullDiscovery) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled()) || fullDiscovery) {
            return fullModuleAddressRange(state.getConfig());
        }
        List<Integer> activeAddresses = sortedActiveModuleAddresses(state);
        if (activeAddresses.isEmpty()) {
            state.getFullDiscoveryRequested().set(true);
            return fullModuleAddressRange(state.getConfig());
        }
        appendRequiredGroupModuleAddress(activeAddresses);
        return activeAddresses;
    }

    /** 确保地址列表包含246组模块地址。 */
    private void appendRequiredGroupModuleAddress(List<Integer> addresses) {
        int groupModuleAddress = 246;
        if (addresses.contains(groupModuleAddress)) {
            return;
        }
        addresses.add(groupModuleAddress);
        Collections.sort(addresses);
    }

    /** 生成全量模块地址范围。 */
    private List<Integer> fullModuleAddressRange(BatteryCollectorChannelConfig config) {
        List<Integer> addresses = new ArrayList<>();
        for (int address = resolveModuleAddressStart(config); address <= resolveModuleAddressEnd(config); address++) {
            addresses.add(address);
        }
        appendRequiredGroupModuleAddress(addresses);
        return addresses;
    }

    /** 返回排序后的活跃模块地址列表。 */
    private List<Integer> sortedActiveModuleAddresses(BatteryCollectorChannelState state) {
        List<Integer> addresses = new ArrayList<>(state.getActiveModuleAddresses());
        Collections.sort(addresses);
        return addresses;
    }

    /** 判断是否需要全量发现。 */
    private boolean shouldRunFullDiscovery(BatteryCollectorChannelState state, long now) {
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return true;
        }
        // 启动、人工重置、缓存为空或周期到期时回到 1..246 全量发现。
        if (state.getFullDiscoveryRequested().getAndSet(false)) {
            return true;
        }
        if (!hasActiveCellModuleAddress(state)) {
            return true;
        }
        Long interval = properties.getModuleAddressFullDiscoveryIntervalMs();
        return interval != null && interval > 0
                && state.getLastFullDiscoveryTime() > 0
                && now - state.getLastFullDiscoveryTime() >= interval;
    }

    boolean shouldSkipRemainingCellDiscovery(boolean fullDiscovery,
                                             Integer currentAddress,
                                             int completedCellCount,
                                             int expectedCellCount) {
        return fullDiscovery
                && isCellModuleAddress(currentAddress)
                && expectedCellCount > 0
                && completedCellCount >= expectedCellCount;
    }

    /** 判断地址是否为单体模块地址(1-245)。 */
    private boolean isCellModuleAddress(Integer address) {
        return address != null && address >= 1 && address <= 245;
    }

    boolean hasActiveCellModuleAddress(BatteryCollectorChannelState state) {
        if (state == null || state.getActiveModuleAddresses().isEmpty()) {
            return false;
        }
        int start = resolveModuleAddressStart(state.getConfig());
        int end = Math.min(resolveModuleAddressEnd(state.getConfig()), 245);
        for (Integer address : state.getActiveModuleAddresses()) {
            if (address != null && address >= start && address <= end) {
                return true;
            }
        }
        return false;
    }

    /** 更新模块地址活跃缓存。 */
    private void updateModuleAddressCache(BatteryCollectorChannelState state, int address, boolean responded) {
        if (isGroupModuleAddress(address)) {
            collectorDeviceStateService.persistGroup246Freshness(state.getConfig(), responded);
        }
        if (!Boolean.TRUE.equals(properties.getModuleAddressCacheEnabled())) {
            return;
        }
        // 只缓存有响应地址；稳定运行后避免每轮扫描不存在的模块。
        if (responded) {
            boolean wasActive = state.getActiveModuleAddresses().contains(address);
            state.getActiveModuleAddresses().add(address);
            state.getModuleAddressMissCounts().remove(address);
            if (!wasActive) {
                collectorDeviceStateService.persistModuleActive(state.getConfig().getName(), state.getConfig(), address, true);
            }
            // 模块重新响应，清除超时状态
            collectorDeviceStateService.clearModuleTimeout(state.getConfig().getName(), state.getConfig(), address);
            return;
        }
        if (!state.getActiveModuleAddresses().contains(address)) {
            return;
        }
        int misses = state.getModuleAddressMissCounts().merge(address, 1, Integer::sum);
        if (misses >= resolveModuleAddressMissThreshold()) {
            state.getActiveModuleAddresses().remove(address);
            state.getModuleAddressMissCounts().remove(address);
            collectorDeviceStateService.persistModuleActive(state.getConfig().getName(), state.getConfig(), address, false);
            log.warn("蓄电池模块地址因连续未响应已从缓存移除, 通道={}, 地址={}, 未响应次数={}",
                    state.getConfig().getName(),
                    address,
                    misses);
        }
    }

    /** 判断是否为 246 组模块地址。 */
    private boolean isGroupModuleAddress(int address) {
        return address == GROUP_MODULE_ADDRESS;
    }

    /**
     * 重置模块地址缓存，下轮轮询恢复全量发现。
     *
     * @param channelName 通道名称；为空时重置全部通道
     * @return 是否匹配到通道
     */
    public boolean resetModuleAddressCache(String channelName) {
        return cacheService.resetModuleAddressCache(channelStates, realtimeSnapshotService, channelName);
    }

    /**
     * 按电池组重置模块地址缓存，下轮轮询恢复全量发现。
     *
     * @param batteryGroup 电池组编号；为空时重置全部通道
     * @return 是否匹配到通道
     */
    public boolean resetModuleAddressCacheByBatteryGroup(Integer batteryGroup) {
        return cacheService.resetModuleAddressCacheByBatteryGroup(channelStates, realtimeSnapshotService, batteryGroup);
    }

    /**
     * 清理设备状态去重缓存，确保删除状态表后相同状态值也能重新写入。
     *
     * @param batteryGroup 电池组编号；为空时清除全部
     * @return 清理的缓存条目数
     */
    public int clearDeviceStateDedupCacheByBatteryGroup(Integer batteryGroup) {
        return collectorDeviceStateService.clearDedupCacheByBatteryGroup(channelStates, batteryGroup);
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
