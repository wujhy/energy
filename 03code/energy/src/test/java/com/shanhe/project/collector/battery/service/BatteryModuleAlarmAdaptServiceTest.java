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
    void shouldMapActiveModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout("01/81", BatteryDeviceStateConstants.StateLevel.WARN)));

        BatteryModuleAlarmContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapRecoveredModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryModuleAlarmAdaptService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout("recovered", BatteryDeviceStateConstants.StateLevel.NORMAL)));

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

    private BatteryDeviceState moduleTimeout(String stateValue, String stateLevel) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setStateCode(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        state.setStateValue(stateValue);
        state.setStateLevel(stateLevel);
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
            if (group246Freshness == null) {
                return null;
            }
            if (BatteryDeviceStateConstants.ScopeType.PACK.equals(scopeType)
                    && "1".equals(scopeKey)
                    && BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS.equals(stateCode)) {
                BatteryDeviceState state = new BatteryDeviceState();
                state.setStateValue(group246Freshness);
                return state;
            }
            return null;
        }

        @Override
        public List<BatteryDeviceState> selectByPackAndCode(Integer packNum, String stateCode) {
            return Collections.emptyList();
        }

        @Override
        public List<BatteryDeviceState> selectByChannelAndCode(String channelName, String stateCode) {
            return channelStates;
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
