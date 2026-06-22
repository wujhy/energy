package com.shanhe.project.collector.battery.runtime;

import com.shanhe.project.collector.battery.command.BatteryCollectorCommandQueueService;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.model.BatteryPendingRequest;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.service.BatteryCollectorProtocolLogService;
import com.shanhe.project.collector.battery.state.BatteryCollectorDeviceStateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 蓄电池采集 pending 超时协调服务。
 *
 * <p>只负责判断是否超时、触发重试、最终超时收尾和状态持久化；实际串口写入由调用方回调完成。</p>
 */
@Slf4j
@Component
public class BatteryCollectorTimeoutService {

    @Resource
    private BatteryCollectorProperties properties;
    @Resource
    private BatteryCollectorProtocolLogService protocolLogService;
    @Resource
    private BatteryCollectorDeviceStateService collectorDeviceStateService;
    @Resource
    private BatteryCollectorCommandQueueService commandQueueService;
    @Resource
    private BatteryCollectorFrameCodec frameCodec;

    /** 重试帧写入回调。 */
    @FunctionalInterface
    public interface RetryFrameWriter {
        /**
         * 写入重试请求帧。
         *
         * @param frame 请求帧
         * @param pendingRequest 待响应请求
         * @param waitingState 写入成功后的等待状态
         * @return 是否写入成功
         */
        boolean write(BatteryCollectorFrame frame,
                      BatteryPendingRequest pendingRequest,
                      BatteryCollectorRunState waitingState);
    }

    /**
     * 检查当前 pending 是否超时，超时后执行重试或最终失败收尾。
     *
     * @param state 通道运行态
     * @param retryFrameWriter 重试写帧回调
     */
    public void checkTimeout(BatteryCollectorChannelState state, RetryFrameWriter retryFrameWriter) {
        if (state.getPendingCommand() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long timeoutMs = resolveResponseTimeoutMs(state.getConfig());
        if (now - state.getLastSendTime() < timeoutMs) {
            return;
        }

        int retryCount = state.getCurrentRetryCount();
        int maxRetryCount = resolveMaxRetryCount(state.getConfig());
        if (retryCount < maxRetryCount) {
            retryPendingRequest(state, retryCount, maxRetryCount, retryFrameWriter);
            return;
        }

        completeTimedOutRequest(state, now);
    }

    /** 当前 pending 未超过最大重试次数时，构造并写入重试帧。 */
    private void retryPendingRequest(BatteryCollectorChannelState state,
                                     int retryCount,
                                     int maxRetryCount,
                                     RetryFrameWriter retryFrameWriter) {
        state.setCurrentRetryCount(retryCount + 1);
        BatteryPendingRequest retryRequest = state.getPendingCommand();
        log.warn("蓄电池采集响应超时，正在重试, 通道={}, 请求={}, 响应={}, 重试={}/{}",
                state.getConfig().getName(),
                String.format("%02X", state.getLastRequestCode()),
                String.format("%02X", state.getExpectedResponseCode()),
                state.getCurrentRetryCount(),
                maxRetryCount);
        protocolLogService.logProtocol(properties, state, "retry",
                "request=" + String.format("%02X", state.getLastRequestCode())
                        + ", expect=" + String.format("%02X", state.getExpectedResponseCode())
                        + ", retry=" + state.getCurrentRetryCount());
        BatteryCollectorFrame request = frameCodec.buildRequest(
                retryRequest.getRequestAddress(),
                retryRequest.getRequestCode(),
                retryRequest.getPayload());
        retryFrameWriter.write(request, retryRequest, retryRequest.isAutoPoll()
                ? BatteryCollectorRunState.WAIT_RESPONSE
                : BatteryCollectorRunState.WAIT_COMMAND_RESPONSE);
        state.getReceiveBuffer().reset();
    }

    /** pending 达到最大重试次数后，记录超时状态并清理等待态。 */
    private void completeTimedOutRequest(BatteryCollectorChannelState state, long now) {
        log.warn("蓄电池采集响应超时, 通道={}, 请求={}, 响应={}, 超时次数={}",
                state.getConfig().getName(),
                String.format("%02X", state.getLastRequestCode()),
                String.format("%02X", state.getExpectedResponseCode()),
                state.getTimeoutCount() + 1);
        BatteryPendingRequest timedOutRequest = state.getPendingCommand();
        collectorDeviceStateService.persistModuleTimeout(state, timedOutRequest);
        commandQueueService.completeTimedOutExplicitCommand(state, timedOutRequest);
        state.setPendingCommand(null);
        state.setExpectedResponseCode(0);
        state.setCurrentRetryCount(0);
        state.setLastPendingCompletedAt(System.currentTimeMillis());
        state.setLastPendingTimedOut(true);
        state.setRunState(BatteryCollectorRunState.READ);
        state.setTimeoutCount(state.getTimeoutCount() + 1);
        state.setLastTimeoutTime(now);
        state.getReceiveBuffer().reset();
        collectorDeviceStateService.persistPollTimeout(state);
    }

    private long resolveResponseTimeoutMs(BatteryCollectorChannelConfig config) {
        Long value = config == null ? null : config.getResponseTimeoutMs();
        return value == null || value <= 0 ? 1500L : value;
    }

    private int resolveMaxRetryCount(BatteryCollectorChannelConfig config) {
        Integer value = config == null ? null : config.getMaxRetryCount();
        return value == null || value < 0 ? 2 : value;
    }
}
