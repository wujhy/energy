package com.shanhe.project.collector.battery.model;

import lombok.Data;
import com.shanhe.project.manage.opt.domain.OptLog;
import com.shanhe.project.iot.model.BatteryModeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified current battery state for pages and external protocols.
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
public class BatteryCurrentState {

    public static final String FRESHNESS_NO_CONFIG = "NO_CONFIG";
    public static final String FRESHNESS_NOT_COLLECTED = "NOT_COLLECTED";
    public static final String FRESHNESS_FRESH = "FRESH";
    public static final String FRESHNESS_STALE = "STALE";
    public static final String FRESHNESS_PARTIAL = "PARTIAL";

    private Integer packNum;
    private Long packId;
    private Integer expectedCellCount;
    private String freshness;
    private String lastPollBatchNo;
    private BatteryCurrentGroupState group;
    private List<BatteryCurrentCellState> cells = new ArrayList<>();
    private List<BatteryDeviceState> deviceStates = new ArrayList<>();
    private List<BatteryCurrentAlarmSummary> alarms = new ArrayList<>();
    private List<String> unsupportedAlarmReasons = new ArrayList<>();
    private BatteryModeInfo modeInfo;
    private List<OptLog> runningOptLogs = new ArrayList<>();
    /** 最近一条命令日志的 error_message 透传（来自 dev_opt_log.error_message）。 */
    private String lastCommandErrorMessage;
    /** 最近一条运行中命令日志的 status（pending/failed 等），无运行命令时为 null。 */
    private String lastCommandStatus;
}
