package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;

import java.util.List;

final class PostProcessBatchGuard {

    private PostProcessBatchGuard() {
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static boolean sameRealtimeBatch(BatteryRealtimePostProcessContext context) {
        if (context == null || context.getGroup() == null
                || context.getCells() == null || context.getCells().isEmpty()) {
            return false;
        }
        String pollBatchNo = context.getPollBatchNo();
        return hasText(pollBatchNo)
                && pollBatchNo.equals(context.getGroup().getPollBatchNo())
                && sameCellBatch(pollBatchNo, context.getCells());
    }

    static boolean sameCellBatch(String pollBatchNo, List<BatteryModuleCellRealtime> cells) {
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
