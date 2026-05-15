package com.shanhe.project.iot.CM03N;

import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * CM03N data upload handler.
 */
@Service
public class DataUploadHandler {

    protected static final Logger logger = LoggerFactory.getLogger(DataUploadHandler.class);

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

        logger.debug("ignore legacy CM03N non-battery upload: type={}, port={}, channel={}, c3={}",
                deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
    }
}
