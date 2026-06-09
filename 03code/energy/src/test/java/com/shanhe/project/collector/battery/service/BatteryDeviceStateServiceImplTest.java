package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.mapper.BatteryDeviceStateMapper;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.service.impl.BatteryDeviceStateServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatteryDeviceStateServiceImplTest {

    @Mock
    private BatteryDeviceStateMapper batteryDeviceStateMapper;

    private BatteryDeviceStateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BatteryDeviceStateServiceImpl();
        ReflectionTestUtils.setField(service, "batteryDeviceStateMapper", batteryDeviceStateMapper);
    }

    @Test
    void getPackStatusSummaryShouldReturnWorkModeOnlineAndGroup246Freshness() {
        Integer packNum = 1;
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.WORK_MODE))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.WORK_MODE));
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.ONLINE))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.ONLINE));
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS));

        List<BatteryDeviceState> summary = service.getPackStatusSummary(packNum);

        Assertions.assertEquals(3, summary.size());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.WORK_MODE, summary.get(0).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.ONLINE, summary.get(1).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, summary.get(2).getStateCode());
        verify(batteryDeviceStateMapper).selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.WORK_MODE);
        verify(batteryDeviceStateMapper).selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.ONLINE);
        verify(batteryDeviceStateMapper).selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "1",
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS);
    }

    @Test
    void getChannelStatusSummaryShouldReturnChannelAndModuleStates() {
        String channelName = "COM1";
        BatteryDeviceState timeout1 = state(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        BatteryDeviceState timeout2 = state(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        BatteryDeviceState active = state(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN));
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR));
        when(batteryDeviceStateMapper.selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT))
                .thenReturn(Arrays.asList(timeout1, timeout2));
        when(batteryDeviceStateMapper.selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE))
                .thenReturn(Arrays.asList(active));

        List<BatteryDeviceState> summary = service.getChannelStatusSummary(channelName);

        Assertions.assertEquals(5, summary.size());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, summary.get(0).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, summary.get(1).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, summary.get(2).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, summary.get(3).getStateCode());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, summary.get(4).getStateCode());
        verify(batteryDeviceStateMapper).selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN);
        verify(batteryDeviceStateMapper).selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR);
        verify(batteryDeviceStateMapper).selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT);
        verify(batteryDeviceStateMapper).selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE);
    }

    @Test
    void getPackStatusSummaryShouldIgnoreMissingStates() {
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "2",
                BatteryDeviceStateConstants.StateCode.WORK_MODE))
                .thenReturn(null);
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "2",
                BatteryDeviceStateConstants.StateCode.ONLINE))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.ONLINE));
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.PACK, "2",
                BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS))
                .thenReturn(null);

        List<BatteryDeviceState> summary = service.getPackStatusSummary(2);

        Assertions.assertEquals(1, summary.size());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.ONLINE, summary.get(0).getStateCode());
    }

    @Test
    void getChannelStatusSummaryShouldIgnoreMissingStates() {
        String channelName = "COM2";
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN))
                .thenReturn(null);
        when(batteryDeviceStateMapper.selectByScope(BatteryDeviceStateConstants.ScopeType.CHANNEL, channelName,
                BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR))
                .thenReturn(state(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR));
        when(batteryDeviceStateMapper.selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT))
                .thenReturn(null);
        when(batteryDeviceStateMapper.selectByChannelAndCode(channelName,
                BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE))
                .thenReturn(null);

        List<BatteryDeviceState> summary = service.getChannelStatusSummary(channelName);

        Assertions.assertEquals(1, summary.size());
        Assertions.assertEquals(BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR, summary.get(0).getStateCode());
    }

    @Test
    void summaryShouldReturnEmptyListWhenInputIsMissing() {
        Assertions.assertTrue(service.getPackStatusSummary(null).isEmpty());
        Assertions.assertTrue(service.getChannelStatusSummary(null).isEmpty());
        Assertions.assertTrue(service.getChannelStatusSummary("  ").isEmpty());
        verifyNoInteractions(batteryDeviceStateMapper);
    }

    private BatteryDeviceState state(String stateCode) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setStateCode(stateCode);
        return state;
    }
}
