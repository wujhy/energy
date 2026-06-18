package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.service.IBatteryPackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * 电压极差更新处理器。
 * <p>
 * 从标准实时模型计算电压极差并更新 BatteryPack。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class VoltageRangeProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private IBatteryPackService batteryPackService;

    @Override
    public String getName() {
        return "voltageRange";
    }

    @Override
    public int getOrder() {
        return 400;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && context.getCells() != null
                && !context.getCells().isEmpty()
                && PostProcessBatchGuard.sameCellBatch(context.getPollBatchNo(), context.getCells());
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        List<BatteryModuleCellRealtime> cells = context.getCells();
        if (cells == null || cells.isEmpty()) {
            return;
        }

        DoubleSummaryStatistics stats = cells.stream()
                .filter(c -> c != null && c.getVoltage() != null)
                .mapToDouble(BatteryModuleCellRealtime::getVoltage)
                .summaryStatistics();

        if (stats.getCount() == 0) {
            return;
        }

        // V → mV，取整
        int voltageRange = (int) ((stats.getMax() - stats.getMin()) * 1000);

        try {
            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
            if (batteryPack != null) {
                batteryPack.setVoltageRange(voltageRange);
                batteryPackService.update(batteryPack);
            }
        } catch (Exception e) {
            log.warn("电压极差更新失败, packNum={}", packNum, e);
        }
    }

}
