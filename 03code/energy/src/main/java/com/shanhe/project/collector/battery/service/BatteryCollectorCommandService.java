package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.model.BatteryCollectorCommandResult;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.opt.service.OptLogService;
import com.shanhe.project.iot.model.BatteryModeInfo;

import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.MAX_CELL_ADDRESS;
import static com.shanhe.project.collector.battery.protocol.BatteryModuleProtocolConstants.UNSIGNED_SHORT_MAX;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 980聚合命令兼容服务。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Slf4j
@Service
public class BatteryCollectorCommandService {

    /** 自动编号命令中的组模块地址。 */
    private static final int GROUP_MODULE_ADDRESS = 246;
    /** 自动编号命令的起始地址。 */
    private static final int START_SET_ADDRESS = 1;
    /** 清除所有调试参数的命令值。 */
    private static final int CLEAR_ALL_DEBUG_PARAMETER = 0x0F;

    /** 980 聚合命令不允许直发 600 节下行总线时的提示。 */
    private static final String AGGREGATE_COMMAND_UNSUPPORTED =
            "980聚合命令不能直接发送到600模块通道，请先实现显式模块控制映射";

    /** 已映射但未能加入串口下发队列时的提示。 */
    private static final String MODULE_COMMAND_MAPPED =
            "980聚合命令已映射为600模块命令，但未加入串口下发队列，请检查通道状态和队列容量";

    /** 已映射并加入600节模块端串口下发队列时的提示。 */
    private static final String MODULE_COMMAND_QUEUED =
            "980聚合命令已映射为600模块命令并加入串口下发队列";

    /** 已有测试/维护模式运行时的提示。 */
    private static final String MODE_BUSY_MESSAGE =
            "当前有其他测试运行中，无法执行均衡操作";

    /** 600节模块端显式控制命令构造服务。 */
    @Resource
    private BatteryModuleControlCommandService moduleControlCommandService = new BatteryModuleControlCommandService();
    /** 采集模块配置。 */
    @Resource
    private BatteryCollectorProperties properties;
    /** 电池模式状态服务。 */
    @Resource
    private BatteryModeStatusService batteryModeStatusService;
    /** 操作日志服务。 */
    @Resource
    private OptLogService optLogService;
    /** 电池组配置服务。 */
    @Autowired(required = false)
    private IBatteryPackService batteryPackService;

    /** 独立采集服务，负责按通道线程串行下发显式模块端命令。 */
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

    /**
     * 执行单体内阻测试命令。
     */
    public BatteryCollectorCommandResult singleInternalResistanceTest(String channelName, int batteryGroup, Integer batteryNumber, Long timeoutMs) {
        BatteryCollectorCommandResult runningResult = rejectRunningWorkMode(
                BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                channelName,
                batteryGroup,
                BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        if (runningResult != null) {
            return runningResult;
        }
        BatteryCollectorCommandResult addressResult = validateSingleInternalResistanceAddress(
                channelName,
                batteryGroup,
                batteryNumber);
        if (addressResult != null) {
            return addressResult;
        }
        return execute(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST, channelName, timeoutMs, batteryGroup, batteryNumber);
    }

    /**
     * 执行整组内阻测试：按 1..batteryCount 顺序下发 02/82 单体内阻命令。
     */
    public BatteryCollectorCommandResult groupInternalResistanceTest(String channelName, int batteryGroup, int batteryCount, Long timeoutMs) {
        if (isBlank(channelName)) {
            return BatteryCollectorCommandResult.builder()
                    .success(false)
                    .message("通道名称不能为空")
                    .build();
        }
        if (batteryGroup <= 0) {
            return BatteryCollectorCommandResult.builder()
                    .success(false)
                    .message("电池组编号无效")
                    .build();
        }
        BatteryCollectorCommandResult runningResult = rejectRunningWorkMode(
                BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                channelName,
                batteryGroup,
                BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        if (runningResult != null) {
            return runningResult;
        }
        try {
            validateBatteryCount(batteryCount);
        } catch (IllegalArgumentException e) {
            return BatteryCollectorCommandResult.builder().success(false).message(e.getMessage()).build();
        }
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.singleBatteryInternalResistanceTest(1);
        } catch (IllegalArgumentException e) {
            log.warn("整组内阻测试命令被拒绝, 通道={}, 电池组={}, 地址={}, 原因={}",
                    channelName, batteryGroup, 1, e.getMessage());
            return blocked(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                    channelName,
                    "整组内阻首节地址无效");
        }
        Long businessOptLogId = optLogService == null
                ? null
                : optLogService.insert(batteryGroup, BatteryTestEnum._1.getDictValue(), null,
                com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants.Source.COLLECTOR);
        moduleCommand.setDescription("整组内阻测试第1节");
        moduleCommand.setOptLogType(BatteryTestEnum._99.getDictValue());
        moduleCommand.setBusinessOptLogId(businessOptLogId);
        moduleCommand.setGroupInternalResistanceNextAddress(2);
        moduleCommand.setGroupInternalResistanceMaxAddress(batteryCount);
        moduleCommand.setGroupInternalResistanceFailed(false);
        applyContext(moduleCommand, batteryGroup, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE);
        boolean queued = queueModuleCommand(channelName, moduleCommand);
        if (!queued && businessOptLogId != null && optLogService != null) {
            optLogService.update(businessOptLogId, 1, null);
        }
        return BatteryCollectorCommandResult.builder()
                .success(queued)
                .timeout(false)
                .mappedToModuleCommand(true)
                .channelName(channelName)
                .commandDefinition(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST)
                .moduleControlCommand(moduleCommand)
                .requestCode(moduleCommand.getRequestCode())
                .responseCode(moduleCommand.getResponseCode())
                .message(queued ? "整组内阻首节命令已加入下发队列" : "整组内阻首节命令加入下发队列失败")
                .build();
    }
    private BatteryCollectorCommandResult validateSingleInternalResistanceAddress(String channelName,
                                                                                  int batteryGroup,
                                                                                  Integer batteryNumber) {
        if (batteryGroup <= 0) {
            return blocked(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                    channelName,
                    "电池组编号无效");
        }
        if (batteryNumber == null || batteryNumber < 1 || batteryNumber > MAX_CELL_ADDRESS) {
            return blocked(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                    channelName,
                    "单体编号必须在1到245之间");
        }
        Integer maxNumber = resolveBatteryMaxNumber(batteryGroup);
        if (maxNumber != null && batteryNumber > maxNumber) {
            return blocked(BatteryAggregateCommandDefinition.SINGLE_INTERNAL_RESISTANCE_TEST,
                    channelName,
                    "单体编号超过电池组实际单体数");
        }
        return null;
    }

    private Integer resolveBatteryMaxNumber(Integer batteryGroup) {
        if (batteryPackService == null || batteryGroup == null || batteryGroup <= 0) {
            return null;
        }
        try {
            Integer maxNumber = batteryPackService.getBatteryMaxNumber(batteryGroup);
            return maxNumber != null && maxNumber > 0 ? Math.min(maxNumber, MAX_CELL_ADDRESS) : null;
        } catch (RuntimeException e) {
            log.warn("查询电池组实际单体数失败, 电池组={}, 原因={}",
                    batteryGroup, e.getMessage());
            return null;
        }
    }
    /**
     * 停止指定电池组当前运行的采集测试，并取消队列中尚未下发的同类命令。
     *
     * @param batteryGroup 电池组编号
     * @param mode 期望停止的工作模式
     * @return 停止处理结果
     */
    public BatteryCollectorCommandResult stopRunningTest(Integer batteryGroup, Integer mode) {
        return stopRunningTest(batteryGroup, mode, resolveCollectorOptLogType(mode));
    }

    public BatteryCollectorCommandResult stopRunningTest(Integer batteryGroup, Integer mode, Integer optLogType) {
        if (batteryGroup == null || batteryGroup <= 0) {
            return stopRejected("电池组编号无效");
        }
        if (mode == null) {
            return stopRejected("测试类型不支持停止");
        }

        closeRunningOptLog(batteryGroup, optLogType);

        BatteryModeInfo modeInfo = batteryModeStatusService == null ? null : batteryModeStatusService.get(batteryGroup);
        if (modeInfo == null || !Objects.equals(modeInfo.getPackNum(), batteryGroup)
                || !Objects.equals(modeInfo.getStatus(), 1)) {
            return stopRejected("当前电池组没有正在执行的测试");
        }
        if (!Objects.equals(modeInfo.getMode(), mode)) {
            return stopRejected("当前运行测试类型与停止类型不一致");
        }

        int cancelled = collectorService == null ? 0 : collectorService.cancelQueuedModuleCommands(batteryGroup, mode);
        batteryModeStatusService.markStopped(batteryGroup, mode, modeInfo.getAddress(), true);
        return BatteryCollectorCommandResult.builder()
                .success(true)
                .timeout(false)
                .mappedToModuleCommand(true)
                .message("测试停止成功，已取消未下发命令" + cancelled + "条")
                .build();
    }
    private BatteryCollectorCommandResult stopRejected(String reason) {
        return blocked(null, null, "未停止，原因是" + reason);
    }
    /**
     * 手动设置模块地址。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param moduleAddress 当前模块地址
     * @param newModuleAddress 新模块地址
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
    public BatteryCollectorCommandResult manualSetSubmoduleAddress(String channelName,
                                                                   int batteryGroup,
                                                                   int moduleAddress,
                                                                   int newModuleAddress,
                                                                   Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.setModuleAddress(moduleAddress, newModuleAddress);
        } catch (IllegalArgumentException e) {
            log.warn("手动设置模块地址命令被拒绝, 通道={}, 电池组={}, 地址={}, 新地址={}, 原因={}",
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

    /**
     * 执行连接条电阻测试命令。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param batteryCount 单体数量（用于确定测试地址范围）
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
    public BatteryCollectorCommandResult connectResistanceTest(String channelName, int batteryGroup, int batteryCount, Long timeoutMs) {
        if (channelName == null || channelName.trim().isEmpty()) {
            return BatteryCollectorCommandResult.builder().success(false).message("通道名称不能为空").build();
        }
        if (batteryGroup <= 0) {
            return BatteryCollectorCommandResult.builder().success(false).message("电池组编号无效").build();
        }
        BatteryCollectorCommandResult runningResult = rejectRunningWorkMode(
                BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST,
                channelName,
                batteryGroup,
                BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
        if (runningResult != null) {
            return runningResult;
        }
        try {
            validateBatteryCount(batteryCount);
        } catch (IllegalArgumentException e) {
            log.warn("连接条测试命令被拒绝, 通道={}, 电池组={}, 单体数量={}, 原因={}",
                    channelName,
                    batteryGroup,
                    batteryCount,
                    e.getMessage());
            return BatteryCollectorCommandResult.builder()
                    .success(false)
                    .timeout(false)
                    .mappedToModuleCommand(false)
                    .channelName(channelName)
                    .commandDefinition(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST)
                    .message("电池组单体数量无效")
                    .build();
        }
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.connectStripResistanceTest();
            moduleCommand.setConnectResistanceNextAddress(1);
            moduleCommand.setConnectResistanceMaxAddress(batteryCount);
            moduleCommand.setOptLogType(BatteryTestEnum._2.getDictValue());
        } catch (IllegalArgumentException e) {
            log.warn("连接条测试命令被拒绝, 通道={}, 电池组={}, 原因={}", channelName, batteryGroup, e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.CONNECT_RESISTANCE_TEST,
                channelName,
                applyContext(moduleCommand, batteryGroup, BatteryModeStatusService.MODE_CONNECT_RESISTANCE),
                queueModuleCommand(channelName, moduleCommand));
    }

    /**
     * 清除电池组调试数据。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
    public BatteryCollectorCommandResult clearBatteryGroupDebugData(String channelName,
                                                                    int batteryGroup,
                                                                    Long timeoutMs) {
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.clearSingleDebugData(CLEAR_ALL_DEBUG_PARAMETER);
        } catch (IllegalArgumentException e) {
            log.warn("清除电池组调试数据命令被拒绝, 通道={}, 电池组={}, 原因={}",
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

    /**
     * 执行自动编号命令。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param batteryCount 单体数量
     * @param batterySpecification 电池规格（2V/12V）
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
    public BatteryCollectorCommandResult autoSetSubmoduleAddress(String channelName,
                                                                 int batteryGroup,
                                                                 int batteryCount,
                                                                 int batterySpecification,
        Long timeoutMs) {
        if (channelName == null || channelName.trim().isEmpty()) {
            return BatteryCollectorCommandResult.builder().success(false).message("通道名称不能为空").build();
        }
        if (batteryGroup <= 0) {
            return BatteryCollectorCommandResult.builder().success(false).message("电池组编号无效").build();
        }
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.autoSetModuleAddress(
                    GROUP_MODULE_ADDRESS,
                    automaticSetAddressStartPayload(batteryCount, batterySpecification));
            moduleCommand.setAutoAddressBatteryCount(batteryCount);
            moduleCommand.setAutoAddressBatterySpecification(batterySpecification);
        } catch (IllegalArgumentException e) {
            log.warn("自动编号命令被拒绝, 通道={}, 电池组={}, 单体数量={}, 规格={}, 原因={}",
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

    /**
     * 单体均衡控制。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param moduleAddress 单体地址
     * @param balanceValue 均衡值
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
    public BatteryCollectorCommandResult singleBatteryBalance(String channelName,
                                                               int batteryGroup,
                                                               int moduleAddress,
                                                               int balanceValue,
                                                               Long timeoutMs) {
        if (hasRunningWorkMode(batteryGroup)) {
            return blocked(BatteryAggregateCommandDefinition.BATTERY_EQUALIZATION_SET, channelName, MODE_BUSY_MESSAGE);
        }
        BatteryModuleControlCommand moduleCommand;
        try {
            moduleCommand = moduleControlCommandService.singleBatteryBalance(moduleAddress, balanceValue);
        } catch (IllegalArgumentException e) {
            log.warn("均衡命令被拒绝, 通道={}, 电池组={}, 地址={}, 原因={}", channelName, batteryGroup, moduleAddress, e.getMessage());
            return unsupported(BatteryAggregateCommandDefinition.BATTERY_EQUALIZATION_SET, channelName);
        }
        return mapped(BatteryAggregateCommandDefinition.BATTERY_EQUALIZATION_SET,
                channelName,
                applyContext(moduleCommand, batteryGroup, BatteryModeStatusService.MODE_BALANCE),
                queueModuleCommand(channelName, moduleCommand));
    }

    /**
     * 设置内阻系数。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param moduleAddress 模块地址
     * @param coefficient 内阻系数
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
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
            log.warn("设置内阻系数命令被拒绝, 通道={}, 电池组={}, 地址={}, 系数={}, 原因={}",
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

    /**
     * 设置校准维护参数。
     *
     * @param channelName 通道名称
     * @param batteryGroup 电池组编号
     * @param moduleAddress 模块地址
     * @param dataType 数据类型
     * @param dataStatus 数据状态
     * @param dataInfo 数据信息
     * @param timeoutMs 超时时间
     * @return 命令结果
     */
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
            log.warn("电池数据校正命令被拒绝, 通道={}, 电池组={}, 地址={}, 数据类型={}, 数据状态={}, 数据信息={}, 原因={}",
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
     * 按电池组解析独立采集通道名称。
     *
     * @param batteryGroup 电池组编号
     * @return 通道名称；无法唯一定位时返回null
     */
    public String resolveChannelName(Integer batteryGroup) {
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
            if (matchedByGroup != null) {
                return null;
            }
            matchedByGroup = channel.getName();
        }
        return matchedByGroup;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 将980聚合命令映射为600模块控制命令。 */
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
            log.warn("980聚合命令映射被拒绝, 命令={}, 原因={}",
                    commandDefinition.name(),
                    e.getMessage());
            return null;
        }
    }

    /** 构建映射成功的命令结果。 */
    private BatteryCollectorCommandResult mapped(BatteryAggregateCommandDefinition commandDefinition,
                                                  String channelName,
                                                  BatteryModuleControlCommand moduleCommand,
                                                  boolean queued) {
        log.info("980聚合命令已映射为600模块命令, 通道={}, 命令={}, 模块命令={}, 已入队={}",
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

    /** 当前已有工作模式运行时，禁止采集测试重复入队。 */
    private BatteryCollectorCommandResult rejectRunningWorkMode(BatteryAggregateCommandDefinition commandDefinition,
                                                                  String channelName,
                                                                  Integer batteryGroup,
                                                                  Integer expectedMode) {
        if (batteryGroup == null || batteryGroup <= 0 || batteryModeStatusService == null) {
            return null;
        }
        BatteryModeInfo modeInfo = batteryModeStatusService.get(batteryGroup);
        if (modeInfo == null
                || !Objects.equals(modeInfo.getPackNum(), batteryGroup)
                || !Objects.equals(modeInfo.getStatus(), 1)) {
            return null;
        }
        if (Objects.equals(modeInfo.getMode(), expectedMode)) {
            return blocked(commandDefinition, channelName, "当前电池组已有同类型测试运行中");
        }
        return blocked(commandDefinition, channelName, "当前电池组有其他测试运行中");
    }

    /** 当前已有工作模式运行时，禁止均衡命令插队。 */
    private boolean hasRunningWorkMode(Integer batteryGroup) {
        if (batteryModeStatusService == null) {
            return false;
        }
        BatteryModeInfo modeInfo = batteryModeStatusService.get(batteryGroup);
        return modeInfo != null
                && Objects.equals(modeInfo.getPackNum(), batteryGroup)
                && Objects.equals(modeInfo.getStatus(), 1);
    }

    /** 关闭600采集测试对应的运行日志。 */
    private void closeRunningOptLog(Integer batteryGroup, Integer optLogType) {
        if (optLogService == null || batteryGroup == null || optLogType == null) {
            return;
        }
        optLogService.doStopTest(batteryGroup, optLogType);
    }
    private Integer resolveCollectorOptLogType(Integer mode) {
        if (Objects.equals(mode, BatteryModeStatusService.MODE_CONNECT_RESISTANCE)) {
            return BatteryTestEnum._2.getDictValue();
        }
        if (Objects.equals(mode, BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)) {
            return BatteryTestEnum._6.getDictValue();
        }
        return null;
    }

    /** 为模块命令设置电池组和工作模式上下文。 */
    private BatteryModuleControlCommand applyContext(BatteryModuleControlCommand moduleCommand,
                                                     Integer batteryGroup,
                                                     Integer mode) {
        if (moduleCommand != null) {
            moduleCommand.setBatteryGroup(batteryGroup);
            moduleCommand.setMode(mode);
        }
        return moduleCommand;
    }

    /** 根据聚合命令类型设置模块命令的工作模式。 */
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
                moduleCommand.setOptLogType(BatteryTestEnum._6.getDictValue());
                break;
            case CONNECT_RESISTANCE_TEST:
                moduleCommand.setMode(BatteryModeStatusService.MODE_CONNECT_RESISTANCE);
                moduleCommand.setOptLogType(BatteryTestEnum._2.getDictValue());
                break;
            default:
                return;
        }
        if (payloadBytes != null && payloadBytes.length > 0) {
            moduleCommand.setBatteryGroup(payloadBytes[0]);
        }
    }

    /** 将内阻系数转换为M460 float字节数组（小端序）。 */
    private int[] resistanceCoefficientToM460FloatBytes(int coefficient) {
        // 16位无符号整数最大值，对应MCU端两字节寄存器
        if (coefficient < 0 || coefficient > UNSIGNED_SHORT_MAX) {
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

    /** 构造自动编号命令的起始载荷。 */
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
        if (batteryCount < 1 || batteryCount > MAX_CELL_ADDRESS) {
            throw new IllegalArgumentException("电池组单体数量必须在1到245之间");
        }
    }

    private void validateBatterySpecification(int batterySpecification) {
        batterySpecificationToVoltage(batterySpecification);
    }

    /**
     * 电池规格转换为电压
     * 电池规格: 2=2V单体, 8=12V(6节串联)
     */
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
        if (value < 0 || value > UNSIGNED_SHORT_MAX) {
            throw new IllegalArgumentException("数据信息必须在-65535到65535之间");
        }
        return value;
    }

    /** 构建不支持的命令结果。 */
    private BatteryCollectorCommandResult unsupported(BatteryAggregateCommandDefinition commandDefinition, String channelName) {
        log.warn("980聚合命令在600模块通道上被阻止, 通道={}, 命令={}",
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

    /** 构建被前置策略阻止的命令结果。 */
    private BatteryCollectorCommandResult blocked(BatteryAggregateCommandDefinition commandDefinition,
                                                  String channelName,
                                                  String message) {
        log.warn("600模块命令被前置策略阻止, 通道={}, 命令={}, 原因={}",
                channelName,
                commandDefinition == null ? null : commandDefinition.name(),
                message);
        return BatteryCollectorCommandResult.builder()
                .success(false)
                .timeout(false)
                .mappedToModuleCommand(false)
                .channelName(channelName)
                .commandDefinition(commandDefinition)
                .requestCode(commandDefinition == null ? null : commandDefinition.getRequestCode())
                .responseCode(commandDefinition == null ? null : commandDefinition.getResponseCode())
                .message(message)
                .build();
    }
}
