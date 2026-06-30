package com.shanhe.project.collector.battery.model;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 采集通道运行态。
 *
 * @author wjh
 * @since 2026-04-28
 */
@Data
public class BatteryCollectorChannelState {

    /** 通道静态配置。 */
    private final BatteryCollectorChannelConfig config;

    /** 当前运行状态。 */
    private volatile BatteryCollectorRunState runState = BatteryCollectorRunState.READ;

    /** 当前串口对象。 */
    private SerialPort serialPort;

    /** 最后接收时间戳。 */
    private volatile long lastReceiveTime;

    /** 最后发送时间戳。 */
    private volatile long lastSendTime;

    /** 最后轮询时间戳。 */
    private volatile long lastPollTime;

    /** 最近一次超时时间戳。 */
    private volatile long lastTimeoutTime;

    /** 累计超时次数。 */
    private volatile int timeoutCount;

    /** 当前重试次数。 */
    private volatile int currentRetryCount;

    /** 当前等待响应的命令。 */
    private volatile BatteryPendingRequest pendingCommand;

    /** 最近一次请求码。 */
    private volatile int lastRequestCode;

    /** 期望的响应码。 */
    private volatile int expectedResponseCode;

    /** 最近一次实际响应码。 */
    private volatile int lastResponseCode;

    /** 最近一次命令完成时间戳。 */
    private volatile long lastPendingCompletedAt;

    /** 最近一次命令是否超时。 */
    private volatile boolean lastPendingTimedOut;

    /** 最近完成的显式模块端命令名称。 */
    private volatile String lastCompletedModuleCommandName;

    /** 最近完成的显式模块端命令响应码。 */
    private volatile int lastCompletedModuleResponseCode;

    /** 最近完成的显式模块端命令是否成功。 */
    private volatile boolean lastCompletedModuleCommandSuccess;

    /** 最近完成的显式模块端命令完成时间戳。 */
    private volatile long lastCompletedModuleCommandTime;

    /** 当前轮询批次号。 */
    private volatile String currentPollBatchNo;

    /** 当前轮询开始时间戳。 */
    private volatile long currentPollStartedAt;

    /** 当前轮询的模块地址。 */
    private volatile int currentPollAddress;

    /** 轮询轮次计数。 */
    private volatile long pollRoundCount;

    /** 当前是否为全地址发现轮询。 */
    private volatile boolean currentFullDiscovery;

    /** 最近一次全量发现时间戳。 */
    private volatile long lastFullDiscoveryTime;

    /** 当前缓存的有响应模块地址。 */
    private final Set<Integer> activeModuleAddresses = ConcurrentHashMap.newKeySet();

    /** 有响应地址的连续无响应次数。 */
    private final ConcurrentMap<Integer, Integer> moduleAddressMissCounts = new ConcurrentHashMap<>();

    /** 下轮是否强制执行全量发现。 */
    private final AtomicBoolean fullDiscoveryRequested = new AtomicBoolean(true);

    /** 等待下发的显式600节模块端控制命令。 */
    private final Queue<BatteryModuleControlCommand> queuedModuleCommands = new ConcurrentLinkedQueue<>();

    /** 串口接收缓冲。 */
    private final ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

    /** 串口打开状态。 */
    private final AtomicBoolean opened = new AtomicBoolean(false);
}
