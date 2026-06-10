package com.shanhe.project.collector.battery.service.postprocess;

import com.shanhe.framework.enums.BatteryPackStatusEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.energy.capacity.service.BatteryPredictorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
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

    @Resource
    private BatteryReportLogService batteryReportLogService;

    /** 上次电池组状态缓存：packNum → batteryPackStatus */
    private final Map<Integer, String> lastStatusCache = new ConcurrentHashMap<>();

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
        return context != null
                && context.getPackNum() != null
                && context.getGroup() != null
                && hasText(context.getPollBatchNo())
                && context.getPollBatchNo().equals(context.getGroup().getPollBatchNo());
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryModuleGroupRealtime group = context.getGroup();
        if (group == null || packNum == null) {
            return;
        }

        // 从标准实时模型获取当前电池组状态
        Integer statusInt = group.getBatteryPackStatus();
        String currentStatus = statusInt != null ? String.valueOf(statusInt) : null;
        if (currentStatus == null || !isKnownBatteryPackStatus(currentStatus)) {
            return;
        }

        // 检查状态是否发生变化
        String lastStatus = lastStatusCache.get(packNum);
        if (Objects.equals(currentStatus, lastStatus)) {
            return;
        }
        lastStatusCache.put(packNum, currentStatus);

        // 状态从备电切换到其他状态时，触发容量预测
        if (BatteryPackStatusEnum.isCode(lastStatus, BatteryPackStatusEnum.BACKUP)
                && !BatteryPackStatusEnum.isCode(currentStatus, BatteryPackStatusEnum.BACKUP)) {
            triggerCapacityPrediction(packNum, currentStatus);
        }
    }

    private void triggerCapacityPrediction(Integer packNum, String newStatus) {
        try {
            BatteryReportLog oldInfo = batteryReportLogService.lastCache(packNum);
            batteryPredictorService.doTotalBatteryStep(packNum, newStatus, oldInfo);
            log.info("容量预测已触发, packNum={}, newStatus={}", packNum, newStatus);
        } catch (Exception e) {
            log.warn("容量预测触发失败, packNum={}", packNum, e);
        }
    }

    private boolean isKnownBatteryPackStatus(String status) {
        return BatteryPackStatusEnum.find(status) != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
