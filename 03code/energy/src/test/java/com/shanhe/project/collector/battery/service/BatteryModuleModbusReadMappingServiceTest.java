package com.shanhe.project.collector.battery.service;

import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.mapper.BatteryModuleRealtimeMapper;
import com.shanhe.project.collector.battery.model.BatteryDeviceState;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

/**
 * BatteryDeviceStateService 在单测中传 null，状态寄存器测试需要单独 mock。
 */

class BatteryModuleModbusReadMappingServiceTest {

    @Test
    void shouldMapCellReferenceRegisters() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.123d, 101, 25.1d, 3.4d),
                cell(2, 2.456d, 102, -5.0d, null)
        ));

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{2123, 2456}, service.readHoldingRegisters(1, 410004, 2));
        Assertions.assertArrayEquals(new int[]{101, 102}, service.readHoldingRegisters(1, 410252, 2));
        Assertions.assertArrayEquals(new int[]{751, 450}, service.readHoldingRegisters(1, 410500, 2));
        Assertions.assertArrayEquals(new int[]{34, 0}, service.readHoldingRegisters(1, 410748, 2));
    }

    @Test
    void shouldMapGroupReferenceRegistersWithFallbacksAndOffsets() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(123.4d);
        group.setChargeDischargeCurrent(-12.3d);
        group.setFloatCurrent(0.123d);
        group.setEnvironmentTemperature1(25.1d);
        group.setEnvironmentTemperature2(null);
        group.setMaxVoltageBatNum(2);
        group.setMaxCellVoltage(2.345d);
        group.setMinVoltageBatNum(1);
        group.setMinCellVoltage(2.111d);
        group.setAvgCellVoltage(2.222d);
        group.setVoltageRange(0.234d);
        group.setMaxResistanceBatNum(3);
        group.setMaxInternalResistance(120);
        group.setMinResistanceBatNum(1);
        group.setMinInternalResistance(90);
        group.setAvgInternalResistance(105.6d);
        group.setMaxTemperatureBatNum(4);
        group.setMaxCellTemperature(30.2d);
        group.setMinTemperatureBatNum(1);
        group.setMinCellTemperature(20.0d);
        group.setAvgCellTemperature(25.6d);
        group.setBatteryPackSoc(80.5d);
        group.setBatteryPackSoh(99.4d);
        group.setBatteryPackStatus(5);
        group.setResistanceTestStatus(2);
        group.setBackupDuration(120);
        group.setCapacity(100.5d);
        group.setDisChargeDuration(60);
        group.setDisChargeCapacity(18.6d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{1234, 29877, 10123, 751, 0},
                service.readHoldingRegisters(1, 411729, 5));
        Assertions.assertArrayEquals(new int[]{2, 2345, 1, 2111, 2222, 0, 234},
                service.readHoldingRegisters(1, 411734, 7));
        Assertions.assertArrayEquals(new int[]{3, 120, 1, 90, 106, 4, 802, 1, 700, 756, 805, 994},
                service.readHoldingRegisters(1, 411741, 12));
        Assertions.assertArrayEquals(new int[]{0x0502, 120, 1005, 60, 186},
                service.readHoldingRegisters(1, 411762, 5));
    }

    @Test
    void shouldMaskM460BatteryStateRegisterFields() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setBatteryPackStatus(0x35);
        group.setResistanceTestStatus(0x102);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{0x0502}, service.readHoldingRegisters(1, 411762, 1));
    }

    @Test
    void shouldDefaultM460BatteryStateRegisterFieldsToZero() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectGroup(1)).thenReturn(new BatteryModuleGroupRealtime());

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411762, 1));
    }

    @Test
    void shouldRejectUnsupportedAddressAndInvalidQuantity() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.0d, 100, 25.0d, null)));
        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 411753, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, 126));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(null, 410004, 1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, 0));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410004, -1));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 410248, 2));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.readHoldingRegisters(1, 411489, 1));
    }

    @Test
    void shouldAllowMaxReadQuantityWhenAddressRangeIsSupported() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.0d, 100, 25.0d, null)));
        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        int[] values = service.readHoldingRegisters(1, 410004, 125);

        Assertions.assertEquals(125, values.length);
        Assertions.assertEquals(2000, values[0]);
        Assertions.assertEquals(0, values[124]);
    }

    @Test
    void shouldThrowWhenDataNotReady() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(null);
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 410004, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411729, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411487, 1));
    }

    @Test
    void shouldThrowWhenCellsAreEmptyAndGroupMissing() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList());
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 410004, 1));
    }

    @Test
    void shouldReturnZeroForMissingCellsWhenGroupExists() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(null);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(48.0d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        // 单体数据为空时返回 0，组数据正常读取
        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 410004, 1));
        Assertions.assertArrayEquals(new int[]{480}, service.readHoldingRegisters(1, 411729, 1));
    }

    @Test
    void shouldReturnZeroForMissingGroupWhenCellsExist() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.0d, 100, 25.0d, null)));
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411729, 1));
    }

    @Test
    void shouldThrowForCapacityStateRegistersWhenGroupMissing() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(cell(1, 2.0d, 100, 25.0d, null)));
        Mockito.when(mapper.selectGroup(1)).thenReturn(null);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411751, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411763, 1));
    }

    @Test
    void shouldThrowForMissingCapacityStateValues() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectGroup(1)).thenReturn(new BatteryModuleGroupRealtime());

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411751, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411752, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411763, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411764, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411765, 1));
        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411766, 1));
    }

    @Test
    void shouldReturnOneForFreshGroup246Freshness() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1))
                .thenReturn(Collections.singletonList(packState(
                        BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "fresh", null, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, null);

        Assertions.assertArrayEquals(new int[]{1}, service.readHoldingRegisters(1, 411487, 1));
        Mockito.verify(stateService, Mockito.times(1)).selectByPackNum(1);
    }

    @Test
    void shouldReturnZeroForStaleGroup246Freshness() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1))
                .thenReturn(Collections.singletonList(packState(
                        BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "stale", null, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, null);

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411487, 1));
    }

    @Test
    void shouldReturnZeroWhenGroup246FreshnessMissing() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Collections.emptyList());

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, null);

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411487, 1));
    }

    @Test
    void shouldReturnZeroForGroup246FreshnessWhenStateServiceMissing() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryModuleModbusReadMappingService service =
                service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411487, 1));
    }

    @Test
    void shouldReturnZeroForDeviceStateRegistersWhenStateServiceMissing() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryModuleModbusReadMappingService service =
                service(mapper, null, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0},
                service.readHoldingRegisters(1, 411483, 6));
    }

    @Test
    void shouldMapDeviceStateRegistersFrom411483To411488() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Arrays.asList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, "open", null, null),
                channelState("COM1", BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                        "error", BatteryDeviceStateConstants.StateLevel.ERROR, null),
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "active", null, null),
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT, "01/81", null, null),
                packState(BatteryDeviceStateConstants.StateCode.GROUP_246_FRESHNESS, "fresh", null, null),
                packState(BatteryDeviceStateConstants.StateCode.WORK_MODE, "mode", null, 6)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{1, 1, 1, 1, 1, 6},
                service.readHoldingRegisters(1, 411483, 6));
        Mockito.verify(stateService, Mockito.times(1)).selectByPackNum(1);
        Mockito.verify(stateService, Mockito.never()).selectByScope(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(stateService, Mockito.never()).selectByChannelAndCode(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void shouldIgnoreOtherPackAndRecoveredModuleStatusRegisters() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Arrays.asList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "inactive", null, null),
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT,
                        "recovered", BatteryDeviceStateConstants.StateLevel.NORMAL, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0, 0},
                service.readHoldingRegisters(1, 411485, 2));
    }

    @Test
    void shouldReturnZeroForRecoveredTimeoutEvenWhenLevelIsWarn() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Collections.singletonList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_TIMEOUT,
                        "recovered", BatteryDeviceStateConstants.StateLevel.WARN, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411486, 1));
    }

    @Test
    void shouldUseOnlyEnabledChannelMatchedByPackNumForChannelScopedStatus() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Arrays.asList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, "open", null, null),
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "active", null, null),
                channelState("COM_DISABLED", BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, "open", null, null),
                channelState("COM_OTHER", BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "active", null, null)));
        BatteryCollectorProperties properties = properties("COM1", 1);
        BatteryCollectorChannelConfig disabledSamePack = new BatteryCollectorChannelConfig();
        disabledSamePack.setName("COM_DISABLED");
        disabledSamePack.setEnabled(Boolean.FALSE);
        disabledSamePack.setBatteryGroup(1);
        BatteryCollectorChannelConfig otherPack = new BatteryCollectorChannelConfig();
        otherPack.setName("COM_OTHER");
        otherPack.setEnabled(Boolean.TRUE);
        otherPack.setBatteryGroup(2);
        properties.setChannels(Arrays.asList(disabledSamePack, otherPack, properties.getChannels().get(0)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties);

        Assertions.assertArrayEquals(new int[]{1, 0, 1}, service.readHoldingRegisters(1, 411483, 3));
        Mockito.verify(stateService, Mockito.times(1)).selectByPackNum(1);
    }

    @Test
    void shouldReturnZeroForChannelScopedStatusWhenPackHasNoEnabledChannel() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM2", 2));

        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0}, service.readHoldingRegisters(1, 411483, 4));
        Mockito.verify(stateService, Mockito.never()).selectByScope(
                Mockito.eq(BatteryDeviceStateConstants.ScopeType.CHANNEL),
                Mockito.anyString(),
                Mockito.anyString());
        Mockito.verify(stateService, Mockito.never()).selectByChannelAndCode(
                Mockito.anyString(),
                Mockito.anyString());
    }

    @Test
    void shouldReturnZeroForMissingDeviceStateRegisters() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Collections.singletonList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.MODULE_ACTIVE, "inactive", null, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0},
                service.readHoldingRegisters(1, 411483, 6));
    }

    @Test
    void shouldReturnZeroForClosedChannelOpenRegister() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Collections.singletonList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.CHANNEL_OPEN, "closed", null, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411483, 1));
    }

    @Test
    void shouldReturnZeroForNonErrorChannelErrorRegister() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1)).thenReturn(Collections.singletonList(
                channelState("COM1", BatteryDeviceStateConstants.StateCode.CHANNEL_ERROR,
                        "normal", BatteryDeviceStateConstants.StateLevel.NORMAL, null)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411484, 1));
    }

    @Test
    void shouldClampWorkModeRegisterToUnsigned16() {
        BatteryModuleRealtimeMapper mapper = mapperWithReadyGroup(1);
        BatteryDeviceStateService stateService = Mockito.mock(BatteryDeviceStateService.class);
        Mockito.when(stateService.selectByPackNum(1))
                .thenReturn(Collections.singletonList(packState(
                        BatteryDeviceStateConstants.StateCode.WORK_MODE, "mode", null, -1)))
                .thenReturn(Collections.singletonList(packState(
                        BatteryDeviceStateConstants.StateCode.WORK_MODE, "mode", null, 70000)));

        BatteryModuleModbusReadMappingService service =
                service(mapper, stateService, properties("COM1", 1));

        Assertions.assertArrayEquals(new int[]{0}, service.readHoldingRegisters(1, 411488, 1));
        Assertions.assertArrayEquals(new int[]{65535}, service.readHoldingRegisters(1, 411488, 1));
    }

    @Test
    void shouldHandleBoundaryCellAddresses() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.0d, 100, 25.0d, null),
                cell(245, 2.5d, 200, 30.0d, 5.0d)
        ));

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        // 单体 1 和 245 的电压
        Assertions.assertArrayEquals(new int[]{2000}, service.readHoldingRegisters(1, 410004, 1));
        Assertions.assertArrayEquals(new int[]{2500}, service.readHoldingRegisters(1, 410248, 1));
        // 单体 1 和 245 的内阻
        Assertions.assertArrayEquals(new int[]{100}, service.readHoldingRegisters(1, 410252, 1));
        Assertions.assertArrayEquals(new int[]{200}, service.readHoldingRegisters(1, 410496, 1));
        // 单体 1 和 245 的温度 (25+50)*10=750, (30+50)*10=800
        Assertions.assertArrayEquals(new int[]{750}, service.readHoldingRegisters(1, 410500, 1));
        Assertions.assertArrayEquals(new int[]{800}, service.readHoldingRegisters(1, 410744, 1));
    }

    @Test
    void shouldLoadMapperSnapshotOnlyOncePerReadRequest() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(1)).thenReturn(Arrays.asList(
                cell(1, 2.0d, 100, 25.0d, null),
                cell(2, 2.1d, 110, 26.0d, null)
        ));
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(48.0d);
        Mockito.when(mapper.selectGroup(1)).thenReturn(group);

        BatteryModuleModbusReadMappingService service = service(mapper, null, null);

        Assertions.assertArrayEquals(new int[]{2000, 2100, 0}, service.readHoldingRegisters(1, 410004, 3));

        Mockito.verify(mapper, Mockito.times(1)).selectCells(1);
        Mockito.verify(mapper, Mockito.times(1)).selectGroup(1);
    }

    @Test
    void shouldUseRealtimeSnapshotServiceWithoutMapperQueries() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setExternalVoltage(48.0d);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .cells(Collections.singletonList(cell(1, 2.0d, 100, 25.0d, null)))
                .group(group)
                .refreshedAt(new Date())
                .build());
        BatteryModuleModbusReadMappingService service =
                new BatteryModuleModbusReadMappingService(null, null, snapshotService);

        Assertions.assertArrayEquals(new int[]{2000, 0, 0},
                service.readHoldingRegisters(1, 410004, 3));

        Mockito.verify(snapshotService, Mockito.times(1)).getCachedSnapshot(1);
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void shouldNotQueryMapperWhenInjectedSnapshotCacheMisses() {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(null);
        BatteryModuleModbusReadMappingService service =
                new BatteryModuleModbusReadMappingService(null, null, snapshotService);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 410004, 1));

        Mockito.verify(snapshotService, Mockito.times(1)).getCachedSnapshot(1);
        Mockito.verifyNoInteractions(mapper);
    }

    @Test
    void shouldRejectExpiredRealtimeSnapshot() {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group)
                .refreshedAt(new Date(System.currentTimeMillis()
                        - BatteryModuleRealtimeSnapshot.DEFAULT_FRESH_MILLIS - 1))
                .build());
        BatteryModuleModbusReadMappingService service =
                new BatteryModuleModbusReadMappingService(null, null, snapshotService);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411729, 1));
    }

    @Test
    void shouldRejectRealtimeSnapshotMarkedStaleByGroupData() {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setDataFresh(false);
        Mockito.when(snapshotService.getCachedSnapshot(1)).thenReturn(BatteryModuleRealtimeSnapshot.builder()
                .packNum(1)
                .group(group)
                .refreshedAt(new Date())
                .build());
        BatteryModuleModbusReadMappingService service =
                new BatteryModuleModbusReadMappingService(null, null, snapshotService);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.readHoldingRegisters(1, 411729, 1));
    }

    private BatteryModuleModbusReadMappingService service(BatteryModuleRealtimeMapper mapper,
                                                          BatteryDeviceStateService stateService,
                                                          BatteryCollectorProperties properties) {
        return new BatteryModuleModbusReadMappingService(stateService, properties, snapshotService(mapper));
    }

    private BatteryModuleRealtimeSnapshotService snapshotService(BatteryModuleRealtimeMapper mapper) {
        BatteryModuleRealtimeSnapshotService snapshotService = Mockito.mock(BatteryModuleRealtimeSnapshotService.class);
        Mockito.when(snapshotService.getCachedSnapshot(Mockito.anyInt())).thenAnswer(invocation -> {
            Integer packNum = invocation.getArgument(0);
            java.util.List<BatteryModuleCellRealtime> cells = mapper.selectCells(packNum);
            BatteryModuleGroupRealtime group = mapper.selectGroup(packNum);
            if ((cells == null || cells.isEmpty()) && group == null) {
                return null;
            }
            return BatteryModuleRealtimeSnapshot.builder()
                    .packNum(packNum)
                    .cells(cells)
                    .group(group)
                    .refreshedAt(new Date())
                    .build();
        });
        return snapshotService;
    }
    private BatteryModuleCellRealtime cell(int batNum,
                                           double voltage,
                                           int resistance,
                                           double temperature,
                                           Double swollenVoltage) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setVoltage(voltage);
        cell.setResistance(resistance);
        cell.setTemperature(temperature);
        cell.setSwollenVoltage(swollenVoltage);
        return cell;
    }

    private BatteryModuleRealtimeMapper mapperWithReadyGroup(int packNum) {
        BatteryModuleRealtimeMapper mapper = Mockito.mock(BatteryModuleRealtimeMapper.class);
        Mockito.when(mapper.selectCells(packNum)).thenReturn(null);
        Mockito.when(mapper.selectGroup(packNum)).thenReturn(new BatteryModuleGroupRealtime());
        return mapper;
    }

    private BatteryDeviceState deviceState(String stateValue) {
        BatteryDeviceState state = new BatteryDeviceState();
        state.setPackNum(1);
        state.setStateValue(stateValue);
        return state;
    }

    private BatteryDeviceState deviceState(Integer packNum, String stateValue) {
        BatteryDeviceState state = deviceState(stateValue);
        state.setPackNum(packNum);
        return state;
    }

    private BatteryDeviceState deviceState(String stateValue, String stateLevel, Integer mode) {
        BatteryDeviceState state = deviceState(stateValue);
        state.setStateLevel(stateLevel);
        state.setMode(mode);
        return state;
    }

    private BatteryDeviceState deviceState(Integer packNum, String stateValue, String stateLevel, Integer mode) {
        BatteryDeviceState state = deviceState(stateValue, stateLevel, mode);
        state.setPackNum(packNum);
        return state;
    }

    private BatteryDeviceState packState(String stateCode, String stateValue, String stateLevel, Integer mode) {
        BatteryDeviceState state = deviceState(stateValue, stateLevel, mode);
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.PACK);
        state.setScopeKey("1");
        state.setStateCode(stateCode);
        return state;
    }

    private BatteryDeviceState channelState(String channelName, String stateCode,
                                            String stateValue, String stateLevel, Integer mode) {
        BatteryDeviceState state = deviceState(1, stateValue, stateLevel, mode);
        state.setScopeType(BatteryDeviceStateConstants.ScopeType.CHANNEL);
        state.setScopeKey(channelName);
        state.setChannelName(channelName);
        state.setStateCode(stateCode);
        return state;
    }
    private BatteryCollectorProperties properties(String channelName, int packNum) {
        BatteryCollectorChannelConfig channel = new BatteryCollectorChannelConfig();
        channel.setName(channelName);
        channel.setEnabled(Boolean.TRUE);
        channel.setBatteryGroup(packNum);
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setChannels(Collections.singletonList(channel));
        return properties;
    }
}
