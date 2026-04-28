package com.shanhe.project.collector.battery.model;

import lombok.Builder;
import lombok.Data;
import com.shanhe.project.collector.battery.protocol.BatteryAggregateCommandDefinition;

@Data
@Builder
public class BatteryCollectorCommandResult {

    private BatteryAggregateCommandDefinition commandDefinition;

    private boolean success;

    private boolean timeout;

    private String channelName;

    private Integer requestCode;

    private Integer responseCode;

    private Integer actualResponseCode;

    private String message;
}
