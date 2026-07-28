package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.manage.stat.service.IStatBatteryResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内阻测试完成后，等待下一轮完整实时采集再生成内阻统计。
 */
@Slf4j
@Component
public class ResistanceStatisticsAfterCompletionProcessor implements BatteryRealtimePostProcessor {

    private final Map<Integer, PendingStatistics> pendingByPack = new ConcurrentHashMap<>();

    @Resource
    private IStatBatteryResService statBatteryResService;

    @Override
    public String getName() {
        return "resistanceStatisticsAfterCompletion";
    }

    @Override
    public int getOrder() {
        return 320;
    }

    public void deferAfterNextRealtimeBatch(Integer packNum, Long businessOptLogId, String excludedPollBatchNo) {
        if (packNum == null || businessOptLogId == null) {
            return;
        }
        pendingByPack.put(packNum, new PendingStatistics(businessOptLogId, excludedPollBatchNo));
        log.debug("内阻测试完成，等待下一轮完整实时采集生成统计, packNum={}, optLogId={}, excludedBatch={}",
                packNum, businessOptLogId, excludedPollBatchNo);
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && pendingByPack.containsKey(context.getPackNum())
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        if (!PostProcessBatchGuard.sameRealtimeBatch(context)) {
            return;
        }
        Integer packNum = context.getPackNum();
        PendingStatistics pending = pendingByPack.get(packNum);
        if (pending == null) {
            return;
        }
        if (Objects.equals(context.getPollBatchNo(), pending.excludedPollBatchNo)) {
            log.debug("跳过内阻测试完成所在采集批次, packNum={}, optLogId={}, batch={}",
                    packNum, pending.businessOptLogId, context.getPollBatchNo());
            return;
        }
        if (!isCompleteCurrentRealtime(context)) {
            return;
        }
        if (statBatteryResService == null) {
            return;
        }
        statBatteryResService.initRealtime(packNum, context.getCells());
        if (pendingByPack.remove(packNum, pending)) {
            log.info("内阻测试完成后的下一轮完整采集已触发统计生成, packNum={}, optLogId={}, batch={}",
                    packNum, pending.businessOptLogId, context.getPollBatchNo());
        }
    }

    private boolean isCompleteCurrentRealtime(BatteryRealtimePostProcessContext context) {
        List<BatteryModuleCellRealtime> cells = context.getCells();
        if (cells == null || cells.isEmpty()) {
            return false;
        }
        BatteryModuleRealtimeSnapshot snapshot = context.getRealtimeSnapshot();
        Integer expectedCellCount = snapshot == null ? null : snapshot.getBatSinSize();
        if (expectedCellCount != null && expectedCellCount > 0 && cells.size() < expectedCellCount) {
            return false;
        }
        if (snapshot != null && !snapshot.getMissingCellNums().isEmpty()) {
            return false;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null || cell.getResistance() == null) {
                return false;
            }
        }
        return true;
    }

    private static class PendingStatistics {
        private final Long businessOptLogId;
        private final String excludedPollBatchNo;

        private PendingStatistics(Long businessOptLogId, String excludedPollBatchNo) {
            this.businessOptLogId = businessOptLogId;
            this.excludedPollBatchNo = excludedPollBatchNo;
        }
    }
}
