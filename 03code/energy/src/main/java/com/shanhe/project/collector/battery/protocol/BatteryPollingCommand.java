package com.shanhe.project.collector.battery.protocol;

import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 下行采集轮询命令。
 */
@Getter
public enum BatteryPollingCommand {

    BATTERY_REALTIME(0x02, 0x82, true, null),
    BATTERY_GROUP_CONFIG(0x06, 0x86, false, null),
    BATTERY_WARN(0x07, 0x87, true, null),
    BATTERY_PARAMS_GENERAL(0x0B, 0x8B, true, 1),
    BATTERY_PARAMS_EXCEPTION(0x0B, 0x8B, true, 2),
    BATTERY_PARAMS_CRITICAL(0x0B, 0x8B, true, 3),
    DEVICE_FAULT(0x0D, 0x8D, true, null),
    DEVICE_VERSION(0x0E, 0x8E, false, null);

    private final int requestCode;

    private final int responseCode;

    private final boolean groupRequired;

    private final Integer alarmLevel;

    BatteryPollingCommand(int requestCode, int responseCode, boolean groupRequired, Integer alarmLevel) {
        this.requestCode = requestCode;
        this.responseCode = responseCode;
        this.groupRequired = groupRequired;
        this.alarmLevel = alarmLevel;
    }

    public byte[] buildPayload(BatteryCollectorChannelConfig channelConfig) {
        if (!groupRequired && alarmLevel == null) {
            return new byte[0];
        }
        int group = channelConfig.getBatteryGroup() == null ? 0 : channelConfig.getBatteryGroup();
        if (alarmLevel == null) {
            return new byte[]{(byte) (group & 0xFF)};
        }
        return new byte[]{
                (byte) (group & 0xFF),
                (byte) (alarmLevel & 0xFF)
        };
    }

    public static List<BatteryPollingCommand> defaults() {
        return Arrays.asList(values());
    }
}
