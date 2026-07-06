package com.shanhe.project.collector.battery.service;

import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.manage.opt.mapper.OptLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 600节模块端命令操作日志服务，负责创建和更新命令执行记录。
 *
 * @author wjh
 * @since 2026-06-16
 */
@Slf4j
@Service
public class BatteryCollectorCommandLogService {

    /** 操作日志 Mapper。 */
    @Resource
    private OptLogMapper optLogMapper;
    @Resource
    private BatteryCollectorProperties properties;

    /**
     * 创建600模块命令操作日志。
     *
     * @param config 采集通道配置
     * @param command 模块端控制命令
     * @return 操作日志ID；创建失败时返回null
     */
    public Long createCommandOptLog(BatteryCollectorChannelConfig config, BatteryModuleControlCommand command) {
        try {
            if (!shouldCreateCommandOptLog(command)) {
                return null;
            }
            String now = now();
            OptLog optLog = new OptLog();
            optLog.setId(IdUtils.getSnowflakeId());
            optLog.setConfigId(config == null ? null : config.getConfigId());
            optLog.setPackNum(command.getBatteryGroup());
            optLog.setType(command.getOptLogType() == null ? BatteryTestEnum._99.getDictValue() : command.getOptLogType());
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

    /**
     * 更新600模块命令操作日志状态。
     *
     * @param optLogId 操作日志ID
     * @param status 命令状态
     * @param responseCode 响应码
     * @param responsePayload 响应载荷hex
     */
    public void updateCommandOptLog(Long optLogId, String status, Integer responseCode, String responsePayload) {
        updateCommandOptLog(optLogId, status, responseCode, responsePayload, null);
    }

    /**
     * 更新600模块命令操作日志状态，可指定自定义错误原因。
     *
     * @param optLogId 操作日志ID
     * @param status 命令状态
     * @param responseCode 响应码
     * @param responsePayload 响应载荷hex
     * @param errorMessage 自定义错误原因，为 null 时根据 status 自动生成
     */
    public void updateCommandOptLog(Long optLogId, String status, Integer responseCode, String responsePayload,
                                    String errorMessage) {
        if (optLogId == null) {
            return;
        }
        try {
            String now = now();
            String resolvedMessage = errorMessage != null ? errorMessage : errorMessageOf(status, responseCode, responsePayload);
            optLogMapper.updateCommandStatus(optLogId, status, resultOf(status), responseCode, now, resolvedMessage, responsePayload);
        } catch (Exception e) {
            log.warn("更新600模块命令日志失败, 日志ID={}, 原因={}", optLogId, e.getMessage());
        }
    }

    private boolean shouldCreateCommandOptLog(BatteryModuleControlCommand command) {
        if (command == null) {
            return false;
        }
        Integer type = command.getOptLogType() == null ? BatteryTestEnum._99.getDictValue() : command.getOptLogType();
        if (!BatteryTestEnum._99.getDictValue().equals(type)) {
            return true;
        }
        return properties == null || Boolean.TRUE.equals(properties.getModuleCommandSuccessLogEnabled());
    }

    private String errorMessageOf(String status, Integer responseCode, String responsePayload) {
        if (BatteryDeviceStateConstants.CommandStatus.SUCCESS.equals(status)
                || BatteryDeviceStateConstants.CommandStatus.PENDING.equals(status)) {
            return null;
        }
        String message;
        if (BatteryDeviceStateConstants.CommandStatus.TIMEOUT.equals(status)) {
            message = BatteryDeviceStateConstants.CommandErrorReason.TIMEOUT;
        } else if (BatteryDeviceStateConstants.CommandStatus.REJECTED.equals(status)) {
            message = BatteryDeviceStateConstants.CommandErrorReason.REJECTED;
        } else if (BatteryDeviceStateConstants.CommandStatus.CANCELLED.equals(status)) {
            message = BatteryDeviceStateConstants.CommandErrorReason.CANCELLED;
        } else if (BatteryDeviceStateConstants.CommandStatus.FAILED.equals(status)) {
            message = BatteryDeviceStateConstants.CommandErrorReason.FAILED;
        } else {
            message = BatteryDeviceStateConstants.CommandErrorReason.GENERIC_FAILED;
        }
        return BatteryDeviceStateConstants.CommandErrorReason.compose(
                message, responseCode, responsePayload);
    }

    private Integer resultOf(String status) {
        if (BatteryDeviceStateConstants.CommandStatus.SUCCESS.equals(status)) {
            return 0;
        }
        if (BatteryDeviceStateConstants.CommandStatus.PENDING.equals(status)) {
            return null;
        }
        return 1;
    }

    /** 生成旧操作日志表使用的时间字符串。 */
    private String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /** 将字节数组转为十六进制字符串（null 安全）。 */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02X", value));
        }
        return builder.toString();
    }
}
