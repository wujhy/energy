package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
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
    public BatteryModuleAlarmContext buildContext(BatteryModuleGroupRealtime group,
                                                  List<BatteryModuleCellRealtime> cells) {
        BatteryModuleAlarmContext context = new BatteryModuleAlarmContext();
        if (group != null) {
            context.setPackNum(group.getPackNum());
            appendGroupDirectStatus(context, group);
        }
        appendCellDirectStatus(context, cells);
        return context;
    }

    private void appendGroupDirectStatus(BatteryModuleAlarmContext context, BatteryModuleGroupRealtime group) {
        if (group.getGroupModuleFresh() != null) {
            context.putPackWarn(ItemCode.TXZT.getCode(), Boolean.TRUE.equals(group.getGroupModuleFresh()) ? "0" : "1");
        }
    }

    private void appendCellDirectStatus(BatteryModuleAlarmContext context, List<BatteryModuleCellRealtime> cells) {
        if (cells == null || cells.isEmpty()) {
            return;
        }
        for (BatteryModuleCellRealtime cell : cells) {
            if (cell == null || cell.getBatNum() == null) {
                continue;
            }
            if (cell.getLeakageStatus() != null) {
                context.putCellWarn(cell.getBatNum(), ItemCode.DTLYGJ.getCode(), toAlarmValue(cell.getLeakageStatus()));
            }
        }
    }

    private String toAlarmValue(Integer value) {
        return value != null && value == 1 ? "1" : "0";
    }
}
