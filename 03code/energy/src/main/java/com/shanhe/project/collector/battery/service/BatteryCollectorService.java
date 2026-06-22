package com.shanhe.project.collector.battery.service;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.common.utils.Threads;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.*;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorMetrics;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelSnapshot;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.command.BatteryConnectResistanceCommandProcessor;
import com.shanhe.project.collector.battery.runtime.BatteryCollectorFrameIoService;
import com.shanhe.project.collector.battery.runtime.BatteryCollectorTimeoutService;
import com.shanhe.project.collector.battery.state.BatteryCollectorDeviceStateService;
import com.shanhe.project.device.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
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
    @Resource
    private BatteryConnectResistanceCommandProcessor connectResistanceCommandProcessor;
    @Resource
    private BatteryCollectorTimeoutService timeoutService;

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
                    commandQueueService.markModeStopped(command, false);
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

    /** 取出一条排队的控制命令并下发。已委托 commandQueueService 做队列和判断。 */
    private boolean processQueuedModuleCommand(BatteryCollectorChannelState state) {
        return commandQueueService.processNextQueuedCommand(
                state,
                (request, pendingRequest, waitingState) -> writeFrame(state, request, pendingRequest, waitingState),
                (request, command) -> writeFrameWithoutPending(state, request, command));
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
        commandQueueService.completeNoResponseCommandSent(state, command);
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
            connectResistanceCommandProcessor.queueNextVoltageRead(state, pendingFrom0F);
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
        // 连接条测试 91 响应不在中间步骤更新日志状态，只在最终完成时更新
        boolean connectResistanceVoltageResponse =
                BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.name().equals(pendingRequest.getName());
        boolean success = commandQueueService.completeExplicitCommandResponse(
                state,
                frame,
                pendingRequest,
                !connectResistanceVoltageResponse,
                bytesToHex(frame.getPayloadSafe()));
        if (BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.name().equals(pendingRequest.getName())) {
            if (!success) {
                log.warn("自动编号失败, 通道={}, 地址={}, 响应={}",
                        state.getConfig() == null ? null : state.getConfig().getName(),
                        String.format("%02X", pendingRequest.getRequestAddress()),
                        String.format("%02X", frame.getCommand()));
                commandQueueService.markModeStopped(pendingRequest, false);
                return;
            }
            if (commandQueueService.queueNextAutoSetAddressStep(
                    state,
                    frame,
                    pendingRequest,
                    this::markModeRunning,
                    () -> cacheService.resetModuleAddressCache(state, realtimeSnapshotService))) {
                return;
            }
            commandQueueService.markModeStopped(pendingRequest, true);
            cacheService.resetModuleAddressCache(state, realtimeSnapshotService);
            log.info("自动编号成功后蓄电池模块地址缓存已重置, 通道={}",
                    state.getConfig() == null ? null : state.getConfig().getName());
            return;
        }
        if (BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE.name().equals(pendingRequest.getName())) {
            connectResistanceCommandProcessor.handleVoltageResponse(state, frame, pendingRequest, success);
            return;
        }
        commandQueueService.markModeStopped(pendingRequest, success);
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

    /** 已委托 commandQueueService。 */
    private boolean shouldResetModuleAddressCacheAfterCommand(BatteryPendingRequest pendingRequest) {
        return commandQueueService.shouldResetModuleAddressCacheAfterCommand(pendingRequest);
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
        timeoutService.checkTimeout(state,
                (request, pendingRequest, waitingState) -> writeFrame(state, request, pendingRequest, waitingState));
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

    /** 判断地址是否为单体模块地址(1-245)。 */
    private boolean isCellModuleAddress(Integer address) {
        return address != null && address >= 1 && address <= 245;
    }

    /**
     * 重置模块地址缓存，下轮轮询恢复全量发现。
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
