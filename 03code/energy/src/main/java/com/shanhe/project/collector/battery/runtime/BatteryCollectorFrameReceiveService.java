package com.shanhe.project.collector.battery.runtime;

import com.fazecast.jSerialComm.SerialPort;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelState;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryCollectorRunState;
import com.shanhe.project.collector.battery.protocol.BatteryCollectorFrameCodec;
import com.shanhe.project.collector.battery.service.BatteryCollectorProtocolLogService;
import com.shanhe.project.collector.battery.service.BatteryModuleFrameDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 蓄电池采集串口接收和响应分派服务。
 *
 * <p>负责读取串口可用字节、维护接收缓冲、解码完整帧并分发给实时消费链路。
 * pending 命令完成后的业务副作用仍由主流程回调处理。</p>
 *
 * @author wjh
 * @since 2026-06-22
 */
@Slf4j
@Component
public class BatteryCollectorFrameReceiveService {

    @Resource
    private BatteryCollectorProperties properties;

    @Resource
    private BatteryCollectorFrameIoService frameIoService;

    @Resource
    private BatteryCollectorProtocolLogService protocolLogService;

    @Resource
    private BatteryModuleFrameDispatcher moduleFrameDispatcher;

    /**
     * 读取并处理当前串口已到达的数据。
     *
     * @param state 通道状态
     * @param readBufferSize 读取缓冲区大小
     * @param receiveBufferLimit 接收缓冲区上限
     * @param pendingMatcher pending 响应匹配器
     * @param pendingCompletion pending 完成回调
     * @param knownResponseChecker 已知模块响应判断器
     */
    public void readOnce(BatteryCollectorChannelState state,
                         int readBufferSize,
                         int receiveBufferLimit,
                         PendingResponseMatcher pendingMatcher,
                         PendingResponseCompletion pendingCompletion,
                         KnownResponseChecker knownResponseChecker) {
        SerialPort serialPort = state.getSerialPort();
        byte[] bytes = frameIoService.readAvailableBytes(serialPort, readBufferSize);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        state.setLastReceiveTime(System.currentTimeMillis());
        protocolLogService.logProtocol(properties, state, "rx-bytes",
                "len=" + bytes.length + ", hex=" + BatteryCollectorFrameIoService.bytesToHex(bytes, bytes.length));

        frameIoService.appendAndTrimBuffer(state.getReceiveBuffer(), bytes, receiveBufferLimit);
        BatteryCollectorFrameCodec.DecodeResult decodeResult = frameIoService.decodeBuffer(state.getReceiveBuffer());
        frameIoService.trimBuffer(state.getReceiveBuffer(), receiveBufferLimit);

        for (BatteryCollectorFrame frame : decodeResult.getFrames()) {
            handleFrame(state, frame, pendingMatcher, pendingCompletion, knownResponseChecker);
        }
    }

    /** 处理单帧分发、pending 匹配和非预期响应日志。 */
    private void handleFrame(BatteryCollectorChannelState state,
                             BatteryCollectorFrame frame,
                             PendingResponseMatcher pendingMatcher,
                             PendingResponseCompletion pendingCompletion,
                             KnownResponseChecker knownResponseChecker) {
        state.setLastResponseCode(frame.getCommand());
        protocolLogService.logProtocol(properties, state, "rx-frame",
                "cmd=" + String.format("%02X", frame.getCommand())
                        + ", expect=" + String.format("%02X", state.getExpectedResponseCode())
                        + ", hex=" + frame.toHex());
        moduleFrameDispatcher.dispatch(state.getConfig(), frame);
        if (pendingMatcher.isCurrentPendingResponse(state, frame)) {
            pendingCompletion.handleCompletedPendingResponse(state, frame);
            state.setPendingCommand(null);
            state.setExpectedResponseCode(0);
            state.setCurrentRetryCount(0);
            state.setLastPendingCompletedAt(System.currentTimeMillis());
            state.setLastPendingTimedOut(false);
            state.setRunState(BatteryCollectorRunState.READ);
        } else if (knownResponseChecker.isKnownModuleResponse(frame.getCommand())) {
            log.debug("蓄电池采集响应帧不在当前等待范围内, 通道={}, 请求={}, 期望={}, 实际={}",
                    state.getConfig().getName(),
                    String.format("%02X", state.getLastRequestCode()),
                    String.format("%02X", state.getExpectedResponseCode()),
                    String.format("%02X", frame.getCommand()));
        } else {
            log.info("蓄电池采集收到非预期帧, 通道={}, 请求={}, 期望={}, 实际={}",
                    state.getConfig().getName(),
                    String.format("%02X", state.getLastRequestCode()),
                    String.format("%02X", state.getExpectedResponseCode()),
                    String.format("%02X", frame.getCommand()));
        }
    }

    /** 判断响应帧是否命中当前 pending。 */
    @FunctionalInterface
    public interface PendingResponseMatcher {
        /**
         * 判断响应帧是否命中当前 pending
         *
         * @param state 通道状态
         * @param frame 响应帧
         * @return 是否命中
         */
        boolean isCurrentPendingResponse(BatteryCollectorChannelState state, BatteryCollectorFrame frame);
    }

    /** 处理已命中 pending 的响应帧。 */
    @FunctionalInterface
    public interface PendingResponseCompletion {
        /**
         * 处理已命中 pending 的响应帧
         *
         * @param state 通道状态
         * @param frame 响应帧
         */
        void handleCompletedPendingResponse(BatteryCollectorChannelState state, BatteryCollectorFrame frame);
    }

    /** 判断响应编码是否属于已知模块响应。 */
    @FunctionalInterface
    public interface KnownResponseChecker {
        /**
         * 判断响应编码是否属于已知模块响应
         *
         * @param commandCode 命令编码
         * @return 是否已知
         */
        boolean isKnownModuleResponse(int commandCode);
    }
}
