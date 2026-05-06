package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.enums.ItemCode;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleDataType;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameData;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModulePollContext;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

class BatteryModuleRealtimeConsumerTest {

    private final BatteryModuleRealtimeConsumer consumer = new BatteryModuleRealtimeConsumer();

    @Test
    void shouldBuildCellRealtime() {
        ReflectionTestUtils.setField(consumer, "compatibilityFillService", new BatteryModuleCellCompatibilityFillService());
        BatteryCollectorChannelConfig channelConfig = channelConfig();
        Date pollStartedAt = new Date(1000L);
        BatteryModulePollContextHolder.set(com.shanhe.project.collector.battery.model.BatteryModulePollContext.builder()
                .pollBatchNo("batch-1")
                .pollStartedAt(pollStartedAt)
                .build());
        BatteryModuleFrameData data = BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.SINGLE_MODULE_INFO)
                .moduleAddress(8)
                .success(true)
                .responseFlag(0)
                .cellVoltage(2.5d)
                .internalResistance(120)
                .cellTemperature(24.5d)
                .leakageStatus(0)
                .swollenVoltage(123.4d)
                .build();

        BatteryModuleCellRealtime realtime;
        try {
            realtime = consumer.buildCell(channelConfig, data);
        } finally {
            BatteryModulePollContextHolder.clear();
        }

        Assertions.assertEquals(1, realtime.getPackNum());
        Assertions.assertEquals(8, realtime.getBatNum());
        Assertions.assertEquals(2.5d, realtime.getVoltage(), 0.0001d);
        Assertions.assertEquals(120, realtime.getResistance());
        Assertions.assertEquals(24.5d, realtime.getTemperature(), 0.0001d);
        Assertions.assertEquals(123.4d, realtime.getSwollenVoltage(), 0.0001d);
        Assertions.assertEquals(0, realtime.getLeakageStatus());
        Assertions.assertEquals("batch-1", realtime.getPollBatchNo());
        Assertions.assertEquals(pollStartedAt, realtime.getPollStartedAt());
    }

    @Test
    void shouldBuildGroupRealtime() {
        BatteryCollectorChannelConfig channelConfig = channelConfig();
        BatteryModuleFrameData data = BatteryModuleFrameData.builder()
                .type(BatteryModuleDataType.ARRAY_MODULE_INFO)
                .moduleAddress(246)
                .success(true)
                .responseFlag(0)
                .chargeDischargeCurrent(12.3d)
                .floatCurrent(0.123d)
                .externalVoltage(123.45d)
                .environmentTemperature1(25.1d)
                .environmentTemperature2(25.2d)
                .build();

        BatteryModuleGroupRealtime realtime = consumer.buildGroup(channelConfig, data);

        Assertions.assertEquals(1, realtime.getPackNum());
        Assertions.assertEquals(12.3d, realtime.getChargeDischargeCurrent(), 0.0001d);
        Assertions.assertEquals(0.123d, realtime.getFloatCurrent(), 0.0001d);
        Assertions.assertEquals(123.45d, realtime.getExternalVoltage(), 0.0001d);
        Assertions.assertEquals(12.3d, realtime.getPackCurrent(), 0.0001d);
        Assertions.assertEquals(0.123d, realtime.getBatteryPackFloatCurrent(), 0.0001d);
        Assertions.assertEquals(123.45d, realtime.getBatteryPackOuterVoltage(), 0.0001d);
        Assertions.assertTrue(realtime.getGroupModuleFresh());
    }

    @Test
    void shouldResolveDefaultCalculationStaleThreshold() {
        BatteryCollectorProperties properties = new BatteryCollectorProperties();
        properties.setGroupCalculationStaleThresholdMs(0L);
        ReflectionTestUtils.setField(consumer, "properties", properties);

        Assertions.assertEquals(180_000L, consumer.resolveCalculationStaleThresholdMs());
    }

    @Test
    void shouldOnlyCalculateAfterGroupRealtimeSaved() {
        Assertions.assertFalse(consumer.shouldCalculateAfterSave(BatteryModuleDataType.SINGLE_MODULE_INFO));
        Assertions.assertTrue(consumer.shouldCalculateAfterSave(BatteryModuleDataType.ARRAY_MODULE_INFO));
    }

    @Test
    void shouldKeepAlarmContextInPollBatch() {
        ReflectionTestUtils.setField(consumer, "alarmAdaptService", new BatteryModuleAlarmAdaptService());
        BatteryModulePollContext context = BatteryModulePollContext.builder().build();
        context.getCells().add(cell(8, 1));
        BatteryModuleGroupRealtime group = new BatteryModuleGroupRealtime();
        group.setPackNum(1);

        consumer.adaptAlarmContext(channelConfig(), context, group);

        Assertions.assertNotNull(context.getAlarmContext());
        Assertions.assertEquals(1, context.getAlarmContext().getPackNum());
        Assertions.assertEquals("1", context.getAlarmContext()
                .getCellWarnParam()
                .get(8)
                .get(ItemCode.DTLYGJ.getCode()));
    }

    private BatteryCollectorChannelConfig channelConfig() {
        BatteryCollectorChannelConfig channelConfig = new BatteryCollectorChannelConfig();
        channelConfig.setName("battery-group-1");
        channelConfig.setPortName("ttyS9");
        channelConfig.setBatteryGroup(1);
        return channelConfig;
    }

    private BatteryModuleCellRealtime cell(int batNum, Integer leakageStatus) {
        BatteryModuleCellRealtime cell = new BatteryModuleCellRealtime();
        cell.setBatNum(batNum);
        cell.setLeakageStatus(leakageStatus);
        return cell;
    }
}
