package com.shanhe.project.iot.CM03N;

import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.IConfigService;
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

    /** 配置服务。 */
    @Resource
    private IConfigService configService;
    /** 蓄电池处理器。 */
    @Resource
    private BatteryHandler batteryHandler;

    public void cmdD3(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    public void cmdD4(DeviceData deviceData) {
        this.dealData(deviceData);
    }

    /** 根据设备类型分派数据上传处理。 */
    private void dealData(DeviceData deviceData) {
        Config config = configService.selectDefaultConfig();
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            batteryHandler.doUploadData(config, deviceData);
            return;
        }

        log.debug("忽略旧版CM03N非电池数据上传: 类型={}, 端口={}, 通道={}, c3={}",
                deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
    }
}
