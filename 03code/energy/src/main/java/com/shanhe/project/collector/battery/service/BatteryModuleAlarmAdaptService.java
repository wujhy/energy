package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 600节模块端实时数据告警适配服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Service
public class BatteryModuleAlarmAdaptService {

    /**
     * 根据当前轮采集结果构建告警候选。
     *
     * @param group 组实时数据
     * @param cells 单体实时数据
     * @return 告警适配上下文
     */
    public BatteryAlarmEvaluationContext buildContext(BatteryModuleGroupRealtime group,
                                                  List<BatteryModuleCellRealtime> cells) {
        BatteryAlarmEvaluationContext context = new BatteryAlarmEvaluationContext();
        if (group != null) {
            context.setPackNum(group.getPackNum());
            appendGroupDirectStatus(context, group);
            appendGroupThreshold(context, group);
        }
        appendCellStatus(context, cells);
        return context;
    }

    /** 追加单体阈值告警候选，按单体编号隔离 itemCode，避免多个单体互相覆盖。 */
    private void appendCellThreshold(BatteryAlarmEvaluationContext context, BatteryModuleCellRealtime cell) {
        Integer batNum = cell.getBatNum();
        if (cell.getVoltage() != null) {
            String value = String.valueOf(cell.getVoltage());
            context.putCellThresholdWarn(batNum, ItemCode.DTDYGC.getCode(), value);
            context.putCellThresholdWarn(batNum, ItemCode.DTDYGF.getCode(), value);
        }
        if (cell.getResistance() != null) {
            String value = String.valueOf(cell.getResistance());
            context.putCellThresholdWarn(batNum, ItemCode.DTNZGD.getCode(), value);
            context.putCellThresholdWarn(batNum, ItemCode.DTNZGX.getCode(), value);
        }
        if (cell.getTemperature() != null) {
            String value = String.valueOf(cell.getTemperature());
            context.putCellThresholdWarn(batNum, ItemCode.DTDCWDG.getCode(), value);
            context.putCellThresholdWarn(batNum, ItemCode.DTDCWDD.getCode(), value);
        }
        if (cell.getSwollenVoltage() != null) {
            context.putCellThresholdWarn(batNum, ItemCode.DTGB.getCode(), String.valueOf(cell.getSwollenVoltage()));
        }
    }

    /** 追加组阈值告警候选。 */
    private void appendGroupThreshold(BatteryAlarmEvaluationContext context, BatteryModuleGroupRealtime group) {
        Double groupVoltage = groupVoltage(group);
        if (groupVoltage != null) {
            String value = String.valueOf(groupVoltage);
            context.putPackThresholdWarn(ItemCode.ZDYGC.getCode(), value);
            context.putPackThresholdWarn(ItemCode.ZDYGF.getCode(), value);
        }
        Double current = group.getChargeDischargeCurrent();
        if (current != null) {
            context.putPackThresholdWarn(ItemCode.ZCGDLGJ.getCode(), String.valueOf(current));
        }
        if (group.getEnvironmentTemperature1() != null) {
            String value = String.valueOf(group.getEnvironmentTemperature1());
            context.putPackThresholdWarn(ItemCode.ZWDG.getCode(), value);
            context.putPackThresholdWarn(ItemCode.ZWDD.getCode(), value);
        }
        if (group.getBatteryPackSoc() != null) {
            context.putPackThresholdWarn(ItemCode.ZSOCDGJ.getCode(), String.valueOf(group.getBatteryPackSoc()));
        }
        if (group.getBatteryPackSoh() != null) {
            context.putPackThresholdWarn(ItemCode.ZSOHDGJ.getCode(), String.valueOf(group.getBatteryPackSoh()));
        }
    }

    private Double groupVoltage(BatteryModuleGroupRealtime group) {
        if (group.getPackVoltage() != null) {
            return group.getPackVoltage();
        }
        if (group.getBatteryPackOuterVoltage() != null) {
            return group.getBatteryPackOuterVoltage();
        }
        return group.getExternalVoltage();
    }

    /** 追加电池组通信状态告警。 */
    private void appendGroupDirectStatus(BatteryAlarmEvaluationContext context, BatteryModuleGroupRealtime group) {
        if (group.getGroupModuleFresh() != null) {
            context.putPackStatusWarn(ItemCode.TXZT.getCode(), Boolean.TRUE.equals(group.getGroupModuleFresh()) ? "0" : "1");
        }
    }

    /** 追加单体直接状态和阈值告警。 */
    private void appendCellStatus(BatteryAlarmEvaluationContext context, List<BatteryModuleCellRealtime> cells) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null) {
                continue;
            }
            if (cell.getLeakageStatus() != null) {
                context.putCellStatusWarn(cell.getBatNum(), ItemCode.DTLYGJ.getCode(), toAlarmValue(cell.getLeakageStatus()));
            }
            appendCellThreshold(context, cell);
        }
    }

    /** 将状态值转换为告警值字符串。 */
    private String toAlarmValue(Integer value) {
        return value != null && value == 1 ? "1" : "0";
    }
}
