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
 * CM03N数据上报处理。
 *
 * @author wjh
 * @since 2025/4/9
 */
@Service
public class DataUploadHandler {

    protected static final Logger logger = LoggerFactory.getLogger(DataUploadHandler.class);

    @Resource
    private IConfigService configService;
    @Resource
    private BatteryHandler batteryHandler;

    /**
     * 自动上报数据。
     *
     * @param deviceData 上报数据
     */
    public void cmdD3(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    /**
     * 手动指令响应数据。
     *
     * @param deviceData 上报数据
     */
    public void cmdD4(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    /**
     * 旧CM03N链路仅保留蓄电池兼容入口，其他设备类型由独立模块后续按需接管。
     *
     * @param deviceData 上报数据
     */
    private void dealData(DeviceData deviceData) {
        Config config = configService.getCache();
        if (config == null) {
            logger.error("设备不存在：{}", deviceData);
            return;
        }

        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            batteryHandler.doUploadData(config, deviceData);
            return;
        }

        logger.debug("忽略旧CM03N非蓄电池上报：type={}, port={}, channel={}, c3={}",
                deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
    }
}
