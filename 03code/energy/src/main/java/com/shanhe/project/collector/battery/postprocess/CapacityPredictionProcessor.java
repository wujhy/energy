package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.manage.capacity.service.BatteryPredictorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 容量预测后处理器。
 * <p>
 * 监控电池组状态变化，当状态切换到备电结束时触发容量预测。
 * 读取标准实时模型的 batteryPackStatus，与缓存的上次状态比较。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class CapacityPredictionProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryPredictorService batteryPredictorService;

    /** 上次电池组状态缓存：packNum → batteryPackStatus */
    private final Map<Integer, Integer> lastStatusCache = new ConcurrentHashMap<>();


    @Override
    public String getName() {
        return "capacityPrediction";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        if (context == null || context.getPackNum() == null
                || context.getGroup() == null || context.getCells() == null || context.getCells().isEmpty()) {
            return false;
        }
        String pollBatchNo = context.getPollBatchNo();
        return PostProcessBatchGuard.hasText(pollBatchNo)
                && pollBatchNo.equals(context.getGroup().getPollBatchNo());
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryModuleGroupRealtime group = context.getGroup();
        if (group == null || packNum == null) {
            return;
        }

        Integer currentStatus = group.getBatteryPackStatus();
        if (!isKnownBatteryPackStatus(currentStatus)) {
            return;
        }

        Integer lastStatus = lastStatusCache.put(packNum, currentStatus);
        if (currentStatus.equals(lastStatus)) {
            return;
        }

        if (isStatus(lastStatus) && !isStatus(currentStatus)) {
            triggerCapacityPrediction(packNum, currentStatus, group, context.getCells());
        }
    }

    private void triggerCapacityPrediction(Integer packNum, Integer newStatus,
                                           BatteryModuleGroupRealtime group,
                                           List<BatteryModuleCellRealtime> cells) {
        try {
            batteryPredictorService.doTotalBatteryStep(group, cells);
            log.info("容量预测已触发, packNum={}, newStatus={}", packNum, newStatus);
        } catch (Exception e) {
            log.warn("容量预测触发失败, packNum={}", packNum, e);
        }
    }

    private boolean isKnownBatteryPackStatus(Integer status) {
        return status != null && BatteryPackStatusEnum.find(String.valueOf(status)) != null;
    }

    private boolean isStatus(Integer status) {
        return status != null && BatteryPackStatusEnum.isCode(String.valueOf(status), BatteryPackStatusEnum.BACKUP);
    }

}
