package com.shanhe.project.collector.battery.command;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 蓄电池命令队列执行服务。
 *
 * <p>负责命令队列的核心逻辑：待处理请求构造、响应匹配判断、命令完成和超时处理。
 * 不负责帧 I/O（由 BatteryCollectorFrameIoService 处理）和日志持久化（由 BatteryCollectorCommandLogService 处理）。</p>
 */
@Slf4j
@Component
public class BatteryCollectorCommandQueueService {

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
            // GROUP_MODULE_ADDRESS = 246
            return pendingRequest.getRequestAddress() != 246 || (payload[0] & 0xFF) == 1;
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
}
