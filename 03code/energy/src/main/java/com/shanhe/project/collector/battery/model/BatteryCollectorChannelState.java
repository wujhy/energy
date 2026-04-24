package com.shanhe.project.collector.battery.model;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.project.collector.battery.protocol.BatteryPollingCommand;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 采集通道运行态。
 */
@Data
public class BatteryCollectorChannelState {

    private final BatteryCollectorChannelConfig config;

    private SerialPort serialPort;

    private volatile long lastReceiveTime;

    private volatile long lastSendTime;

    private volatile long lastPollTime;

    private volatile long lastTimeoutTime;

    private volatile int timeoutCount;

    private volatile int currentRetryCount;

    private volatile BatteryPollingCommand pendingCommand;

    private volatile int lastRequestCode;

    private volatile int expectedResponseCode;

    private final ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

    private final AtomicBoolean opened = new AtomicBoolean(false);
}
