package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.BatteryTestEnum;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryDeviceStateConstants;
import com.shanhe.project.collector.battery.model.BatteryModuleControlCommand;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.mapper.OptLogMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class BatteryCollectorCommandLogServiceTest {

    private final BatteryCollectorCommandLogService service = new BatteryCollectorCommandLogService();

    @Test
    void shouldCreateCommandOptLogWithModuleCommandFields() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        BatteryCollectorChannelConfig config = new BatteryCollectorChannelConfig();
        config.setName("battery-group-1");
        config.setConfigId(1L);
        BatteryModuleControlCommand command = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .description("single-test")
                .batteryGroup(2)
                .address(8)
                .mode(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE)
                .optLogType(BatteryTestEnum._2.getDictValue())
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[]{0x01, 0x23})
                .build();

        Long optLogId = service.createCommandOptLog(config, command);

        Assertions.assertNotNull(optLogId);
        ArgumentCaptor<OptLog> captor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(captor.capture());
        OptLog optLog = captor.getValue();
        Assertions.assertEquals(optLogId, optLog.getId());
        Assertions.assertEquals(1L, optLog.getConfigId());
        Assertions.assertEquals(2, optLog.getPackNum());
        Assertions.assertEquals(BatteryTestEnum._2.getDictValue(), optLog.getType());
        Assertions.assertEquals("single-test", optLog.getContent());
        Assertions.assertEquals(BatteryDeviceStateConstants.Source.COLLECTOR, optLog.getSource());
        Assertions.assertEquals("battery-group-1", optLog.getChannelName());
        Assertions.assertEquals("module", optLog.getTargetType());
        Assertions.assertEquals(8, optLog.getTargetAddress());
        Assertions.assertEquals(BatteryModeStatusService.MODE_INTERNAL_RESISTANCE, optLog.getMode());
        Assertions.assertEquals(BatteryDeviceStateConstants.CommandStatus.PENDING, optLog.getStatus());
        Assertions.assertEquals(0x02, optLog.getRequestCode());
        Assertions.assertEquals(0x82, optLog.getResponseCode());
        Assertions.assertEquals("SINGLE_BATTERY_IR_TEST", optLog.getProtocolCode());
        Assertions.assertEquals(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST.getDescription(), optLog.getCommandName());
        Assertions.assertEquals("0123", optLog.getRequestPayload());
        Assertions.assertNotNull(optLog.getStartedAt());
    }

    @Test
    void shouldUseNullRequestPayloadForEmptyPayload() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);
        BatteryModuleControlCommand command = BatteryModuleControlCommand.builder()
                .protocolCode(BatteryDeviceProtocolCode.SINGLE_BATTERY_IR_TEST)
                .address(8)
                .requestCode(0x02)
                .responseCode(0x82)
                .payload(new byte[0])
                .build();

        service.createCommandOptLog(null, command);

        ArgumentCaptor<OptLog> captor = ArgumentCaptor.forClass(OptLog.class);
        Mockito.verify(optLogMapper).insert(captor.capture());
        Assertions.assertNull(captor.getValue().getRequestPayload());
    }

    @Test
    void shouldUpdateTimeoutCommandOptLogWithErrorMessage() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.TIMEOUT, null, null);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.TIMEOUT),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.eq("命令响应超时"),
                Mockito.isNull());
    }

    @Test
    void shouldUpdateSuccessCommandOptLogWithResultZero() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.SUCCESS, 0x82, "0001");

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.SUCCESS),
                Mockito.eq(0),
                Mockito.eq(0x82),
                Mockito.anyString(),
                Mockito.isNull(),
                Mockito.eq("0001"));
    }

    @Test
    void shouldUpdatePendingCommandOptLogWithResultNull() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.PENDING, null, null);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.PENDING),
                Mockito.isNull(),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.isNull(),
                Mockito.isNull());
    }

    @Test
    void shouldSkipUpdateWhenOptLogIdIsNull() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(null, BatteryDeviceStateConstants.CommandStatus.SUCCESS, 0x82, "0001");

        Mockito.verifyNoInteractions(optLogMapper);
    }

    @Test
    void shouldUpdateFailedCommandOptLogWithResultOne() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.FAILED, 0x82, "0001");

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.FAILED),
                Mockito.eq(1),
                Mockito.eq(0x82),
                Mockito.anyString(),
                Mockito.eq("命令响应失败, responseCode=130, payload=0001"),
                Mockito.eq("0001"));
    }

    @Test
    void shouldUpdateRejectedCommandOptLogWithResultOne() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.REJECTED, null, null);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.REJECTED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.eq("命令队列拒绝"),
                Mockito.isNull());
    }

    @Test
    void shouldUpdateCancelledCommandOptLogWithResultOne() {
        OptLogMapper optLogMapper = Mockito.mock(OptLogMapper.class);
        ReflectionTestUtils.setField(service, "optLogMapper", optLogMapper);

        service.updateCommandOptLog(10L, BatteryDeviceStateConstants.CommandStatus.CANCELLED, null, null);

        Mockito.verify(optLogMapper).updateCommandStatus(
                Mockito.eq(10L),
                Mockito.eq(BatteryDeviceStateConstants.CommandStatus.CANCELLED),
                Mockito.eq(1),
                Mockito.isNull(),
                Mockito.anyString(),
                Mockito.eq("命令已取消"),
                Mockito.isNull());
    }
}
