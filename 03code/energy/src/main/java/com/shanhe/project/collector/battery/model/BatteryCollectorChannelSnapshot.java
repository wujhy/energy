package com.shanhe.project.collector.battery.model;

import lombok.Data;

/**
 * Read-only collector channel state for commissioning and diagnostics.
 */
@Data
public class BatteryCollectorChannelSnapshot {

    private String name;

    private String portName;

    private Integer batteryGroup;

    private Integer deviceAddress;

    private Boolean opened;

    private BatteryCollectorRunState runState;

    private Long lastReceiveTime;

    private Long lastSendTime;

    private Long lastPollTime;

    private Long lastTimeoutTime;

    private Integer timeoutCount;

    private Integer currentRetryCount;

    private Integer lastRequestCode;

    private Integer expectedResponseCode;

    private Integer lastResponseCode;

    private Long lastPendingCompletedAt;

    private Boolean lastPendingTimedOut;

    private String currentPollBatchNo;

    private Long currentPollStartedAt;

    private Integer currentPollAddress;

    private Long pollRoundCount;

    private Boolean currentFullDiscovery;

    private Long lastFullDiscoveryTime;

    private Integer activeModuleAddressCount;

    private String activeModuleAddresses;

    private String pendingCommandName;

    private Integer pendingRequestCode;

    private Integer pendingResponseCode;

    private Boolean pendingAutoPoll;

    private Integer receiveBufferSize;
}
