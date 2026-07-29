package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryAlarmEvaluationContext;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class BatteryAlarmStateContextServiceTest {
    @Test
    void shouldMapStaleGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithGroup246Freshness("stale");

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapFreshGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithGroup246Freshness("fresh");

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapMissingGroup246FreshnessToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithGroup246Freshness(null);

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals(1, context.getPackNum());
        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldMapClosedChannelOpenToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(channelState(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN,
                        "closed", BatteryDeviceStateConstants.StateLevel.ERROR)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.DTTXZT.getCode()));
    }

    @Test
    void shouldMapChannelErrorToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(channelState(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                        "read failed", BatteryDeviceStateConstants.StateLevel.ERROR)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.DTTXZT.getCode()));
    }

    @Test
    void shouldEmitRecoveredChannelCommunicationCandidate() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(channelState(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN,
                        "open", BatteryDeviceStateConstants.StateLevel.NORMAL)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.DTTXZT.getCode()));
    }

    @Test
    void shouldMapOfflineOnlineStateToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(packOnlineState("offline", BatteryDeviceStateConstants.StateLevel.WARN)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldEmitRecoveredOnlineStateCandidate() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(packOnlineState("online", BatteryDeviceStateConstants.StateLevel.NORMAL)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldMapActiveModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout(1, "01/81", BatteryDeviceStateConstants.StateLevel.WARN)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapRecoveredModuleTimeoutToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleTimeout(1, "recovered", BatteryDeviceStateConstants.StateLevel.NORMAL)));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldMapInactiveModuleActiveToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleActive(1, "inactive")));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldNotMapActiveModuleActiveToCommunicationAlarm() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Collections.singletonList(moduleActive(1, "active")));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("0", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldIgnoreModuleCommunicationStatesFromOtherPack() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates(null,
                Arrays.asList(
                        moduleTimeout(2, "01/81", BatteryDeviceStateConstants.StateLevel.WARN),
                        moduleActive(2, "inactive")));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertFalse(context.getPackWarnParam().containsKey(ItemCode.TXZT.getCode()));
    }

    @Test
    void shouldIgnoreNullModuleActiveStateAndContinueWithGroup246Freshness() throws Exception {
        BatteryAlarmStateContextService service = communicationServiceWithStates("stale",
                Collections.singletonList(null));

        BatteryAlarmEvaluationContext context = service.buildCommunicationAlarmContext(1, "COM1");

        Assertions.assertEquals("1", context.getPackWarnParam().get(ItemCode.TXZT.getCode()));
    }

    private BatteryAlarmStateContextService communicationServiceWithGroup246Freshness(String stateValue) throws Exception {
        return communicationServiceWithStates(stateValue, Collections.emptyList());
    }

    private BatteryAlarmStateContextService communicationServiceWithStates(String stateValue,
                                                                          List<BatteryDeviceState> channelStates) throws Exception {
        BatteryAlarmStateContextService service = new BatteryAlarmStateContextService();
        Field field = BatteryAlarmStateContextService.class.getDeclaredField("batteryDeviceStateService");
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

    private BatteryDeviceState packOnlineState(String stateValue, String stateLevel) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
        state.setScopeKey("1");
        state.setPackNum(1);
        state.setStateCode(BatteryDeviceStateConstants.StateCode.ONLINE);
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