package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class BatteryModuleAlarmAdaptServiceTest {

    private final BatteryModuleAlarmAdaptService service = new BatteryModuleAlarmAdaptService();

    @Test
    void shouldBuildLeakageAlarmCandidates() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setGroupModuleFresh(false);

        BatteryAlarmEvaluationContext context = service.buildContext(group,
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

        BatteryAlarmEvaluationContext context = service.buildContext(group, null);

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
        Assertions.assertTrue(context.getCellWarnParam().isEmpty());
    }

    @Test
    void shouldKeepSameThresholdItemCodeSeparatedByCellNumber() {
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setVoltage(2.1d);
        cell1.setResistance(101);
        cell1.setTemperature(25.5d);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setVoltage(2.2d);
        cell2.setResistance(102);

        BatteryAlarmEvaluationContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

        Assertions.assertEquals("2.1", context.getCellWarnParam().get(1).get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.1", context.getCellWarnParam().get(1).get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("101", context.getCellWarnParam().get(1).get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("101", context.getCellWarnParam().get(1).get(ItemCode.DTNZGX.getCode()));
        Assertions.assertEquals("25.5", context.getCellWarnParam().get(1).get(ItemCode.DTDCWDG.getCode()));
        Assertions.assertEquals("25.5", context.getCellWarnParam().get(1).get(ItemCode.DTDCWDD.getCode()));
        Assertions.assertEquals("2.2", context.getCellWarnParam().get(2).get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.2", context.getCellWarnParam().get(2).get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("102", context.getCellWarnParam().get(2).get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("102", context.getCellWarnParam().get(2).get(ItemCode.DTNZGX.getCode()));
    }

    @Test
    void shouldKeepSameItemCodeSeparatedAcrossMultipleCellsInFullPipeline() {
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setVoltage(2.1d);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setVoltage(2.2d);
        BatteryModuleCellRealtime cell3 = cell(3, null);
        cell3.setVoltage(2.3d);

        BatteryAlarmEvaluationContext context = service.buildContext(null, Arrays.asList(cell1, cell2, cell3));

        Assertions.assertEquals(3, context.getCellWarnParam().size());
        Assertions.assertEquals("2.1", context.getCellWarnParam().get(1).get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.1", context.getCellWarnParam().get(1).get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("2.2", context.getCellWarnParam().get(2).get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.2", context.getCellWarnParam().get(2).get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("2.3", context.getCellWarnParam().get(3).get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.3", context.getCellWarnParam().get(3).get(ItemCode.DTDYGF.getCode()));
    }

    @Test
    void shouldBuildGroupThresholdAlarmCandidates() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setChargeDischargeCurrent(-12.3d);
        group.setEnvironmentTemperature1(28.8d);
        group.setBatteryPackSoc(86.5d);
        group.setBatteryPackSoh(97.5d);

        BatteryAlarmEvaluationContext context = service.buildContext(group, null);

        Assertions.assertEquals("230.5", context.getPackWarnParam().get(ItemCode.ZDYGC.getCode()));
        Assertions.assertEquals("230.5", context.getPackWarnParam().get(ItemCode.ZDYGF.getCode()));
        Assertions.assertEquals("-12.3", context.getPackWarnParam().get(ItemCode.ZCGDLGJ.getCode()));
        Assertions.assertEquals("28.8", context.getPackWarnParam().get(ItemCode.ZWDG.getCode()));
        Assertions.assertEquals("28.8", context.getPackWarnParam().get(ItemCode.ZWDD.getCode()));
        Assertions.assertEquals("86.5", context.getPackWarnParam().get(ItemCode.ZSOCDGJ.getCode()));
        Assertions.assertEquals("97.5", context.getPackWarnParam().get(ItemCode.ZSOHDGJ.getCode()));
    }

    @Test
    void shouldIgnoreUnsupportedM460AlarmSourcesWithoutConfirmedEnergyMapping() {
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);
        group.setHydrogenConcentration(12.3d);

        BatteryAlarmEvaluationContext context = service.buildContext(group, Collections.emptyList());

        Assertions.assertTrue(context.getPackWarnParam().isEmpty());
        Assertions.assertTrue(context.getCellWarnParam().isEmpty());
    }

    /** ALARM-002: 同一 itemCode 不同 batNum（内阻/温度）不互相覆盖。 */
    @Test
    void shouldIsolateSameItemCodeAcrossDifferentBatNumByResistanceAndTemperature() {
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setResistance(100);
        cell1.setTemperature(30.0d);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setResistance(200);
        cell2.setTemperature(40.0d);

        BatteryAlarmEvaluationContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

        // batNum=1 entries
        Map<String, String> warns1 = context.getCellWarnParam().get(1);
        Assertions.assertNotNull(warns1);
        Assertions.assertEquals("100", warns1.get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("100", warns1.get(ItemCode.DTNZGX.getCode()));
        Assertions.assertEquals("30.0", warns1.get(ItemCode.DTDCWDG.getCode()));
        Assertions.assertEquals("30.0", warns1.get(ItemCode.DTDCWDD.getCode()));
        // batNum=2 entries — same itemCodes, different values
        Map<String, String> warns2 = context.getCellWarnParam().get(2);
        Assertions.assertNotNull(warns2);
        Assertions.assertEquals("200", warns2.get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("200", warns2.get(ItemCode.DTNZGX.getCode()));
        Assertions.assertEquals("40.0", warns2.get(ItemCode.DTDCWDG.getCode()));
        Assertions.assertEquals("40.0", warns2.get(ItemCode.DTDCWDD.getCode()));
    }

    /** ALARM-002: 恢复逻辑只恢复目标单体，不影响其他单体的告警。 */
    @Test
    void shouldOnlyRecoverTargetCellWithoutAffectingOtherCellAlarms() {
        // cell1 voltage returns to normal (2.0), cell2 still overcharged (2.5)
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setVoltage(2.0d);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setVoltage(2.5d);

        BatteryAlarmEvaluationContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

        // Both cells must have independent DTDYGC entries
        Assertions.assertEquals(2, context.getCellWarnParam().size());
        // cell 1 still reports its own voltage value (recovery candidate)
        Assertions.assertEquals("2.0", context.getCellWarnParam().get(1).get(ItemCode.DTDYGC.getCode()));
        // cell 2 still reports its own voltage value (alarm still active)
        Assertions.assertEquals("2.5", context.getCellWarnParam().get(2).get(ItemCode.DTDYGC.getCode()));
        // Each cell map has exactly two entries (DTDYGC + DTDYGF) — no cross-contamination
        Assertions.assertEquals(2, context.getCellWarnParam().get(1).size());
        Assertions.assertEquals(2, context.getCellWarnParam().get(2).size());
    }

    private BatteryModuleCellRealtime cell(int batNum, Integer leakageStatus) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setLeakageStatus(leakageStatus);
        return cell;
    }

}
