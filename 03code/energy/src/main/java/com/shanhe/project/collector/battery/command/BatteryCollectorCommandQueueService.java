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
 * <p>只负责命令队列的核心逻辑：待处理请求构造、响应匹配判断。
 * 不负责帧 I/O、业务回调或日志持久化。</p>
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
}
