package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 600节模块端轮询批次上下文。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
@Builder
public class BatteryModulePollContext {

    /**
     * 轮询批次号。
     */
    private String pollBatchNo;

    /**
     * 轮询开始时间。
     */
    private Date pollStartedAt;

    /**
     * 当前批次内缓存的单体实时数据。
     */
    @Builder.Default
    private List<BatteryModuleCellRealtime> cells = new ArrayList<>();

    /**
     * 当前批次内缓存的组实时数据。
     */
    @Builder.Default
    private List<BatteryModuleGroupRealtime> groups = new ArrayList<>();

    /**
     * 当前批次的告警候选上下文，仅用于后续标准告警服务衔接。
     */
    private BatteryModuleAlarmContext alarmContext;
}
