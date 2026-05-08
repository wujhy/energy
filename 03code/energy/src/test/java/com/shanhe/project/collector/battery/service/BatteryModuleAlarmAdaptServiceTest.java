package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class BatteryModuleAlarmAdaptServiceTest {

    private final BatteryModuleAlarmAdaptService service = new BatteryModuleAlarmAdaptService();

    @Test
    void shouldBuildLeakageAlarmCandidates() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setGroupModuleFresh(false);

        BatteryModuleAlarmContext context = service.buildContext(group,
                Arrays.asList(cell(1, 1), cell(2, 0), cell(3, null)));

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
        Assertions.assertEquals("1", context.getCellWarnParam().get(1).get(ItemCode.DTLYGJ.getCode()));
        Assertions.assertEquals("0", context.getCellWarnParam().get(2).get(ItemCode.DTLYGJ.getCode()));
        Assertions.assertFalse(context.getCellWarnParam().containsKey(3));
    }

    @Test
    void shouldBuildGroupModuleRecoveredCandidate() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setGroupModuleFresh(true);

        BatteryModuleAlarmContext context = service.buildContext(group, null);

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
        Assertions.assertTrue(context.getCellWarnParam().isEmpty());
    }

    private BatteryModuleCellRealtime cell(int batNum, Integer leakageStatus) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setLeakageStatus(leakageStatus);
        return cell;
    }
}
