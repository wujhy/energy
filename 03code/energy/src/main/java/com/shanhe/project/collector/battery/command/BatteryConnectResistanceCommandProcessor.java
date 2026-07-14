package com.shanhe.project.collector.battery.command;

import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.collector.battery.service.BatteryCollectorCommandLogService;
import com.shanhe.project.collector.battery.service.BatteryConnectResistanceStatisticsRefreshService;
import com.shanhe.project.collector.battery.service.BatteryModuleCellCompatibilityFillService;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 连接条电阻测试命令处理器。
 *
 * <p>负责 0F 启动命令后的 11/91 读电压排队、91 响应解析、电阻计算、缓存和实时表写入。</p>
 *
 * @author wjh
 * @since 2026-06-18
 */
@Slf4j
@Component
public class BatteryConnectResistanceCommandProcessor {

    public enum QueueNextVoltageReadResult {
        QUEUED,
        COMPLETED,
        REJECTED
    }

    @Resource
    private BatteryModuleCellCompatibilityFillService compatibilityFillService;
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;
    @Resource
    private BatteryModuleRealtimeSnapshotService snapshotService;
    @Resource
    private BatteryCollectorCommandLogService commandLogService;
    @Resource
    private BatteryCollectorCommandQueueService commandQueueService;
    @Resource
    private BatteryConnectResistanceStatisticsRefreshService statisticsRefreshService;

    /**
     * 根据连接条测试上下文排队下一条 11/91 读电压命令。
     *
     * @param state 通道状态
     * @param pendingRequest 当前待响应请求
     * @return QUEUED 表示已排队，COMPLETED 表示所有地址已完成，REJECTED 表示队列拒绝
     */
    public QueueNextVoltageReadResult queueNextVoltageRead(BatteryCollectorChannelState state, BatteryPendingRequest pendingRequest) {
        Integer nextAddress = pendingRequest.getConnectResistanceNextAddress();
        Integer maxAddress = pendingRequest.getConnectResistanceMaxAddress();
        if (nextAddress == null || maxAddress == null || nextAddress > maxAddress) {
            return QueueNextVoltageReadResult.COMPLETED;
        }
        int address = nextAddress;
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
        if (!state.getQueuedModuleCommands().offer(command)) {
            return QueueNextVoltageReadResult.REJECTED;
        }
        pendingRequest.setConnectResistanceNextAddress(address + 1);
        return QueueNextVoltageReadResult.QUEUED;
    }

    public void closeConnectResistanceAsRejected(BatteryPendingRequest pendingRequest) {
        if (pendingRequest == null) {
            return;
        }
        commandLogService.updateCommandOptLog(
                pendingRequest.getOptLogId(),
                BatteryDeviceStateConstants.CommandStatus.REJECTED,
                null,
                null);
        commandQueueService.markModeStopped(pendingRequest, false);
    }

    /**
     * 处理连接条 91 电压响应，包含中间步骤排队和最终状态写入。
     *
     * @param state 通道状态
     * @param frame 当前响应帧
     * @param pendingRequest 当前待响应请求
     * @param success 当前响应是否成功
     */
    public void handleVoltageResponse(BatteryCollectorChannelState state,
                                      BatteryCollectorFrame frame,
                                      BatteryPendingRequest pendingRequest,
                                      boolean success) {
        if (!success) {
            pendingRequest.setConnectResistanceFailed(true);
        }
        if (success) {
            storeConnectResistanceResult(pendingRequest, frame);
        }
        QueueNextVoltageReadResult queueResult = queueNextVoltageRead(state, pendingRequest);
        if (queueResult == QueueNextVoltageReadResult.REJECTED) {
            closeConnectResistanceAsRejected(pendingRequest);
            return;
        }
        if (queueResult == QueueNextVoltageReadResult.COMPLETED) {
            boolean finalSuccess = success && !pendingRequest.isConnectResistanceFailed();
            String finalStatus = finalSuccess
                    ? BatteryDeviceStateConstants.CommandStatus.SUCCESS
                    : BatteryDeviceStateConstants.CommandStatus.FAILED;
            commandLogService.updateCommandOptLog(
                    pendingRequest.getOptLogId(),
                    finalStatus,
                    frame.getCommand(),
                    bytesToHex(frame.getPayloadSafe()));
            commandQueueService.markModeStopped(pendingRequest, finalSuccess);
            if (finalSuccess && statisticsRefreshService != null) {
                statisticsRefreshService.refreshAfterCompletedTest(pendingRequest.getBatteryGroup());
            }
        }
    }

    /** 解析 91 响应帧中的连接条测试电压并计算电阻。 */
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

            Double current = currentOfGroup(batteryGroup);
            Double connectBatteryVoltage = batteryVoltageRaw / 10000.0d;
            Double connectTestVoltage = testVoltageRaw / 10000.0d;
            Double resistance = calculateConnectResistance(connectTestVoltage, connectBatteryVoltage, current);

            if (resistance != null) {
                compatibilityFillService.putConnectResistance(batteryGroup, address, resistance);
                upsertCellResistance(batteryGroup, address, resistance);
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

    /** 获取电池组实时电流，优先使用标准实时快照中的 chargeDischargeCurrent。 */
    private Double currentOfGroup(Integer batteryGroup) {
        BatteryModuleRealtimeSnapshot snapshot = snapshotOf(batteryGroup);
        BatteryModuleGroupRealtime group = snapshot == null ? null : snapshot.getGroup();
        if (group == null) {
            return null;
        }
        return group.getChargeDischargeCurrent();
    }

    /** 写入单体实时表中的连接条电阻值。 */
    private void upsertCellResistance(Integer batteryGroup, int address, Double resistance) {
        if (realtimeMapper == null) {
            return;
        }
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setPackNum(batteryGroup);
        cell.setBatNum(address);
        cell.setResistanceRageSlip(resistance);
        cell.setCreateTime(new Date());
        realtimeMapper.upsertCell(cell);
    }

    /** 读取标准实时快照；只读缓存，不回源实时表。 */
    private BatteryModuleRealtimeSnapshot snapshotOf(Integer batteryGroup) {
        return snapshotService == null ? null : snapshotService.getCachedSnapshot(batteryGroup);
    }

    /** 计算真实连接条电阻，单位 uΩ；无法计算时返回 null。 */
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
        resistance = Math.round(resistance * 10000.0d) / 10000.0d;
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

    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            sb.append(String.format("%02X", value & 0xFF));
        }
        return sb.toString();
    }
}
