package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.CacheKeyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 开关量响应处理。
 *
 * @author wjh
 * @since 2025/6/6
 */
@Service
public class DataSwitchHandler {

    protected static final Logger logger = LoggerFactory.getLogger(DataSwitchHandler.class);

    /**
     * 保留输出开关量响应，用于声光告警设备控制结果缓存。
     *
     * @param deviceData 响应数据
     */
    public void cmdD8(DeviceData deviceData) {
        if (StrUtil.isBlank(deviceData.getImei()) || StrUtil.isBlank(deviceData.getC3()) || StrUtil.isBlank(deviceData.getInfo())) {
            return;
        }
        try {
            String key = String.format(CacheKeyEnum.RESULT_CX.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
            int resResult = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));
            if (resResult == 1) {
                logger.error("响应设置输出开关量失败：key={}, result={}", key, resResult);
            }
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), key, resResult == 0 ? 0 : 1);
        } catch (Exception e) {
            logger.error("响应设置输出开关量异常：imei={}, info={}", deviceData.getImei(), deviceData.getInfo());
        }
    }
}
