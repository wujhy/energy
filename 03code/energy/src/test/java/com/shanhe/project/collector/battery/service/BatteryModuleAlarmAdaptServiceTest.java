package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleAlarmContext;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
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

    @Test
    void shouldKeepSameThresholdItemCodeSeparatedByCellNumber() {
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setVoltage(2.1d);
        cell1.setResistance(101);
        cell1.setTemperature(25.5d);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setVoltage(2.2d);
        cell2.setResistance(102);

        BatteryModuleAlarmContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

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

        BatteryModuleAlarmContext context = service.buildContext(null, Arrays.asList(cell1, cell2, cell3));

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
        group.setBatteryPackOuterVoltage(230.5d);
        group.setChargeDischargeCurrent(-12.3d);
        group.setEnvironmentTemperature1(28.8d);
        group.setBatteryPackSoc(86.5d);
        group.setBatteryPackSoh(97.5d);

        BatteryModuleAlarmContext context = service.buildContext(group, null);

        Assertions.assertEquals("230.5", context.getPackWarnParam().get(ItemCode.ZDYGC.getCode()));
        Assertions.assertEquals("230.5", context.getPackWarnParam().get(ItemCode.ZDYGF.getCode()));
        Assertions.assertEquals("-12.3", context.getPackWarnParam().get(ItemCode.ZCGDLGJ.getCode()));
        Assertions.assertEquals("28.8", context.getPackWarnParam().get(ItemCode.ZWDG.getCode()));
        Assertions.assertEquals("28.8", context.getPackWarnParam().get(ItemCode.ZWDD.getCode()));
        Assertions.assertEquals("86.5", context.getPackWarnParam().get(ItemCode.ZSOCDGJ.getCode()));
        Assertions.assertEquals("97.5", context.getPackWarnParam().get(ItemCode.ZSOHDGJ.getCode()));
    }

    @Test
    void shouldBuildFlatThresholdAlarmCandidatesForHighAndLowItems() {
        BatteryModuleCellRealtime cell = cell(1, null);
        cell.setVoltage(2.1d);
        cell.setResistance(101);
        cell.setTemperature(25.5d);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setBatteryPackOuterVoltage(230.5d);
        group.setEnvironmentTemperature1(28.8d);

        Map<String, String> warnParam = service.buildThresholdAlarmParam(1, Collections.singletonList(cell), group);

        Assertions.assertEquals("2.1", warnParam.get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.1", warnParam.get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("101", warnParam.get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("101", warnParam.get(ItemCode.DTNZGX.getCode()));
        Assertions.assertEquals("25.5", warnParam.get(ItemCode.DTDCWDG.getCode()));
        Assertions.assertEquals("25.5", warnParam.get(ItemCode.DTDCWDD.getCode()));
        Assertions.assertEquals("230.5", warnParam.get(ItemCode.ZDYGC.getCode()));
        Assertions.assertEquals("230.5", warnParam.get(ItemCode.ZDYGF.getCode()));
        Assertions.assertEquals("28.8", warnParam.get(ItemCode.ZWDG.getCode()));
        Assertions.assertEquals("28.8", warnParam.get(ItemCode.ZWDD.getCode()));
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldDocumentFlatThresholdAlarmParamAsSingleCellCompatibilityPath() {
        BatteryModuleCellRealtime cell1 = cell(1, null);
        cell1.setVoltage(2.1d);
        cell1.setResistance(101);
        BatteryModuleCellRealtime cell2 = cell(2, null);
        cell2.setVoltage(2.2d);
        cell2.setResistance(102);

        Map<String, String> warnParam = service.buildThresholdAlarmParam(1, Arrays.asList(cell1, cell2), null);

        Assertions.assertEquals("2.2", warnParam.get(ItemCode.DTDYGC.getCode()));
        Assertions.assertEquals("2.2", warnParam.get(ItemCode.DTDYGF.getCode()));
        Assertions.assertEquals("102", warnParam.get(ItemCode.DTNZGD.getCode()));
        Assertions.assertEquals("102", warnParam.get(ItemCode.DTNZGX.getCode()));
    }

    @Test
    void shouldMapStaleGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithGroup246Freshness("stale");

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapFreshGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithGroup246Freshness("fresh");

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapMissingGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithGroup246Freshness(null);

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldMapClosedChannelOpenToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(channelState(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN,
                        "closed", BatteryDeviceStateConstants.StateLevel.ERROR)));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.DTTXZT.getCode()));
    }

    @Test
    void shouldMapChannelErrorToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(channelState(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                        "read failed", BatteryDeviceStateConstants.StateLevel.ERROR)));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.DTTXZT.getCode()));
    }

    @Test
    void shouldMapActiveModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout(1, "01/81", BatteryDeviceStateConstants.StateLevel.WARN)));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapRecoveredModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout(1, "recovered", BatteryDeviceStateConstants.StateLevel.NORMAL)));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldMapInactiveModuleActiveToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleActive(1, "inactive")));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapActiveModuleActiveToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleActive(1, "active")));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldIgnoreModuleCommunicationStatesFromOtherPack() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Arrays.asList(
                        moduleTimeout(2, "01/81", BatteryDeviceStateConstants.StateLevel.WARN),
                        moduleActive(2, "inactive")));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldIgnoreNullModuleActiveStateAndContinueWithGroup246Freshness() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates("stale",
                Collections.singletonList(null));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
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

        BatteryModuleAlarmContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

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

        BatteryModuleAlarmContext context = service.buildContext(null, Arrays.asList(cell1, cell2));

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

    private BatteryModuleAlarmAdaptService communicationServiceWithGroup246Freshness(String stateValue) throws Exception {
        return communicationServiceWithStates(stateValue, Collections.emptyList());
    }

    private BatteryModuleAlarmAdaptService communicationServiceWithStates(String stateValue,
                                                                          List<BatteryDeviceState> channelStates) throws Exception {
        BatteryModuleAlarmAdaptService service = new BatteryModuleAlarmAdaptService();
        Field field = BatteryModuleAlarmAdaptService.class.getDeclaredField("batteryDeviceStateService");
        field.setAccessible(true);
        field.set(service, new StubBatteryDeviceStateService(stateValue, channelStates));
        return service;
    }

    private BatteryDeviceState channelState(String stateCode, String stateValue, String stateLevel) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.CHANNEL);
        state.setScopeKey("COM1");
        state.setPackNum(1);
        state.setChannelName("COM1");
        state.setStateCode(stateCode);
        state.setStateValue(stateValue);
        state.setStateLevel(stateLevel);
        return state;
    }

    private BatteryDeviceState moduleTimeout(Integer packNum, String stateValue, String stateLevel) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.MODULE);
        state.setScopeKey("COM1:8");
        state.setPackNum(packNum);
        state.setChannelName("COM1");
        state.setModelNum(8);
        state.setStateCode(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        state.setStateValue(stateValue);
        state.setStateLevel(stateLevel);
        return state;
    }

    private BatteryDeviceState moduleActive(Integer packNum, String stateValue) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.MODULE);
        state.setScopeKey("COM1:8");
        state.setPackNum(packNum);
        state.setChannelName("COM1");
        state.setModelNum(8);
        state.setStateCode(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        state.setStateValue(stateValue);
        state.setStateLevel("inactive".equals(stateValue)
                ? BatteryDeviceStateConstants.StateLevel.WARN
                : BatteryDeviceStateConstants.StateLevel.NORMAL);
        return state;
    }

    private static class StubBatteryDeviceStateService implements BatteryDeviceStateService {

        private final String group246Freshness;
        private final List<BatteryDeviceState> channelStates;

        private StubBatteryDeviceStateService(String group246Freshness, List<BatteryDeviceState> channelStates) {
            this.group246Freshness = group246Freshness;
            this.channelStates = channelStates;
        }

        @Override
        public void upsert(BatteryDeviceState state) {
        }

        @Override
        public BatteryDeviceState selectByScope(String scopeType, String scopeKey, String stateCode) {
            if (BatteryDeviceStateConstants.ScopeType.PACK.equals(scopeType)
                    && "1".equals(scopeKey)
                    && BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS.equals(stateCode)
                    && group246Freshness != null) {
                BatteryDeviceState state = new BatteryDeviceState();
                state.setScopeType(scopeType);
                state.setScopeKey(scopeKey);
                state.setPackNum(1);
                state.setStateCode(stateCode);
                state.setStateValue(group246Freshness);
                return state;
            }
            for (BatteryDeviceState state : channelStates) {
                if (state != null
                        && scopeType.equals(state.getScopeType())
                        && scopeKey.equals(state.getScopeKey())
                        && stateCode.equals(state.getStateCode())) {
                    return state;
                }
            }
            return null;
        }

        @Override
        public List<BatteryDeviceState> selectByPackAndCode(Integer packNum, String stateCode) {
            return Collections.emptyList();
        }

        @Override
        public List<BatteryDeviceState> selectByChannelAndCode(String channelName, String stateCode) {
            if (channelStates == null || channelStates.isEmpty()) {
                return Collections.emptyList();
            }
            List<BatteryDeviceState> result = new java.util.ArrayList<>();
            for (BatteryDeviceState state : channelStates) {
                if (state != null
                        && channelName.equals(state.getChannelName())
                        && stateCode.equals(state.getStateCode())) {
                    result.add(state);
                }
            }
            return result;
        }

        @Override
        public List<BatteryDeviceState> selectByPackNum(Integer packNum) {
            return Collections.emptyList();
        }

        @Override
        public List<BatteryDeviceState> selectList(BatteryDeviceState state) {
            return Collections.emptyList();
        }

        @Override
        public void deleteByStateId(Long stateId) {
        }

        @Override
        public void deleteByScope(String scopeType, String scopeKey) {
        }

        @Override
        public int deleteExpired() {
            return 0;
        }

        @Override
        public void deleteByPackNum(Integer packNum) {
        }

        @Override
        public void deleteAll() {
        }

        @Override
        public List<BatteryDeviceState> getPackStatusSummary(Integer packNum) {
            return Collections.emptyList();
        }

        @Override
        public List<BatteryDeviceState> getChannelStatusSummary(String channelName) {
            return Collections.emptyList();
        }
    }
}
