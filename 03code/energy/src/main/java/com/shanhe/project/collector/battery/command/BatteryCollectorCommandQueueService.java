package com.shanhe.project.collector.battery.command;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandLogService;
import com.shanhe.project.collector.battery.service.BatteryModeStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.function.Consumer;

import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.BATTERY_SPEC_2V;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.BATTERY_SPEC_12V;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.GROUP_MODULE_ADDRESS;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.VOLTAGE_2V;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.VOLTAGE_12V;

/**
 * 蓄电池命令队列执行服务。
 *
 * <p>负责命令队列的核心逻辑：待处理请求构造、响应匹配判断、命令完成和超时处理。
 * 不负责帧 I/O（由 BatteryCollectorFrameIoService 处理）。</p>
 *
 * @author wjh
 * @since 2026-06-18
 */
@Slf4j
@Component
public class BatteryCollectorCommandQueueService {

    private static final int START_SET_ADDRESS = 1;
    private static final int STOP_SET_ADDRESS = 2;

    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    @Resource
    private BatteryCollectorCommandLogService commandLogService;
    @Resource
    private BatteryCollectorFrameCodec frameCodec;

    /**
     * 有响应命令发送回调。
     */
    @FunctionalInterface
    public interface PendingFrameWriter {
        /**
         * 写入命令帧并登记 pending 请求。
         *
         * @param frame 请求帧
         * @param pendingRequest 待响应请求
         * @param waitingState 写入成功后的等待状态
         * @return 是否写入成功
         */
        boolean write(BatteryCollectorFrame frame,
                      BatteryPendingRequest pendingRequest,
                      BatteryCollectorRunState waitingState);
    }

    /**
     * 无响应命令发送回调。
     */
    @FunctionalInterface
    public interface NoResponseFrameWriter {
        /**
         * 写入不等待响应的命令帧。
         *
         * @param frame 请求帧
         * @param command 控制命令
         * @return 是否写入成功
         */
        boolean write(BatteryCollectorFrame frame, BatteryModuleControlCommand command);
    }

    /**
     * 将控制命令转换为等待响应的待处理请求。
     *
     * @param command 控制命令
     * @return 待处理请求
     */
    public BatteryPendingRequest pendingFromCommand(BatteryModuleControlCommand command) {
        BatteryPendingRequest pendingRequest = BatteryPendingRequest.fromProtocolCode(
                command.getProtocolCode(),
                command.getAddress(),
                command.getPayload() == null ? new byte[0] : command.getPayload(),
                false);
        pendingRequest.setConfigId(command.getConfigId());
        pendingRequest.setBatteryGroup(command.getBatteryGroup());
        pendingRequest.setMode(command.getMode());
        pendingRequest.setAutoAddressBatteryCount(command.getAutoAddressBatteryCount());
        pendingRequest.setAutoAddressBatterySpecification(command.getAutoAddressBatterySpecification());
        pendingRequest.setOptLogId(command.getOptLogId());
        pendingRequest.setConnectResistanceNextAddress(command.getConnectResistanceNextAddress());
        pendingRequest.setConnectResistanceMaxAddress(command.getConnectResistanceMaxAddress());
        pendingRequest.setConnectResistanceFailed(command.isConnectResistanceFailed());
        return pendingRequest;
    }

    /**
     * 判断响应帧是否为当前等待的 pending 响应。
     *
     * @param state 通道状态
     * @param frame 响应帧
     * @return 是否匹配
     */
    public boolean isCurrentPendingResponse(BatteryCollectorChannelState state, BatteryCollectorFrame frame) {
        BatteryPendingRequest pending = state.getPendingCommand();
        if (pending == null || frame == null) {
            return false;
        }
        if (pending.isAutoPoll()) {
            return frame.getCommand() == state.getExpectedResponseCode();
        }
        return frame.getCommand() == state.getExpectedResponseCode()
                && frame.getCommand() != 0;
    }

    /**
     * 判断响应帧是否表示操作成功。
     *
     * @param frame 响应帧
     * @param pendingRequest 待处理请求
     * @return 是否成功
     */
    public boolean isSuccessResponse(BatteryCollectorFrame frame, BatteryPendingRequest pendingRequest) {
        BatteryDeviceProtocolCode protocolCode = BatteryDeviceProtocolCode.find(pendingRequest.getResponseCode());
        if (protocolCode == null) {
            return true;
        }
        if (protocolCode == BatteryDeviceProtocolCode.GET_CONNECT_STRIP_RESISTANCE_VOLTAGE) {
            return frame.getPayloadSafe().length >= 8;
        }
        if (!protocolCode.isStatusResponse() && protocolCode != BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS) {
            return true;
        }
        byte[] payload = frame.getPayloadSafe();
        if (protocolCode == BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS) {
            if (payload.length < 3) {
                return false;
            }
            return pendingRequest.getRequestAddress() != GROUP_MODULE_ADDRESS || (payload[0] & 0xFF) == 1;
        }
        // payload[0]==0 表示模块应答成功
        return payload.length > 0 && (payload[0] & 0xFF) == 0;
    }

    /**
     * 从队列中取出一条待执行的命令。
     *
     * @param state 通道状态
     * @return 取出的命令，队列为空返回 null
     */
    public BatteryModuleControlCommand dequeueCommand(BatteryCollectorChannelState state) {
        if (state.getPendingCommand() != null) {
            return null;
        }
        return state.getQueuedModuleCommands().poll();
    }

    /**
     * 从队列取出下一条命令并协调发送。
     *
     * <p>本方法只决定命令队列执行路径：出队、构造请求帧、判断是否等待响应、发送失败回队。
     * 串口实际写入由调用方通过回调完成，避免 command 包依赖串口运行细节。</p>
     *
     * @param state 通道状态
     * @param pendingFrameWriter 有响应命令写入回调
     * @param noResponseFrameWriter 无响应命令写入回调
     * @return 是否成功发出一条队列命令
     */
    public boolean processNextQueuedCommand(BatteryCollectorChannelState state,
                                            PendingFrameWriter pendingFrameWriter,
                                            NoResponseFrameWriter noResponseFrameWriter) {
        BatteryModuleControlCommand command = dequeueCommand(state);
        if (command == null) {
            return false;
        }
        BatteryCollectorFrame request = frameCodec.buildRequest(
                command.getAddress(),
                command.getRequestCode(),
                command.getPayload() == null ? new byte[0] : command.getPayload());
        if (isNoResponseCommand(command)) {
            if (!noResponseFrameWriter.write(request, command)) {
                requeueCommand(state, command);
                return false;
            }
            return true;
        }
        if (!pendingFrameWriter.write(request, pendingFromCommand(command), BatteryCollectorRunState.WAIT_COMMAND_RESPONSE)) {
            requeueCommand(state, command);
            return false;
        }
        return true;
    }

    /**
     * 将命令重新入队（发送失败时使用）。
     *
     * @param state 通道状态
     * @param command 待重新入队的命令
     */
    public void requeueCommand(BatteryCollectorChannelState state, BatteryModuleControlCommand command) {
        if (state != null && command != null) {
            state.getQueuedModuleCommands().offer(command);
        }
    }

    /**
     * 判断命令是否为无响应命令（发送后不等待响应）。
     *
     * @param command 控制命令
     * @return 是否无响应
     */
    public boolean isNoResponseCommand(BatteryModuleControlCommand command) {
        return command != null && command.getResponseCode() == null;
    }

    /**
     * 判断命令是否需要在无响应命令发送成功后停止工作模式。
     *
     * @param command 控制命令
     * @return 是否应停止模式
     */
    public boolean shouldStopModeAfterNoResponseCommand(BatteryModuleControlCommand command) {
        if (command == null || command.getMode() == null) {
            return false;
        }
        // 连接条测试模式不在无响应命令后停止
        if (command.getMode() == 10) { // MODE_CONNECT_RESISTANCE
            return false;
        }
        return true;
    }

    /**
     * 判断命令成功后是否需要重置模块地址缓存。
     *
     * @param pendingRequest 待处理请求
     * @return 是否需要重置
     */
    public boolean shouldResetModuleAddressCacheAfterCommand(BatteryPendingRequest pendingRequest) {
        String name = pendingRequest.getName();
        return BatteryDeviceProtocolCode.SET_MODULE_ADDRESS.name().equals(name);
    }

    /**
     * 记录最近完成的显式模块命令状态，供运行状态快照和外部查询使用。
     *
     * @param state 通道状态
     * @param commandName 命令名称
     * @param responseCode 响应码
     * @param success 命令是否成功
     */
    public void markCompletedCommand(BatteryCollectorChannelState state,
                                     String commandName,
                                     int responseCode,
                                     boolean success) {
        if (state == null) {
            return;
        }
        state.setLastCompletedModuleCommandName(commandName);
        state.setLastCompletedModuleResponseCode(responseCode);
        state.setLastCompletedModuleCommandSuccess(success);
        state.setLastCompletedModuleCommandTime(System.currentTimeMillis());
    }

    /**
     * 完成不等待响应命令的发送成功收尾。
     *
     * @param state 通道状态
     * @param command 已发送成功的无响应命令
     */
    public void completeNoResponseCommandSent(BatteryCollectorChannelState state,
                                              BatteryModuleControlCommand command) {
        if (state == null || command == null) {
            return;
        }
        state.setLastSendTime(System.currentTimeMillis());
        state.setLastRequestCode(command.getRequestCode());
        state.setExpectedResponseCode(0);
        state.setLastPendingTimedOut(false);
        state.setRunState(BatteryCollectorRunState.READ);
        markCompletedCommand(state, command.getProtocolCode().name(), 0, true);
        if (shouldStopModeAfterNoResponseCommand(command)) {
            markModeStopped(command, true);
        }
    }

    /**
     * 完成显式命令的超时收尾：记录快照、停止工作模式并更新操作日志。
     *
     * @param state 通道状态
     * @param pendingRequest 超时的待响应请求
     */
    public void completeTimedOutExplicitCommand(BatteryCollectorChannelState state,
                                                BatteryPendingRequest pendingRequest) {
        if (state == null || pendingRequest == null || pendingRequest.isAutoPoll()) {
            return;
        }
        markCompletedCommand(state, pendingRequest.getName(), pendingRequest.getResponseCode(), false);
        markModeStopped(pendingRequest, false);
        commandLogService.updateCommandOptLog(
                pendingRequest.getOptLogId(),
                BatteryDeviceStateConstants.CommandStatus.TIMEOUT,
                null,
                null);
    }

    /**
     * 完成显式命令响应收尾，并按需更新普通命令日志。
     *
     * @param state 通道状态
     * @param frame 响应帧
     * @param pendingRequest 待响应请求
     * @param updateCommandLog 是否更新命令操作日志
     * @param responsePayload 响应载荷十六进制字符串
     * @return 响应是否表示成功
     */
    public boolean completeExplicitCommandResponse(BatteryCollectorChannelState state,
                                                   BatteryCollectorFrame frame,
                                                   BatteryPendingRequest pendingRequest,
                                                   boolean updateCommandLog,
                                                   String responsePayload) {
        boolean success = isSuccessResponse(frame, pendingRequest);
        markCompletedCommand(state, pendingRequest.getName(), frame.getCommand(), success);
        if (updateCommandLog) {
            commandLogService.updateCommandOptLog(
                    pendingRequest.getOptLogId(),
                    success ? BatteryDeviceStateConstants.CommandStatus.SUCCESS
                            : BatteryDeviceStateConstants.CommandStatus.FAILED,
                    frame.getCommand(),
                    responsePayload);
        }
        return success;
    }

    /**
     * 自动编号响应成功后排队下一步命令。
     *
     * <p>本方法只处理 18/A8 自动编号协议推进和命令入队；工作模式运行标记、地址缓存重置由调用方回调完成。</p>
     *
     * @param state 通道状态
     * @param frame 当前响应帧
     * @param pendingRequest 当前待响应请求
     * @param modeRunningCallback 新增等待响应命令后的模式运行标记回调
     * @param addressCacheResetCallback 自动编号停止命令已排队后的地址缓存重置回调
     * @return true 表示已排队后续命令，false 表示没有后续步骤
     */
    public boolean queueNextAutoSetAddressStep(BatteryCollectorChannelState state,
                                               BatteryCollectorFrame frame,
                                               BatteryPendingRequest pendingRequest,
                                               Consumer<BatteryModuleControlCommand> modeRunningCallback,
                                               Runnable addressCacheResetCallback) {
        Integer batteryCount = pendingRequest.getAutoAddressBatteryCount();
        Integer batterySpecification = pendingRequest.getAutoAddressBatterySpecification();
        if (batteryCount == null || batterySpecification == null) {
            return false;
        }
        int currentAddress = pendingRequest.getRequestAddress();
        if (currentAddress == GROUP_MODULE_ADDRESS) {
            return offerAutoSetAddressStep(
                    state, pendingRequest, 1, firstAutoSetAddressCellPayload(batterySpecification), modeRunningCallback);
        }
        if (currentAddress < batteryCount) {
            return offerAutoSetAddressStep(
                    state, pendingRequest, currentAddress + 1, nextAutoSetAddressPayload(frame.getPayloadSafe()), modeRunningCallback);
        }
        byte[] stopPayload = stopAutoSetAddressPayload(frame.getPayloadSafe());
        state.getQueuedModuleCommands().offer(autoSetAddressCommand(pendingRequest, currentAddress, stopPayload, false));
        BatteryModuleControlCommand stopGroupCommand = autoSetAddressCommand(pendingRequest, GROUP_MODULE_ADDRESS, stopPayload, false);
        stopGroupCommand.setMode(pendingRequest.getMode());
        state.getQueuedModuleCommands().offer(stopGroupCommand);
        if (addressCacheResetCallback != null) {
            addressCacheResetCallback.run();
        }
        log.info("自动编号成功后蓄电池模块地址缓存已重置, 通道={}",
                state.getConfig() == null ? null : state.getConfig().getName());
        return true;
    }

    /**
     * 处理自动编号响应后的模式状态、后续步骤排队和地址缓存重置。
     *
     * @param state 通道状态
     * @param frame 响应帧
     * @param pendingRequest 当前自动编号请求
     * @param success 当前响应是否成功
     * @param modeRunningCallback 后续等待响应命令的模式运行中回调
     * @param addressCacheResetCallback 地址缓存重置回调
     * @return true 表示当前响应属于自动编号且已处理
     */
    public boolean handleAutoSetAddressResponse(BatteryCollectorChannelState state,
                                                BatteryCollectorFrame frame,
                                                BatteryPendingRequest pendingRequest,
                                                boolean success,
                                                Consumer<BatteryModuleControlCommand> modeRunningCallback,
                                                Runnable addressCacheResetCallback) {
        if (pendingRequest == null
                || !BatteryDeviceProtocolCode.AUTO_SET_MODULE_ADDRESS.name().equals(pendingRequest.getName())) {
            return false;
        }
        if (!success) {
            markModeStopped(pendingRequest, false);
            return true;
        }
        if (queueNextAutoSetAddressStep(
                state,
                frame,
                pendingRequest,
                modeRunningCallback,
                addressCacheResetCallback)) {
            return true;
        }
        markModeStopped(pendingRequest, true);
        if (addressCacheResetCallback != null) {
            addressCacheResetCallback.run();
        }
        log.info("自动编号成功后蓄电池模块地址缓存已重置, 通道={}",
                state == null || state.getConfig() == null ? null : state.getConfig().getName());
        return true;
    }

    /** 构造并排队自动编号的下一步命令。 */
    private boolean offerAutoSetAddressStep(BatteryCollectorChannelState state,
                                            BatteryPendingRequest pendingRequest,
                                            int address,
                                            byte[] payload,
                                            Consumer<BatteryModuleControlCommand> modeRunningCallback) {
        BatteryModuleControlCommand command = autoSetAddressCommand(pendingRequest, address, payload, true);
        if (modeRunningCallback != null) {
            modeRunningCallback.accept(command);
        }
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

    /**
     * 标记控制命令关联的工作模式已停止。
     *
     * @param command 控制命令
     * @param success 命令是否成功完成
     */
    public void markModeStopped(BatteryModuleControlCommand command, boolean success) {
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

    /**
     * 标记待响应请求关联的工作模式已停止。
     *
     * @param pendingRequest 待响应请求
     * @param success 命令是否成功完成
     */
    public void markModeStopped(BatteryPendingRequest pendingRequest, boolean success) {
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

    /** 获取模式关联地址，自动编号组命令使用实际电池数量作为旧接口展示地址。 */
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
}
