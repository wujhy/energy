package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.stat.service.IStatBatteryResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内阻统计后处理器。
 * <p>
 * 仅在标准实时模型明确提供内阻测试状态时复用旧统计服务；
 * 当前不推断 91 连接条结果，也不把普通采集内阻误判为测试结束。
 *
 * @author wjh
 * @since 2026-06-05
 */
@Slf4j
@Component
public class ResistanceStatisticsProcessor implements BatteryRealtimePostProcessor {

    private static final String RESISTANCE_TEST_STATUS = "resistanceTestStatus";

    private final Set<String> processedBatches = ConcurrentHashMap.newKeySet();

    @Resource
    private IStatBatteryResService statBatteryResService;

    @Resource
    private BatteryReportLogService batteryReportLogService;

    @Resource
    private IBatteryPackService batteryPackService;

    @Override
    public String getName() {
        return "resistanceStatistics";
    }

    @Override
    public int getOrder() {
        return 350;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        return context != null
                && context.getPackNum() != null
                && context.getGroup() != null
                && context.getGroup().getResistanceTestStatus() != null
                && context.getCells() != null
                && !context.getCells().isEmpty()
                && PostProcessBatchGuard.sameRealtimeBatch(context);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        Integer packNum = context.getPackNum();
        BatteryReportLog report = RealtimeToReportLogAdapter.adapt(
                packNum, context.getGroup(), context.getCells());
        Map<String, Object> packParam = report.getPackParam();
        if (!packParam.containsKey(RESISTANCE_TEST_STATUS)) {
            log.debug("内阻统计后处理跳过：缺少内阻测试状态, packNum={}", packNum);
            return;
        }
        if (!isCompleteResistanceBatch(context)) {
            return;
        }
        String batchKey = packNum + ":" + context.getPollBatchNo();
        if (!processedBatches.add(batchKey)) {
            return;
        }

        try {
            BatteryReportLog oldInfo = batteryReportLogService.lastCache(packNum);
            statBatteryResService.init(packNum, packParam, report.getBatteryList(), oldInfo);
        } catch (Exception e) {
            processedBatches.remove(batchKey);
            log.warn("内阻统计后处理失败, packNum={}", packNum, e);
        }
    }

    /** 校验本轮单体内阻是否完整：数量达标且每个单体都有内阻值。 */
    private boolean isCompleteResistanceBatch(BatteryRealtimePostProcessContext context) {
        Integer expectedCellCount = resolveExpectedCellCount(context.getPackNum());
        if (expectedCellCount != null && expectedCellCount > 0 && context.getCells().size() < expectedCellCount) {
            return false;
        }
        for (com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime cell : context.getCells()) {
            if (cell == null || cell.getBatNum() == null || cell.getResistance() == null) {
                return false;
            }
        }
        return true;
    }

    /** 从电池组配置读取期望单体数量。 */
    private Integer resolveExpectedCellCount(Integer packNum) {
        if (batteryPackService == null || packNum == null) {
            return null;
        }
        try {
            BatteryPack batteryPack = batteryPackService.selectBatteryInfoByPackNum(packNum);
            return batteryPack == null ? null : batteryPack.getBatSinSize();
        } catch (Exception e) {
            log.debug("获取电池组单体数失败, packNum={}", packNum, e);
            return null;
        }
    }
}
