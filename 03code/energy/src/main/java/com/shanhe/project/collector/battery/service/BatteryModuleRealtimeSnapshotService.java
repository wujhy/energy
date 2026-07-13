package com.shanhe.project.collector.battery.service;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 标准实时有效快照服务。
 *
 * @author wjh
 * @since 2026-06-15
 */
@Slf4j
@Service
public class BatteryModuleRealtimeSnapshotService {

    /** 单体连续缺失判定为过期的阈值次数，达到后不再用历史值填充。 */
    private static final int STALE_MISS_THRESHOLD = 2;

    /** 模块实时数据 Mapper。 */
    @Resource
    private BatteryModuleRealtimeMapper realtimeMapper;

    /** 电池组配置服务。 */
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private BatteryCollectorProperties properties;

    /**
     * 采集批次入库后刷新有效快照。
     *
     * @param packNum 电池组编号
     * @param context 本轮采集上下文
     * @param calculatedGroup 组计算结果
     * @return 有效快照
     */
    public BatteryModuleRealtimeSnapshot refreshAfterPoll(Integer packNum,
                                                          BatteryModulePollContext context,
                                                          BatteryModuleGroupRealtime calculatedGroup) {
        if (packNum == null || context == null) {
            return null;
        }
        BatteryModuleRealtimeSnapshot previous = getSnapshot(packNum);
        List<BatteryModuleCellRealtime> persistedCells = safeList(realtimeMapper.selectCells(packNum));
        BatteryModuleGroupRealtime group = calculatedGroup == null ? realtimeMapper.selectGroup(packNum) : calculatedGroup;
        BatteryModuleRealtimeSnapshot snapshot = buildSnapshot(packNum, context, persistedCells, group, previous);
        putSnapshot(snapshot);
        return snapshot;
    }

    /**
     * 获取快照；缓存未命中时用实时表做一次保守重建。
     *
     * @param packNum 电池组编号
     * @return 有效快照
     */
    public BatteryModuleRealtimeSnapshot getSnapshot(Integer packNum) {
        if (packNum == null) {
            return null;
        }
        Object value = CacheUtils.get(CacheKeyEnum.BATTERY_REPORT.getCache(), snapshotKey(packNum));
        if (value instanceof BatteryModuleRealtimeSnapshot) {
            return (BatteryModuleRealtimeSnapshot) value;
        }
        return rebuildFromRealtime(packNum);
    }

    public BatteryModuleRealtimeSnapshot getCachedSnapshot(Integer packNum) {
        if (packNum == null) {
            return null;
        }
        Object value = CacheUtils.get(CacheKeyEnum.BATTERY_REPORT.getCache(), snapshotKey(packNum));
        return value instanceof BatteryModuleRealtimeSnapshot ? (BatteryModuleRealtimeSnapshot) value : null;
    }

    public BatteryModuleRealtimeSnapshot getFreshCachedSnapshot(Integer packNum) {
        BatteryModuleRealtimeSnapshot snapshot = getCachedSnapshot(packNum);
        return isFresh(snapshot) ? snapshot : null;
    }

    public boolean isFresh(BatteryModuleRealtimeSnapshot snapshot) {
        Date latest = latestSnapshotTime(snapshot);
        if (latest == null) {
            return false;
        }
        long thresholdMs = resolveFreshThresholdMs();
        return System.currentTimeMillis() - latest.getTime() <= thresholdMs;
    }

    /**
     * 删除快照缓存。
     *
     * @param packNum 电池组编号
     */
    public void evict(Integer packNum) {
        if (packNum == null) {
            return;
        }
        CacheUtils.remove(CacheKeyEnum.BATTERY_REPORT.getCache(), snapshotKey(packNum));
    }

    /** 删除全部标准实时快照缓存。 */
    public void evictAll() {
        for (String key : CacheUtils.getCacheKeys(CacheKeyEnum.BATTERY_REPORT.getCache())) {
            if (key != null && key.startsWith("battery:module:snapshot:")) {
                CacheUtils.remove(CacheKeyEnum.BATTERY_REPORT.getCache(), key);
            }
        }
    }

    BatteryModuleRealtimeSnapshot buildSnapshot(Integer packNum,
                                                BatteryModulePollContext context,
                                                List<BatteryModuleCellRealtime> persistedCells,
                                                BatteryModuleGroupRealtime group,
                                                BatteryModuleRealtimeSnapshot previous) {
        Integer batSinSize = resolveBatSinSize(packNum);
        boolean needFallback = (batSinSize == null || batSinSize <= 0) && previous != null;
        if (needFallback) {
            batSinSize = previous.getBatSinSize();
        }
        Map<Integer, BatteryModuleCellRealtime> currentMap = toCellMap(context.getCells());
        Map<Integer, BatteryModuleCellRealtime> persistedMap = toCellMap(persistedCells);
        Map<Integer, BatteryModuleCellRealtime> previousMap = previous == null
                ? Collections.emptyMap() : previous.getCellMap();
        Map<Integer, Integer> previousMissCounts = previous == null
                ? Collections.emptyMap() : previous.getCellMissCounts();

        Set<Integer> currentNums = new LinkedHashSet<>(currentMap.keySet());
        Map<Integer, Integer> missCounts = new LinkedHashMap<>();
        Set<Integer> staleNums = new LinkedHashSet<>();
        List<BatteryModuleCellRealtime> merged = new ArrayList<>();
        Set<Integer> selectedNums = new LinkedHashSet<>();

        List<BatteryModuleCellRealtime> currentCells = sorted(currentMap.values());
        int limit = batSinSize == null || batSinSize <= 0 ? Integer.MAX_VALUE : batSinSize;
        for (BatteryModuleCellRealtime cell : currentCells) {
            if (cell == null || cell.getBatNum() == null || selectedNums.size() >= limit) {
                continue;
            }
            merged.add(cell);
            selectedNums.add(cell.getBatNum());
            missCounts.put(cell.getBatNum(), 0);
        }

        if (selectedNums.size() < limit) {
            List<BatteryModuleCellRealtime> candidates = sorted(previousMap.values());
            for (BatteryModuleCellRealtime cell : candidates) {
                if (cell == null || cell.getBatNum() == null || selectedNums.size() >= limit) {
                    continue;
                }
                Integer batNum = cell.getBatNum();
                if (currentNums.contains(batNum)) {
                    continue;
                }
                int missCount = previousMissCounts.getOrDefault(batNum, 0) + 1;
                missCounts.put(batNum, missCount);
                if (missCount >= STALE_MISS_THRESHOLD) {
                    staleNums.add(batNum);
                    continue;
                }
                BatteryModuleCellRealtime latest = persistedMap.getOrDefault(batNum, cell);
                merged.add(latest);
                selectedNums.add(batNum);
            }
        }

        merged = sorted(merged);
        Map<Integer, BatteryModuleCellRealtime> cellMap = toCellMap(merged);
        Set<Integer> missingNums = resolveMissingNums(batSinSize, merged.size(), staleNums);
        return BatteryModuleRealtimeSnapshot.builder()
                .packNum(packNum)
                .batSinSize(batSinSize)
                .pollBatchNo(context.getPollBatchNo())
                .pollStartedAt(context.getPollStartedAt())
                .group(group)
                .cells(merged)
                .cellMap(cellMap)
                .currentBatchCellNums(currentNums)
                .staleCellNums(staleNums)
                .missingCellNums(missingNums)
                .cellMissCounts(missCounts)
                .refreshedAt(new Date())
                .build();
    }

    private BatteryModuleRealtimeSnapshot rebuildFromRealtime(Integer packNum) {
        try {
            List<BatteryModuleCellRealtime> cells = safeList(realtimeMapper.selectCells(packNum));
            BatteryModuleGroupRealtime group = realtimeMapper.selectGroup(packNum);
            Integer batSinSize = resolveBatSinSize(packNum);
            List<BatteryModuleCellRealtime> selected = limit(sorted(cells), batSinSize);
            Map<Integer, BatteryModuleCellRealtime> cellMap = toCellMap(selected);
            Map<Integer, Integer> missCounts = new LinkedHashMap<>();
            for (BatteryModuleCellRealtime cell : selected) {
                if (cell != null && cell.getBatNum() != null) {
                    missCounts.put(cell.getBatNum(), 1);
                }
            }
            BatteryModuleRealtimeSnapshot snapshot = BatteryModuleRealtimeSnapshot.builder()
                    .packNum(packNum)
                    .batSinSize(batSinSize)
                    .group(group)
                    .cells(selected)
                    .cellMap(cellMap)
                    .currentBatchCellNums(Collections.emptySet())
                    .staleCellNums(Collections.emptySet())
                    .missingCellNums(resolveMissingNums(batSinSize, selected.size(), Collections.emptySet()))
                    .cellMissCounts(missCounts)
                    .refreshedAt(new Date())
                    .build();
            putSnapshot(snapshot);
            return snapshot;
        } catch (Exception e) {
            log.warn("重建蓄电池实时快照失败, packNum={}", packNum, e);
            return null;
        }
    }

    private void putSnapshot(BatteryModuleRealtimeSnapshot snapshot) {
        if (snapshot == null || snapshot.getPackNum() == null) {
            return;
        }
        CacheUtils.put(CacheKeyEnum.BATTERY_REPORT.getCache(), snapshotKey(snapshot.getPackNum()), snapshot);
    }

    private Date latestSnapshotTime(BatteryModuleRealtimeSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        Date latest = snapshot.getPollStartedAt();
        BatteryModuleGroupRealtime group = snapshot.getGroup();
        if (group != null) {
            latest = max(latest, group.getLatestGroupUpdateTime());
            latest = max(latest, group.getLatestCellUpdateTime());
            latest = max(latest, group.getPollStartedAt());
            latest = max(latest, group.getCreateTime());
        }
        for (BatteryModuleCellRealtime cell : snapshot.getCells()) {
            latest = max(latest, cell.getPollStartedAt());
            latest = max(latest, cell.getCreateTime());
        }
        return latest == null ? snapshot.getRefreshedAt() : latest;
    }

    private Date max(Date left, Date right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.after(left) ? right : left;
    }

    private long resolveFreshThresholdMs() {
        Long value = properties == null ? null : properties.getRealtimeSnapshotFreshThresholdMs();
        return value == null || value <= 0 ? 180_000L : value;
    }

    private String snapshotKey(Integer packNum) {
        return "battery:module:snapshot:" + packNum;
    }

    private Integer resolveBatSinSize(Integer packNum) {
        if (batteryPackService == null || packNum == null) {
            return null;
        }
        BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
        return batteryPack == null ? null : batteryPack.getBatSinSize();
    }

    private List<BatteryModuleCellRealtime> safeList(List<BatteryModuleCellRealtime> cells) {
        return cells == null ? Collections.emptyList() : cells;
    }

    private List<BatteryModuleCellRealtime> sorted(Iterable<BatteryModuleCellRealtime> cells) {
        List<BatteryModuleCellRealtime> result = new ArrayList<>();
        if (cells != null) {
            for (BatteryModuleCellRealtime cell : cells) {
                if (cell != null && cell.getBatNum() != null) {
                    result.add(cell);
                }
            }
        }
        result.sort(Comparator.comparing(BatteryModuleCellRealtime::getBatNum));
        return result;
    }

    private Map<Integer, BatteryModuleCellRealtime> toCellMap(Iterable<BatteryModuleCellRealtime> cells) {
        Map<Integer, BatteryModuleCellRealtime> result = new LinkedHashMap<>();
        if (cells != null) {
            for (BatteryModuleCellRealtime cell : cells) {
                if (cell != null && cell.getBatNum() != null) {
                    result.put(cell.getBatNum(), cell);
                }
            }
        }
        return result;
    }

    private List<BatteryModuleCellRealtime> limit(List<BatteryModuleCellRealtime> cells, Integer batSinSize) {
        if (batSinSize == null || batSinSize <= 0 || cells.size() <= batSinSize) {
            return cells;
        }
        return new ArrayList<>(cells.subList(0, batSinSize));
    }

    /**
     * 解析当前仍缺失的单体编号。
     * batSinSize 只表示组内单体数量，不代表单体编号必须落在 1..batSinSize；
     * 因此不能凭数量反推编号，只能记录已经明确连续缺采的历史编号。
     */
    private Set<Integer> resolveMissingNums(Integer batSinSize, int selectedSize, Set<Integer> staleNums) {
        if (batSinSize == null || batSinSize <= 0 || selectedSize >= batSinSize) {
            return Collections.emptySet();
        }
        return staleNums == null ? Collections.emptySet() : new LinkedHashSet<>(staleNums);
    }
}
