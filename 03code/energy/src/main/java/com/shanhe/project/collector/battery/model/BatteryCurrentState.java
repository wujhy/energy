package com.shanhe.project.collector.battery.model;

import lombok.Data;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.collector.battery.model.BatteryModeInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 统一的电池当前状态，供页面展示和外部协议使用
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentState {

    /** 数据新鲜度状态：未配置电池组。 */
    public static final String FRESHNESS_NO_CONFIG = "NO_CONFIG";
    /** 数据新鲜度状态：尚未采集到数据。 */
    public static final String FRESHNESS_NOT_COLLECTED = "NOT_COLLECTED";
    /** 数据新鲜度状态：数据新鲜。 */
    public static final String FRESHNESS_FRESH = "FRESH";
    /** 数据新鲜度状态：数据过期。 */
    public static final String FRESHNESS_STALE = "STALE";
    /** 数据新鲜度状态：部分单体缺失。 */
    public static final String FRESHNESS_PARTIAL = "PARTIAL";

    /** 电池组编号。 */
    private Integer packNum;
    /** 电池组配置主键。 */
    private Long packId;
    /** 配置中的期望单体数量。 */
    private Integer expectedCellCount;
    /** 数据新鲜度（FRESH/STALE/PARTIAL/NOT_COLLECTED/NO_CONFIG）。 */
    private String freshness;
    /** 最近一次轮询批次号。 */
    private String lastPollBatchNo;
    /** 快照刷新时间。 */
    private Date snapshotRefreshedAt;
    /** 快照是否包含组或单体数据。 */
    private Boolean snapshotDataReady;
    /** 快照是否仍在默认新鲜度窗口内。 */
    private Boolean snapshotFresh;
    /** 本轮采集到的单体编号。 */
    private List<Integer> currentBatchCellNums = new ArrayList<>();
    /** 连续缺采进入 stale 的单体编号。 */
    private List<Integer> staleCellNums = new ArrayList<>();
    /** 当前快照未补齐的单体编号。 */
    private List<Integer> missingCellNums = new ArrayList<>();
    /** 单体连续未采集轮数。 */
    private Map<Integer, Integer> cellMissCounts;
    /** 组级实时状态。 */
    private BatteryCurrentGroupState group;
    /** 单体状态列表。 */
    private List<BatteryCurrentCellState> cells = new ArrayList<>();
    /** 设备状态列表。 */
    private List<BatteryDeviceState> deviceStates = new ArrayList<>();
    /** 当前告警摘要列表。 */
    private List<BatteryCurrentAlarmSummary> alarms = new ArrayList<>();
    /** M460 不支持的告警类型及原因说明。 */
    private List<String> unsupportedAlarmReasons = new ArrayList<>();
    /** 当前模块工作模式信息。 */
    private BatteryModeInfo modeInfo;
    /** 当前运行中的操作日志列表。 */
    private List<OptLog> runningOptLogs = new ArrayList<>();
    /** 最近一条命令日志的 error_message 透传（来自 dev_opt_log.error_message）。 */
    private String lastCommandErrorMessage;
    /** 最近一条运行中命令日志的 status（pending/failed 等），无运行命令时为 null。 */
    private String lastCommandStatus;
}
