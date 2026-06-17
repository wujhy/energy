package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 蓄电池采集协议日志服务，集中处理协议收发和轮询摘要日志。
 *
 * @author wjh
 * @since 2026-06-16
 */
@Slf4j
@Service
public class BatteryCollectorProtocolLogService {

    /**
     * 按调试配置输出协议收发日志。
     *
     * @param properties 采集配置
     * @param state 当前通道运行态
     * @param stage 协议阶段
     * @param message 日志正文
     */
    public void logProtocol(BatteryCollectorProperties properties, BatteryCollectorChannelState state,
                            String stage, String message) {
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

    /**
     * 输出本轮轮询汇总日志。
     *
     * @param state 当前通道运行态
     * @param fullDiscovery 本轮是否全量发现
     * @param polledCommands 本轮已轮询命令
     * @param completedCommands 本轮已完成命令
     */
    public void logPollSummary(BatteryCollectorChannelState state, boolean fullDiscovery,
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
}
