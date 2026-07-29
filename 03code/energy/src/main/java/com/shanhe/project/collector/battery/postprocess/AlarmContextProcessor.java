package com.shanhe.project.collector.battery.postprocess;

import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.service.BatteryAlarmEvaluationService;
import com.shanhe.project.collector.battery.service.BatteryAlarmStateContextService;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 告警上下文适配处理器。
 *
 * <p>从标准实时模型构建告警候选上下文，交给告警评估服务消费。</p>
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class AlarmContextProcessor implements BatteryRealtimePostProcessor {

    @Resource
    private BatteryModuleAlarmAdaptService alarmAdaptService;

    @Resource
    private BatteryAlarmEvaluationService alarmEvaluationService;

    @Resource
    private BatteryAlarmStateContextService alarmStateContextService;

    @Override
    public String getName() {
        return "alarmContext";
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public boolean shouldProcess(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return false;
        }
        if (!PostProcessBatchGuard.hasText(context.getPollBatchNo())) {
            return false;
        }
        if (context.getAlarmContext() != null && alarmEvaluationService != null) {
            return true;
        }
        return context.getPackNum() != null
                && ((alarmAdaptService != null && PostProcessBatchGuard.sameRealtimeBatch(context))
                || (alarmStateContextService != null && context.getChannelConfig() != null));
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return;
        }
        try {
            BatteryAlarmEvaluationContext alarmContext = buildAlarmContext(context);
            fillEvaluationMetadata(context, alarmContext);
            context.setAlarmContext(alarmContext);
            if (alarmEvaluationService != null) {
                alarmEvaluationService.evaluate(context.getPackNum(), alarmContext);
            }
        } catch (Exception e) {
            log.warn("适配蓄电池模块告警上下文失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
    }

    private BatteryAlarmEvaluationContext buildAlarmContext(BatteryRealtimePostProcessContext context) {
        BatteryAlarmEvaluationContext alarmContext = context.getAlarmContext();
        if (alarmContext == null && alarmAdaptService != null && PostProcessBatchGuard.sameRealtimeBatch(context)) {
            alarmContext = alarmAdaptService.buildContext(context.getGroup(), context.getCells());
        }
        if (alarmContext == null && (alarmAdaptService != null || alarmStateContextService != null)) {
            alarmContext = new BatteryAlarmEvaluationContext();
            alarmContext.setPackNum(context.getPackNum());
        }
        if (alarmStateContextService != null && context.getChannelConfig() != null) {
            mergeAlarmContext(alarmContext, alarmStateContextService.buildCommunicationAlarmContext(
                    context.getPackNum(), context.getChannelConfig().getName()));
        }
        return alarmContext;
    }

    /** 将通信告警上下文合并到目标上下文，不覆盖已有的单体告警参数。 */
    private void mergeAlarmContext(BatteryAlarmEvaluationContext target, BatteryAlarmEvaluationContext source) {
        if (target == null || source == null || source.isEmpty()) {
            return;
        }
        if (source.getPackWarnParam() != null) {
            mergePackWarnParam(target, source.getPackWarnParam());
        }
        if (source.getCellWarnParam() == null || source.getCellWarnParam().isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Map<String, String>> entry : source.getCellWarnParam().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            target.getCellWarnParam()
                    .computeIfAbsent(entry.getKey(), key -> new java.util.LinkedHashMap<>())
                    .putAll(entry.getValue());
        }
    }

    private void mergePackWarnParam(BatteryAlarmEvaluationContext target, Map<String, String> sourceWarnParam) {
        for (Map.Entry<String, String> entry : sourceWarnParam.entrySet()) {
            String currentValue = target.getPackWarnParam().get(entry.getKey());
            if ("1".equals(currentValue) && "0".equals(entry.getValue())) {
                continue;
            }
            target.getPackWarnParam().put(entry.getKey(), entry.getValue());
        }
    }

    private void fillEvaluationMetadata(BatteryRealtimePostProcessContext context,
                                        BatteryAlarmEvaluationContext alarmContext) {
        if (alarmContext == null) {
            return;
        }
        alarmContext.setSnapshotFresh(PostProcessBatchGuard.sameRealtimeBatch(context));
        List<Integer> currentBatchCellNums = new ArrayList<>();
        List<Integer> staleCellNums = new ArrayList<>();
        if (context.getCells() != null) {
            for (BatteryModuleCellRealtime cell : context.getCells()) {
                if (cell == null || cell.getBatNum() == null) {
                    continue;
                }
                if (PostProcessBatchGuard.sameBatch(context.getPollBatchNo(), cell.getPollBatchNo())) {
                    currentBatchCellNums.add(cell.getBatNum());
                } else {
                    staleCellNums.add(cell.getBatNum());
                }
            }
        }
        alarmContext.setCurrentBatchCellNums(currentBatchCellNums);
        alarmContext.setStaleCellNums(staleCellNums);
    }
}
