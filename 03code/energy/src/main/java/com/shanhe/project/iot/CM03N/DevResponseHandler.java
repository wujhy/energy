package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 设备响应处理服务。
 */
@Service
public class DevResponseHandler {

    protected static Logger logger = LoggerFactory.getLogger(DevResponseHandler.class);

    @Resource
    private IConfigService configService;

    public void cmdD1(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    public void cmdD2(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    public int responseResult(DeviceData deviceData){
        if (StrUtil.isBlank(deviceData.getImei()) || StrUtil.isBlank(deviceData.getInfo())) {
            return 1;
        }
        try {
            int result = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));

            String key = String.format(CacheKeyEnum.RESULT_CX.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
            CacheUtils.remove(CacheKeyEnum.RESULT_CX.getCache(), key);
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), key, result == 0 ? 0 : 1);
            if (result != 0) {
                logger.info("imei：{} 返回结果{}：{}", deviceData.getImei(), key, result);
            }
            return result;
        } catch (Exception e) {
            logger.error("imei：{} 返回结果异常：{}", deviceData.getImei(), deviceData.getInfo());
            return 1;
        }
    }

    public void cmdB0(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            logger.error("B0：响应读取配置参数=> {}", deviceData.getInfo());
            return;
        }

        Config config = new Config();
        config.setType(deviceData.getC0());
        config.setPort(deviceData.getC1());
        config.setChannel(deviceData.getC2());
        config.setPortType(CodingUtil.hexParseInt(deviceData.getInfo().substring(2, 4)));
        config.setBaudRate(CodingUtil.hexParseInt(deviceData.getInfo().substring(4, 12)));
        config.setDataBits(CodingUtil.hexParseInt(deviceData.getInfo().substring(12, 14)));
        config.setStopBits(CodingUtil.hexParseInt(deviceData.getInfo().substring(14, 16)));
        config.setParityBits(CodingUtil.hexParseInt(deviceData.getInfo().substring(16, 18)));
        config.setIntervalTime(CodingUtil.hexParseInt(deviceData.getInfo().substring(18, 22)));
        configService.updatePost(config);
    }
}
