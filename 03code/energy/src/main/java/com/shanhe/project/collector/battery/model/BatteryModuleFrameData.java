package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;

/**
 * Standard parsed data from a 600-cell module frame.
 */
@Data
@Builder
public class BatteryModuleFrameData {

    private BatteryModuleDataType type;

    private int moduleAddress;

    private boolean success;

    private int responseFlag;

    private String protocolCode;

    private Integer statusCode;

    private Integer assignedModuleAddress;

    private Integer autoSetAddressStep;

    private Double cellVoltage;

    private Integer internalResistance;

    private Double cellTemperature;

    private Integer leakageStatus;

    private Double swollenVoltage;

    private Double chargeDischargeCurrent;

    private Double floatCurrent;

    private Double externalVoltage;

    private Double environmentTemperature1;

    private Double environmentTemperature2;

    private Double connectBatteryVoltage;

    private Double connectTestVoltage;
}
