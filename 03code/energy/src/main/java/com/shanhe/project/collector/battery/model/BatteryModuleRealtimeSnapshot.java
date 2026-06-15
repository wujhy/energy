package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 600节模块端标准实时有效快照。
 *
 * @author wjh
 * @since 2026-06-15
 */
@Data
@Builder
public class BatteryModuleRealtimeSnapshot {

    /** 电池组编号。 */
    private Integer packNum;

    /** 配置中的组内单体个数。 */
    private Integer batSinSize;

    /** 采集批次号。 */
    private String pollBatchNo;

    /** 采集开始时间。 */
    private Date pollStartedAt;

    /** 组实时数据。 */
    private BatteryModuleGroupRealtime group;

    /** 对外有效单体列表。 */
    private List<BatteryModuleCellRealtime> cells;

    /** 按单体编号索引的有效单体。 */
    private Map<Integer, BatteryModuleCellRealtime> cellMap;

    /** 本轮采集到的单体编号集合。 */
    private Set<Integer> currentBatchCellNums;

    /** 连续两轮未采集到的单体编号集合。 */
    private Set<Integer> staleCellNums;

    /** 当前配置数量下未补齐的单体编号集合。 */
    private Set<Integer> missingCellNums;

    /** 单体连续未采集轮数。 */
    private Map<Integer, Integer> cellMissCounts;

    /** 快照刷新时间。 */
    private Date refreshedAt;

    public List<BatteryModuleCellRealtime> getCells() {
        return cells == null ? Collections.emptyList() : cells;
    }

    public Map<Integer, BatteryModuleCellRealtime> getCellMap() {
        return cellMap == null ? Collections.emptyMap() : cellMap;
    }

    public Set<Integer> getCurrentBatchCellNums() {
        return currentBatchCellNums == null ? Collections.emptySet() : currentBatchCellNums;
    }

    public Set<Integer> getStaleCellNums() {
        return staleCellNums == null ? Collections.emptySet() : staleCellNums;
    }

    public Set<Integer> getMissingCellNums() {
        return missingCellNums == null ? Collections.emptySet() : missingCellNums;
    }

    public Map<Integer, Integer> getCellMissCounts() {
        return cellMissCounts == null ? Collections.emptyMap() : cellMissCounts;
    }

    public boolean isDataReady() {
        return group != null || !getCells().isEmpty();
    }
}
