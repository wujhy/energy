package com.shanhe.project.collector.battery.model;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 采集通道运行态。
 */
@Data
public class BatteryCollectorChannelState {

    private final BatteryCollectorChannelConfig config;

    private volatile BatteryCollectorRunState runState = BatteryCollectorRunState.READ;

    private SerialPort serialPort;

    private volatile long lastReceiveTime;

    private volatile long lastSendTime;

    private volatile long lastPollTime;

    private volatile long lastTimeoutTime;

    private volatile int timeoutCount;

    private volatile int currentRetryCount;

    private volatile BatteryPendingRequest pendingCommand;

    private volatile int lastRequestCode;

    private volatile int expectedResponseCode;

    private volatile int lastResponseCode;

    private volatile long lastPendingCompletedAt;

    private volatile boolean lastPendingTimedOut;

    private volatile String currentPollBatchNo;

    private volatile long currentPollStartedAt;

    private volatile int currentPollAddress;

    private volatile long pollRoundCount;

    private volatile boolean currentFullDiscovery;

    private volatile long lastFullDiscoveryTime;

    private final Set<Integer> activeModuleAddresses = ConcurrentHashMap.newKeySet();

    private final ConcurrentMap<Integer, Integer> moduleAddressMissCounts = new ConcurrentHashMap<>();

    private final AtomicBoolean fullDiscoveryRequested = new AtomicBoolean(true);

    private final ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

    private final AtomicBoolean opened = new AtomicBoolean(false);
}
