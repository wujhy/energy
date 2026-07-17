package com.shanhe.project.collector.battery.config;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 蓄电池独立采集模块配置。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "battery-collector.collector")
public class BatteryCollectorProperties {

    private Boolean enabled = Boolean.FALSE;

    /** 第一阶段默认关闭主动轮询，避免与旧链路冲突。 */
    private Boolean autoPollEnabled = Boolean.FALSE;

    private Long loopDelayMs = 300L;

    private Long requestGapMs = 120L;

    /** 是否开启协议级调试日志。 */
    private Boolean debugEnabled = Boolean.FALSE;

    /** 是否保存 600 节模块端原始帧日志；联调或追溯时打开。 */
    private Boolean rawFrameLogEnabled = Boolean.FALSE;

    /** 是否写入 600 节模块端标准实时数据表。 */
    private Boolean realtimeDataEnabled = Boolean.FALSE;

    /** 是否在实时数据入库后计算电池组指标。 */
    private Boolean groupCalculationEnabled = Boolean.FALSE;

    /** 是否把独立模块采集结果同步为旧 dev_battery_report_log 历史记录。 */
    private Boolean compatReportLogEnabled = Boolean.FALSE;


    /**
     * JSON/TCP/页面/计划控制是否优先尝试独立模块命令服务。
     * 开启后 _2/_6 适配失败直接返回错误，不回退旧 980 链路；关闭时保留旧兼容链路。
     */
    private Boolean jsonTcpModuleCommandEnabled = Boolean.FALSE;

    /** 是否从源头保留 600 模块命令 _99 明细日志；业务日志不受影响。 */
    private Boolean moduleCommandSuccessLogEnabled = Boolean.FALSE;

    /** _5 备电运行日志补偿前等待实时状态离开 BACKUP 的确认窗口。 */
    private Long backupRuntimeRecoveryConfirmMs = 12L * 60L * 60L * 1000L;

    /** _5 采集后处理关闭备电日志前，实时状态需持续离开 BACKUP 的防抖确认窗口。 */
    private Long backupEndConfirmMs = 180_000L;

    /** 测试启动后采集状态的滞后宽限期；宽限期内不做状态自然结束补偿和自动停止评估。 */
    private Long testStartupStatusGraceMs = 120_000L;

    /** 是否启用 _5 备电自动停止评估（截止电压/备电时长，对应 M460 0x30 下层停止语义）。 */
    private Boolean backupAutoStopEnabled = Boolean.TRUE;

    /** _1/_2 内阻/连接条测试无进展补偿窗口；现场确认典型耗时后调整。 */
    private Long resistanceRuntimeRecoveryConfirmMs = 6L * 60L * 60L * 1000L;

    /** _6 单节内阻测试无进展补偿窗口；现场确认典型耗时后调整。 */
    private Long singleResistanceRuntimeRecoveryConfirmMs = 30L * 60L * 1000L;

    /** 组计算时单体实时数据的新鲜度阈值。 */
    private Long groupCalculationStaleThresholdMs = 180_000L;

    /** 全量发现后是否只轮询有响应的模块地址。 */
    private Boolean moduleAddressCacheEnabled = Boolean.TRUE;

    /** 有响应地址连续无响应多少次后从缓存移除。 */
    private Integer moduleAddressMissThreshold = 3;

    /** 周期性全量发现间隔；0 表示只在启动、手动重置或缓存为空时全量发现。 */
    private Long moduleAddressFullDiscoveryIntervalMs = 0L;

    /** 指定需要输出协议日志的通道名称；为空时表示全部通道。 */
    private List<String> debugChannels = new ArrayList<>();

    /** 原始帧日志保留天数；0 或负数表示不清理。 */
    private Integer rawFrameLogRetentionDays = 7;

    /** 原始帧日志查询最大返回条数。 */
    private Integer rawFrameLogQueryLimit = 500;

    /** 指定本轮实际运行的通道名称；为空时按 enabled 规则运行。 */
    private List<String> activeChannels = new ArrayList<>();

    private List<BatteryCollectorChannelConfig> channels = new ArrayList<>();
}
