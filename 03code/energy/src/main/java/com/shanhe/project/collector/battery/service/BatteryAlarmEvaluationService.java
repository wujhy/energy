package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 蓄电池告警评估提交服务。
 *
 * <p>消费标准告警评估上下文，统一提交组级、单体告警并执行本轮恢复。</p>
 *
 * @author wjh
 * @since 2026-07-29
 */
@Service
public class BatteryAlarmEvaluationService {

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
    private IAlarmLogService alarmLogService;

    @Resource
    private IBatteryPackService batteryPackService;

    /**
     * 提交告警评估上下文，并恢复本轮未上报单体的告警。
     *
     * @param fallbackPackNum 上下文缺少电池组编号时使用的兜底编号
     * @param context 告警评估上下文
     */
    public void evaluate(Integer fallbackPackNum, BatteryAlarmEvaluationContext context) {
        if (alarmLogService == null || context == null) {
            return;
        }
        Integer packNum = context.getPackNum() == null ? fallbackPackNum : context.getPackNum();
        if (packNum == null) {
            return;
        }
        submitPackWarnings(packNum, packWarnParam(context));
        submitCellWarnings(packNum, cellWarnParam(context));
        recoverCurrentBatchCells(packNum, context);
    }

    private Map<String, String> packWarnParam(BatteryAlarmEvaluationContext context) {
        return Boolean.FALSE.equals(context.getSnapshotFresh())
                ? context.getPackStatusWarnParam()
                : context.getPackWarnParam();
    }

    private Map<Integer, Map<String, String>> cellWarnParam(BatteryAlarmEvaluationContext context) {
        return Boolean.FALSE.equals(context.getSnapshotFresh())
                ? context.getCellStatusWarnParam()
                : context.getCellWarnParam();
    }

    /**
     * 提交电池组级告警候选。
     *
     * @param packNum 电池组编号
     * @param warnParam 告警候选
     */
    public void submitPackWarnings(Integer packNum, Map<String, String> warnParam) {
        if (alarmLogService == null || packNum == null || warnParam == null || warnParam.isEmpty()) {
            return;
        }
        alarmLogService.alarmBatteryValue(packNum, null, warnParam);
    }

    /**
     * 恢复电池组级告警。
     *
     * @param packNum 电池组编号
     * @param itemCodes 需要恢复的告警编码
     */
    public void recoverPackWarnings(Integer packNum, List<String> itemCodes) {
        if (alarmLogService == null || packNum == null || itemCodes == null || itemCodes.isEmpty()) {
            return;
        }
        alarmLogService.alarmFix(packNum, false, null, itemCodes);
    }

    private void submitCellWarnings(Integer packNum, Map<Integer, Map<String, String>> cellWarnParam) {
        if (cellWarnParam == null || cellWarnParam.isEmpty()) {
            return;
        }
        for (Map.Entry<Integer, Map<String, String>> entry : cellWarnParam.entrySet()) {
            Map<String, String> warnParam = entry.getValue();
            if (alarmLogService == null || packNum == null || warnParam == null || warnParam.isEmpty()) {
                continue;
            }
            alarmLogService.alarmBatteryValue(packNum, entry.getKey(), warnParam);
        }
    }

    private void recoverCurrentBatchCells(Integer packNum, BatteryAlarmEvaluationContext context) {
        if (!Boolean.TRUE.equals(context.getSnapshotFresh())) {
            return;
        }
        List<Integer> currentBatchCellNums = currentBatchCellNums(context);
        BatteryPack batteryPack = batteryPackService == null ? null : batteryPackService.selectBatteryInfoByPackNum(packNum);
        if (currentBatchCellNums.isEmpty() || batteryPack == null || batteryPack.getBatSinSize() == null
                || batteryPack.getBatSinSize() <= 0) {
            return;
        }

        for (int cellNum = 1; cellNum <= batteryPack.getBatSinSize(); cellNum++) {
            if (!currentBatchCellNums.contains(cellNum)) {
                return;
            }
        }
        alarmLogService.alarmFix(packNum, true, currentBatchCellNums, ALL_CELL_ALARM_CODES);
    }

    private List<Integer> currentBatchCellNums(BatteryAlarmEvaluationContext context) {
        return context.getCurrentBatchCellNums() == null
                ? Collections.emptyList()
                : context.getCurrentBatchCellNums();
    }
}
