package com.shanhe.project.iot.CM03N;

import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * CM03N数据上传处理器。
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class DataUploadHandler {

    @Resource
    private IConfigService configService;
    @Resource
    private BatteryHandler batteryHandler;

    public void cmdD3(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    public void cmdD4(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    private void dealData(DeviceData deviceData) {
        Config config = configService.selectDefaultConfig();
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            batteryHandler.doUploadData(config, deviceData);
            return;
        }

        log.debug("ignore legacy CM03N non-battery upload: type={}, port={}, channel={}, c3={}",
                deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
    }
}
