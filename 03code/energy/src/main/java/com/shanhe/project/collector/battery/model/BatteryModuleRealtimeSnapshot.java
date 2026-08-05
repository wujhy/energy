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

    /** 标准实时快照缓存 TTL，需与 ehcache/battery-realtime-snapshot 保持一致。 */
    public static final long DEFAULT_FRESH_MILLIS = 300_000L;

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

    /** 获取有效单体列表，null 安全。 */
    public List<BatteryModuleCellRealtime> getCells() {
        return cells == null ? Collections.emptyList() : cells;
    }

    /** 获取单体编号索引映射，null 安全。 */
    public Map<Integer, BatteryModuleCellRealtime> getCellMap() {
        return cellMap == null ? Collections.emptyMap() : cellMap;
    }

    /** 获取本轮采集到的单体编号集合，null 安全。 */
    public Set<Integer> getCurrentBatchCellNums() {
        return currentBatchCellNums == null ? Collections.emptySet() : currentBatchCellNums;
    }

    /** 获取连续两轮未采集到的单体编号集合，null 安全。 */
    public Set<Integer> getStaleCellNums() {
        return staleCellNums == null ? Collections.emptySet() : staleCellNums;
    }

    /** 获取当前配置数量下未补齐的单体编号集合，null 安全。 */
    public Set<Integer> getMissingCellNums() {
        return missingCellNums == null ? Collections.emptySet() : missingCellNums;
    }

    /** 获取单体连续未采集轮数映射，null 安全。 */
    public Map<Integer, Integer> getCellMissCounts() {
        return cellMissCounts == null ? Collections.emptyMap() : cellMissCounts;
    }

    /** 判断快照是否包含有效数据（组数据或单体数据至少一项存在）。 */
    public boolean isDataReady() {
        return group != null || !getCells().isEmpty();
    }

    /** 判断快照是否仍在统一新鲜度窗口内。 */
    public boolean isFresh(Date now, long freshMillis) {
        if (refreshedAt == null || freshMillis <= 0) {
            return false;
        }
        Date checkTime = now == null ? new Date() : now;
        long ageMillis = checkTime.getTime() - refreshedAt.getTime();
        return ageMillis >= 0 && ageMillis <= freshMillis;
    }

    /** 判断快照是否仍在默认实时新鲜度窗口内。 */
    public boolean isFresh() {
        return isFresh(new Date(), DEFAULT_FRESH_MILLIS);
    }
}
