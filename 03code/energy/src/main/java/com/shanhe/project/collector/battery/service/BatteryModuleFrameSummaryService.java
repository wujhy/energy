package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.collector.battery.model.BatteryModuleFrameSummary;
import com.shanhe.project.collector.battery.protocol.BatteryDeviceProtocolCode;
import org.springframework.stereotype.Service;

/**
 * 600节采集模块端帧识别与摘要。
 *
 * <p>这里只做原始模块端帧的轻量识别，不做 980 节聚合语义转换，也不写业务表。
 */
@Service
public class BatteryModuleFrameSummaryService {

    public BatteryModuleFrameSummary summarize(BatteryCollectorFrame frame) {
        if (frame == null) {
            return null;
        }

        BatteryDeviceProtocolCode protocolCode = BatteryDeviceProtocolCode.find(frame.getCommand());
        byte[] payload = frame.getPayloadSafe();
        BatteryModuleFrameSummary.BatteryModuleFrameSummaryBuilder builder = BatteryModuleFrameSummary.builder()
                .protocolCode(protocolCode)
                .known(protocolCode != null)
                .moduleAddress(frame.getAddress())
                .payloadLength(payload.length)
                .payloadHex(CodingUtil.bytesToHexString(payload).toUpperCase());

        if (protocolCode == null) {
            return builder
                    .success(false)
                    .description("未知600节模块端协议")
                    .build();
        }

        int responseFlag = 0;
        boolean success = true;
        if (protocolCode.isStatusResponse()) {
            if (payload.length > 0) {
                responseFlag = payload[0] & 0xFF;
                success = responseFlag == 0;
            } else {
                success = false;
            }
        }
        return builder.responseFlag(responseFlag)
                .success(success)
                .description(protocolCode.getDescription())
                .build();
    }
}
