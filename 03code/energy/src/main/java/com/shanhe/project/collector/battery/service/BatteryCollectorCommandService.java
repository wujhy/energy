package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
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

    private static final int GROUP_MODULE_ADDRESS = 246;
    private static final int START_SET_ADDRESS = 1;
    private static final int CLEAR_ALL_DEBUG_PARAMETER = 0x0F;

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
    @Resource
    private BatteryCollectorProperties properties;

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
        applyModeContext(moduleCommand, commandDefinition, payloadBytes);
        return mapped(commandDefinition, channelName, moduleCommand, queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult singleInternalResistanceTest(String channelName, int batteryGroup, int batteryNumber, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST, channelName, timeoutMs, batteryGroup, batteryNumber);
    }

    public BatteryCollectorCommandResult manualSetSubmoduleAddress(String channelName,
                                                                   int batteryGroup,
                                                                   int moduleAddress,
                                                                   int newModuleAddress,
                                                                   Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.setModuleAddress(moduleAddress, newModuleAddress);
        } catch (IllegalArgumentException e) {
            log.warn("manual module address command rejected, channel={}, group={}, address={}, newAddress={}, reason={}",
                    channelName,
                    batteryGroup,
                    moduleAddress,
                    newModuleAddress,
                    e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.SET_SUBMODULE_ID, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.SET_SUBMODULE_ID,
                channelName,
                applyContext(moduleCommand, batteryGroup, null),
                queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult connectResistanceTest(String channelName, int batteryGroup, Long timeoutMs) {
        return execute(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST, channelName, timeoutMs, batteryGroup);
    }

    public BatteryCollectorCommandResult clearBatteryGroupDebugData(String channelName,
                                                                    int batteryGroup,
                                                                    Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.clearSingleDebugData(CLEAR_ALL_DEBUG_PARAMETER);
        } catch (IllegalArgumentException e) {
            log.warn("clear battery group debug data command rejected, channel={}, group={}, reason={}",
                    channelName,
                    batteryGroup,
                    e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.CLEAR_INDIVIDUAL_DEBUGGING_DATA, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.CLEAR_INDIVIDUAL_DEBUGGING_DATA,
                channelName,
                applyContext(moduleCommand, batteryGroup, null),
                queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult autoSetSubmoduleAddress(String channelName,
                                                                 int batteryGroup,
                                                                 int batteryCount,
                                                                 int batterySpecification,
                                                                 Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.autoSetModuleAddress(
                    GROUP_MODULE_ADDRESS,
                    automaticSetAddressStartPayload(batteryCount, batterySpecification));
            moduleCommand.setAutoAddressBatteryCount(batteryCount);
            moduleCommand.setAutoAddressBatterySpecification(batterySpecification);
        } catch (IllegalArgumentException e) {
            log.warn("automatic module address command rejected, channel={}, group={}, batteryCount={}, specification={}, reason={}",
                    channelName,
                    batteryGroup,
                    batteryCount,
                    batterySpecification,
                    e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.AUTOMATIC_SET_SUBMODULE_ADDRESS,
                channelName,
                applyContext(moduleCommand, batteryGroup, BatteryModeStatusService.MODE_AUTO_MODEL_NUM),
                queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult setInternalResistanceCoefficient(String channelName,
                                                                          int batteryGroup,
                                                                          int moduleAddress,
                                                                          int coefficient,
                                                                          Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.setInternalResistanceCoefficient(
                    moduleAddress,
                    resistanceCoefficientToM460FloatBytes(coefficient));
        } catch (IllegalArgumentException e) {
            log.warn("internal resistance coefficient command rejected, channel={}, group={}, address={}, coefficient={}, reason={}",
                    channelName,
                    batteryGroup,
                    moduleAddress,
                    coefficient,
                    e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.SETTING_INTERNAL_RESISTANCE_COEFFICIENT,
                channelName,
                applyContext(moduleCommand, batteryGroup, null),
                queueModuleCommand(channelName, moduleCommand));
    }

    public BatteryCollectorCommandResult setCalibrationParameter(String channelName,
                                                                 int batteryGroup,
                                                                 int moduleAddress,
                                                                 int dataType,
                                                                 int dataStatus,
                                                                 int dataInfo,
                                                                 Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.setCalibrationParameter(
                    moduleAddress,
                    dataType,
                    dataStatus,
                    unsignedShortHigh(dataInfo),
                    unsignedShortLow(dataInfo));
        } catch (IllegalArgumentException e) {
            log.warn("battery calibration command rejected, channel={}, group={}, address={}, dataType={}, dataStatus={}, dataInfo={}, reason={}",
                    channelName,
                    batteryGroup,
                    moduleAddress,
                    dataType,
                    dataStatus,
                    dataInfo,
                    e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.BATTERY_DATA_CORRECTION, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.BATTERY_DATA_CORRECTION,
                channelName,
                applyContext(moduleCommand, batteryGroup, null),
                queueModuleCommand(channelName, moduleCommand));
    }

    /**
     * 按旧业务设备和电池组解析独立采集通道名称。
     *
     * @param configId 旧业务设备ID
     * @param batteryGroup 电池组编号
     * @return 通道名称；无法唯一定位时返回null
     */
    public String resolveChannelName(Long configId, Integer batteryGroup) {
        if (properties == null || properties.getChannels() == null || batteryGroup == null) {
            return null;
        }
        String matchedByGroup = null;
        for (BatteryCollectorChannelConfig channel : properties.getChannels()) {
            if (channel == null
                    || isBlank(channel.getName())
                    || !Boolean.TRUE.equals(channel.getEnabled())
                    || !batteryGroup.equals(channel.getBatteryGroup())) {
                continue;
            }
            if (configId != null && configId.equals(channel.getConfigId())) {
                return channel.getName();
            }
            if (configId == null) {
                if (matchedByGroup != null) {
                    return null;
                }
                matchedByGroup = channel.getName();
            }
        }
        return configId == null ? matchedByGroup : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private BatteryModuleControlCommand applyContext(BatteryModuleControlCommand moduleCommand,
                                                     Integer batteryGroup,
                                                     Integer mode) {
        if (moduleCommand != null) {
            moduleCommand.setBatteryGroup(batteryGroup);
            moduleCommand.setMode(mode);
        }
        return moduleCommand;
    }

    private void applyModeContext(BatteryModuleControlCommand moduleCommand,
                                  BatteryAggregateCommandDefinition commandDefinition,
                                  int... payloadBytes) {
        if (moduleCommand == null || commandDefinition == null) {
            return;
        }
        switch (commandDefinition) {
            case AUTOMATIC_SET_SUBMODULE_ADDRESS:
                moduleCommand.setMode(BatteryModeStatusService.MODE_AUTO_MODEL_NUM);
                break;
            case SINGLE_INTERNAL_RESISTANCE_TEST:
                moduleCommand.setMode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
                break;
            case CONNECT_RESISTANCE_TEST:
                moduleCommand.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
                break;
            default:
                return;
        }
        if (payloadBytes != null && payloadBytes.length > 0) {
            moduleCommand.setBatteryGroup(payloadBytes[0]);
        }
    }

    private int[] resistanceCoefficientToM460FloatBytes(int coefficient) {
        if (coefficient < 0 || coefficient > 65535) {
            throw new IllegalArgumentException("内阻系数必须在0到65535之间");
        }
        // 旧 M460 将 980 侧两字节整数除以 1000 后，按 MCU 小端 float 内存字节下发给 600 模块。
        int bits = Float.floatToIntBits(coefficient / 1000.0f);
        return new int[]{
                bits & 0xFF,
                (bits >> 8) & 0xFF,
                (bits >> 16) & 0xFF,
                (bits >> 24) & 0xFF
        };
    }

    private int[] automaticSetAddressStartPayload(int batteryCount, int batterySpecification) {
        validateBatteryCount(batteryCount);
        validateBatterySpecification(batterySpecification);
        return new int[]{
                0,
                0,
                0,
                0,
                0,
                0,
                START_SET_ADDRESS
        };
    }

    private void validateBatteryCount(int batteryCount) {
        if (batteryCount < 1 || batteryCount > 245) {
            throw new IllegalArgumentException("电池组单体数量必须在1到245之间");
        }
    }

    private void validateBatterySpecification(int batterySpecification) {
        batterySpecificationToVoltage(batterySpecification);
    }

    private int batterySpecificationToVoltage(int batterySpecification) {
        switch (batterySpecification) {
            case 2:
                return 2;
            case 8:
                return 12;
            default:
                throw new IllegalArgumentException("自动编号仅支持2V或12V电池规格");
        }
    }

    private int unsignedShortHigh(int value) {
        return (toUnsignedShort(value) >> 8) & 0xFF;
    }

    private int unsignedShortLow(int value) {
        return toUnsignedShort(value) & 0xFF;
    }

    private int toUnsignedShort(int value) {
        if (value < 0) {
            value += 65536;
        }
        if (value < 0 || value > 65535) {
            throw new IllegalArgumentException("dataInfo must be between -65535 and 65535");
        }
        return value;
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
