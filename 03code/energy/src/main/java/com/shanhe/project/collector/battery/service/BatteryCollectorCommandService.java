package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 980聚合命令兼容服务。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Service
public class BatteryCollectorCommandService {

    /**
     * 980 聚合命令不允许直发 600 节下行总线时的提示。
     */
    private static final String AGGREGATE_COMMAND_UNSUPPORTED =
            "980 aggregate command cannot be sent directly to 600 module channel; implement an explicit module-control mapping first";

    /**
     * 已映射但尚未接入串口下发队列时的提示。
     */
    private static final String MODULE_COMMAND_MAPPED =
            "980 aggregate command mapped to 600 module command; serial dispatch is not enabled yet";

    /**
     * 已映射并加入600节模块端串口下发队列时的提示。
     */
    private static final String MODULE_COMMAND_QUEUED =
            "980 aggregate command mapped to 600 module command and queued for serial dispatch";

    /**
     * 600节模块端显式控制命令构造服务。
     */
    @Resource
    private BatteryModuleControlCommandService moduleControlCommandService = new BatteryModuleControlCommandService();

    /**
     * 独立采集服务，负责按通道线程串行下发显式模块端命令。
     */
    @Autowired(required = false)
    private BatteryCollectorService collectorService;

    /**
     * 执行 980 聚合兼容命令。
     *
     * @param commandDefinition 聚合命令定义
     * @param channelName 通道名称
     * @param timeoutMs 超时时间
     * @param payloadBytes 请求参数
     * @return 命令结果
     */
    public BatteryCollectorCommandResult execute(BatteryAggregateCommandDefinition commandDefinition,
                                                 String channelName,
                                                 Long timeoutMs,
                                                 int... payloadBytes) {
        BatteryModuleControlCommand moduleCommand = mapToModuleCommand(commandDefinition, payloadBytes);
        if (moduleCommand == null) {
            // 兼容入口只保留旧 980 语义，不允许绕过映射直写 600 节下行总线。
            return unsupported(commandDefinition, channelName);
        }
        return mapped(commandDefinition, channelName, moduleCommand, queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult setSystemState(String channelName, int batteryGroup, int systemState, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.SET_SYSTEM_STATE, channelName, timeoutMs, systemState, batteryGroup);
    }

    public BatteryCollectorCommandResult singleInternalResistanceTest(String channelName, int batteryGroup, int batteryNumber, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST, channelName, timeoutMs, batteryGroup, batteryNumber);
    }

    public BatteryCollectorCommandResult automaticSetSubmoduleAddress(String channelName, int batteryGroup, Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS, channelName);
    }

    public BatteryCollectorCommandResult connectResistanceTest(String channelName, int batteryGroup, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST, channelName, timeoutMs, batteryGroup);
    }

    public BatteryCollectorCommandResult settingInternalResistanceCoefficient(String channelName, int batteryGroup, double coefficient, Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT, channelName);
    }

    public BatteryCollectorCommandResult updateTimeAll(String channelName, int deviceType, int year, int month, int day, int hour, int minute, int second, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.UPDATE_TIME_ALL, channelName, timeoutMs,
                deviceType, year, month, day, hour, minute, second);
    }

    public BatteryCollectorCommandResult batteryEqualizationSet(String channelName, int manualEnable, int autoEnable, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.BATTERY_EQUALIZATION_SET, channelName, timeoutMs, manualEnable, autoEnable);
    }

    public BatteryCollectorCommandResult setDeviceIpAddress(String channelName,
                                                            int[] deviceIpBytes,
                                                            int[] maskBytes,
                                                            int[] gatewayBytes,
                                                            int port,
                                                            Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.SET_DEVICE_IP_ADDRESS, channelName);
    }

    public BatteryCollectorCommandResult setCloudServerIpAddress(String channelName, int[] cloudServerIpBytes, int port, Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.SET_CLOUD_SERVER_IP_ADDRESS, channelName);
    }

    public BatteryCollectorCommandResult setServerClientMode(String channelName, int mode, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.SET_SERVER_CLIENT_MODE, channelName, timeoutMs, mode);
    }

    public BatteryCollectorCommandResult clearIndividualDebuggingData(String channelName, int batteryGroup, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.CLEAR_INDIVIDUAL_DEBUGGING_DATA, channelName, timeoutMs, batteryGroup);
    }

    public BatteryCollectorCommandResult clearHostDebuggingData(String channelName, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.CLEAR_HOST_DEBUGGING_DATA, channelName, timeoutMs);
    }

    public BatteryCollectorCommandResult setDeviceId(String channelName, int deviceId, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.SET_DEVICE_ID, channelName, timeoutMs, deviceId);
    }

    public BatteryCollectorCommandResult setSubmoduleId(String channelName, int submoduleId, Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.SET_SUBMODULE_ID, channelName);
    }

    public BatteryCollectorCommandResult setSwollenVoltageReference(String channelName, int batteryGroup, double referenceValue, Long timeoutMs) {
        return unsupported(BatteryAggregateCommandDefinition.SET_SWOLLEN_VOLTAGE_REFERENCE, channelName);
    }

    private BatteryModuleControlCommand mapToModuleCommand(BatteryAggregateCommandDefinition commandDefinition,
                                                           int... payloadBytes) {
        if (commandDefinition == null) {
            return null;
        }
        try {
            switch (commandDefinition) {
                case SINGLE_INTERNAL_RESISTANCE_TEST:
                    return payloadBytes != null && payloadBytes.length >= 2
                            ? moduleControlCommandService.singleBatteryInternalResistanceTest(payloadBytes[1])
                            : null;
                case CONNECT_RESISTANCE_TEST:
                    return moduleControlCommandService.connectStripResistanceTest();
                case CLEAR_INDIVIDUAL_DEBUGGING_DATA:
                    return payloadBytes != null && payloadBytes.length >= 1
                            ? moduleControlCommandService.clearSingleDebugData(payloadBytes[0])
                            : null;
                case AUTOMATIC_SET_SUBMODULE_ADDRESS:
                    return payloadBytes != null && payloadBytes.length == 7
                            ? moduleControlCommandService.autoSetModuleAddress(0, payloadBytes)
                            : null;
                default:
                    return null;
            }
        } catch (IllegalArgumentException e) {
            log.warn("980 aggregate command mapping rejected, command={}, reason={}",
                    commandDefinition.name(),
                    e.getMessage());
            return null;
        }
    }

    private BatteryCollectorCommandResult mapped(BatteryAggregateCommandDefinition commandDefinition,
                                                  String channelName,
                                                  BatteryModuleControlCommand moduleCommand,
                                                  boolean queued) {
        log.info("mapped 980 aggregate command to 600 module command, channel={}, command={}, moduleCommand={}, queued={}",
                channelName,
                commandDefinition == null ? null : commandDefinition.name(),
                moduleCommand == null ? null : moduleCommand.getProtocolCode(),
                queued);
        return BatteryCollectorCommandResult.builder()
                .success(queued)
                .timeout(false)
                .mappedToModuleCommand(true)
                .channelName(channelName)
                .commandDefinition(commandDefinition)
                .moduleControlCommand(moduleCommand)
                .requestCode(moduleCommand == null ? null : moduleCommand.getRequestCode())
                .responseCode(moduleCommand == null ? null : moduleCommand.getResponseCode())
                .message(queued ? MODULE_COMMAND_QUEUED : MODULE_COMMAND_MAPPED)
                .build();
    }

    private boolean queueModuleCommand(String channelName, BatteryModuleControlCommand moduleCommand) {
        return collectorService != null && collectorService.submitModuleCommand(channelName, moduleCommand);
    }

    private BatteryCollectorCommandResult unsupported(BatteryAggregateCommandDefinition commandDefinition, String channelName) {
        log.warn("blocked 980 aggregate command on 600 module channel, channel={}, command={}",
                channelName,
                commandDefinition == null ? null : commandDefinition.name());
        return BatteryCollectorCommandResult.builder()
                .success(false)
                .timeout(false)
                .mappedToModuleCommand(false)
                .channelName(channelName)
                .commandDefinition(commandDefinition)
                .requestCode(commandDefinition == null ? null : commandDefinition.getRequestCode())
                .responseCode(commandDefinition == null ? null : commandDefinition.getResponseCode())
                .message(AGGREGATE_COMMAND_UNSUPPORTED)
                .build();
    }
}
