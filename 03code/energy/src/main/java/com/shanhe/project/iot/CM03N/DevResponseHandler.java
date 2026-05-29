package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.CacheKeyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * 设备响应处理服务。
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class DevResponseHandler {

    /**
     * 处理D1指令响应
     *
     * @param deviceData 设备数据
     */
    public void cmdD1(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 处理D2指令响应
     *
     * @param deviceData 设备数据
     */
    public void cmdD2(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 处理通用响应结果
     *
     * @param deviceData 设备数据
     * @return 响应结果码，0表示成功
     */
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
                log.info("imei：{} 返回结果{}：{}", deviceData.getImei(), key, result);
            }
            return result;
        } catch (Exception e) {
            log.error("imei：{} 返回结果异常：{}", deviceData.getImei(), deviceData.getInfo());
            return 1;
        }
    }

    /**
     * 处理B0指令（读取配置参数）响应
     *
     * @param deviceData 设备数据
     */
    public void cmdB0(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            log.error("B0：响应读取配置参数=> {}", deviceData.getInfo());
            return;
        }

        log.debug("忽略静态默认配置响应, 类型={}, 端口={}, 通道={}",
                deviceData.getC0(), deviceData.getC1(), deviceData.getC2());
    }
}
