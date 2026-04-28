package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Per-thread polling context used to associate realtime rows with one 600-module scan round.
 */
@Data
@Builder
public class BatteryModulePollContext {

    private String pollBatchNo;

    private Date pollStartedAt;

    @Builder.Default
    private List<BatteryModuleCellRealtime> cells = new ArrayList<>();

    @Builder.Default
    private List<BatteryModuleGroupRealtime> groups = new ArrayList<>();
}
