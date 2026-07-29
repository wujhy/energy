package com.shanhe.project.collector.battery.postprocess;


import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.service.BatteryModuleAlarmAdaptService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.shanhe.framework.enums.ItemCode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 告警上下文适配处理器。
 * <p>
 * 从标准实时模型构建告警候选上下文，供 AlarmLogService 消费。
 *
 * @author wjh
 * @since 2026-06-04
 */
@Slf4j
@Component
public class AlarmContextProcessor implements BatteryRealtimePostProcessor {

    /** 所有单体电池告警编码列表，用于恢复本轮未上报数据的单体告警。 */
    private static final List<String> ALL_CELL_ALARM_CODES = Arrays.asList(
            ItemCode.DTDCWDD.getCode(),
            ItemCode.DTDCWDG.getCode(),
            ItemCode.DTNZGX.getCode(),
            ItemCode.DTNZGD.getCode(),
            ItemCode.DTDYGF.getCode(),
            ItemCode.DTDYGC.getCode(),
            ItemCode.DTLJTGJ.getCode(),
            ItemCode.DTDCKL.getCode(),
            ItemCode.DTFCDYD.getCode(),
            ItemCode.DTFCDYG.getCode(),
            ItemCode.DTNZBJ.getCode(),
            ItemCode.DTDCWDBJ.getCode(),
            ItemCode.DTDYBJ.getCode(),
            ItemCode.DTGB.getCode(),
            ItemCode.DTLYGJ.getCode(),
            ItemCode.DTWDCGQGZ.getCode(),
            ItemCode.DTTXZT.getCode()
    );

    @Resource
    private BatteryModuleAlarmAdaptService alarmAdaptService;

    @Resource
    private IAlarmLogService alarmLogService;

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
        if (context.getAlarmContext() != null && alarmLogService != null) {
            return true;
        }
        return alarmAdaptService != null && context.getPackNum() != null
                && (PostProcessBatchGuard.sameRealtimeBatch(context) || context.getChannelConfig() != null);
    }

    @Override
    public void process(BatteryRealtimePostProcessContext context) {
        if (context == null) {
            return;
        }
        try {
            BatteryAlarmEvaluationContext alarmContext = context.getAlarmContext();
            if (alarmContext == null && alarmAdaptService != null && PostProcessBatchGuard.sameRealtimeBatch(context)) {
                alarmContext = alarmAdaptService.buildContext(context.getGroup(), context.getCells());
            }
            if (alarmContext == null && alarmAdaptService != null) {
                alarmContext = new BatteryAlarmEvaluationContext();
                alarmContext.setPackNum(context.getPackNum());
            }
            if (alarmAdaptService != null && context.getChannelConfig() != null) {
                mergeAlarmContext(alarmContext, alarmAdaptService.buildCommunicationAlarmContext(
                        context.getPackNum(), context.getChannelConfig().getName()));
            }
            fillEvaluationMetadata(context, alarmContext);
            context.setAlarmContext(alarmContext);
            handleAlarmContext(context, alarmContext);
        } catch (Exception e) {
            log.warn("适配蓄电池模块告警上下文失败, 通道={}, 电池组={}",
                    context.getChannelConfig() == null ? null : context.getChannelConfig().getName(),
                    context.getPackNum(),
                    e);
        }
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
            for (com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime cell : context.getCells()) {
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
    /** 将告警上下文提交给告警服务，并恢复本轮未上报单体的告警。 */
    private void handleAlarmContext(BatteryRealtimePostProcessContext context,
                                    BatteryAlarmEvaluationContext alarmContext) {
        if (alarmLogService == null || alarmContext == null) {
            return;
        }
        Integer packNum = alarmContext.getPackNum() == null ? context.getPackNum() : alarmContext.getPackNum();
        if (packNum == null) {
            return;
        }

        if (alarmContext.getPackWarnParam() != null && !alarmContext.getPackWarnParam().isEmpty()) {
            alarmLogService.alarmBatteryValue(null, packNum, null, alarmContext.getPackWarnParam());
        }

        if (alarmContext.getCellWarnParam() != null && !alarmContext.getCellWarnParam().isEmpty()) {
            for (Map.Entry<Integer, Map<String, String>> entry : alarmContext.getCellWarnParam().entrySet()) {
                Map<String, String> warnParam = entry.getValue();
                if (warnParam == null || warnParam.isEmpty()) {
                    continue;
                }
                alarmLogService.alarmBatteryValue(null, packNum, entry.getKey(), warnParam);
            }
        }
        alarmLogService.alarmFix(packNum, true, alarmContext.getCurrentBatchCellNums(), ALL_CELL_ALARM_CODES);
    }

}
