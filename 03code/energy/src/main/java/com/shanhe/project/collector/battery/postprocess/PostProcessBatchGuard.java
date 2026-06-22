package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;

import java.util.List;

/**
 * 后处理批次守卫工具类。
 *
 * <p>提供批次号一致性和非空校验，用于后处理器的 {@code shouldProcess} 前置判断。</p>
 */
public final class PostProcessBatchGuard {

    private PostProcessBatchGuard() {
    }

    /**
     * 判断字符串非空且非空白。
     *
     * @param value 待校验字符串
     * @return 是否非空非空白
     */
    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 校验上下文的 pollBatchNo 与组数据、单体数据的批次号一致。
     *
     * @param context 后处理上下文
     * @return 批次号一致且非空
     */
    public static boolean sameRealtimeBatch(BatteryRealtimePostProcessContext context) {
        if (context == null || context.getGroup() == null
                || context.getCells() == null || context.getCells().isEmpty()) {
            return false;
        }
        String pollBatchNo = context.getPollBatchNo();
        return hasText(pollBatchNo)
                && pollBatchNo.equals(context.getGroup().getPollBatchNo())
                && sameCellBatch(pollBatchNo, context.getCells());
    }

    /**
     * 校验所有单体的批次号与指定批次号一致。
     *
     * @param pollBatchNo 期望的批次号
     * @param cells 单体列表
     * @return 所有单体批次号一致且非空
     */
    public static boolean sameCellBatch(String pollBatchNo, List<BatteryModuleCellRealtime> cells) {
        if (!hasText(pollBatchNo) || cells == null || cells.isEmpty()) {
            return false;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || !pollBatchNo.equals(cell.getPollBatchNo())) {
                return false;
            }
        }
        return true;
    }
}
