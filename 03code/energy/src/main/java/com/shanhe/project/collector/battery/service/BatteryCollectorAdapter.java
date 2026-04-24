package com.shanhe.project.collector.battery.service;

import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryCollectorFrame;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.iot.CM03N.BatteryHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 将新采集模块收到的 Battery 电池帧桥接到既有 BatteryHandler。
 */
@Slf4j
@Component
public class BatteryCollectorAdapter {

    @Resource
    private IConfigService configService;
    @Resource
    private BatteryHandler batteryHandler;

    public void dispatch(BatteryCollectorChannelConfig channelConfig, BatteryCollectorFrame frame) {
        if (channelConfig.getBatteryPort() == null || channelConfig.getBatteryChannel() == null) {
            log.debug("Battery 通道未绑定 energy 电池配置，跳过业务分发 channel={}", channelConfig.getName());
            return;
        }

        Config config = configService.getCacheBy(
                DeviceTypeEnum._1.getDictValue(),
                channelConfig.getBatteryPort(),
                channelConfig.getBatteryChannel());
        if (config == null) {
            log.warn("Battery 通道未找到电池配置 channel={}, bindPort={}, bindChannel={}",
                    channelConfig.getName(),
                    channelConfig.getBatteryPort(),
                    channelConfig.getBatteryChannel());
            return;
        }

        DeviceData deviceData = new DeviceData();
        deviceData.setC0(DeviceTypeEnum._1.getDictValue());
        deviceData.setC1(channelConfig.getBatteryPort());
        deviceData.setC2(channelConfig.getBatteryChannel());
        deviceData.setC3(String.format("%02X", frame.getCommand()));
        deviceData.setCid("D3");
        deviceData.setImei(buildImei(channelConfig));
        deviceData.setInfo(frame.toHex());
        batteryHandler.doUploadData(config, deviceData);
    }

    private String buildImei(BatteryCollectorChannelConfig channelConfig) {
        return "BatteryCollector-" + channelConfig.getPortName() + "-" + channelConfig.getBatteryPort() + "-" + channelConfig.getBatteryChannel();
    }
}
