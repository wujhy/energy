package com.shanhe.project.iot.battery;

import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.iot.model.BatteryModeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 电池组操作响应信息
 */
@Service
public class BatteryOptResHandler {

    protected static Logger logger = LoggerFactory.getLogger(BatteryOptResHandler.class);
    /** 缓存结果 **/
    CacheKeyEnum cacheKeyEnum = CacheKeyEnum.RESULT;

    /**
     * 上传设备型号及软件版本号
     *
     * @param deviceData 上报信息
     */
    public void uploadBatterySoftNum(Config config, DeviceData deviceData) {
        /* 版本号通过注册设置，此处先不管*/
        String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
        // 二进制字符串
        String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
        // 应答结果
        Integer result = CodingUtil.binaryToDecimal(binary.substring(0, 4));
        String softNum = Objects.equals(result, 0) ? String.format("V%s.%s", Integer.parseInt(CodingUtil.hexStringToString(info.substring(4, 6))),
                Integer.parseInt(CodingUtil.hexStringToString(info.substring(6, 8)))) : "";

        // 请求结果放入缓存
        String key = String.format(cacheKeyEnum.getKey(), config.getConfigId(), 0, deviceData.getC3());
        CacheUtils.put(cacheKeyEnum.getCache(), key, softNum);
    }

    /**
     * 设置系统参数响应
     */
    public void batteryResponse85(Config config, DeviceData deviceData) {
        try {
            String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
            // 参数号
            String paramNum = info.substring(0, 2);
            // 应答结果
            Integer result = CodingUtil.hexStringToInteger(info.substring(2, 4));

            String key = String.format(cacheKeyEnum.getKey(), config.getConfigId(), 0, deviceData.getC3() + paramNum);
            CacheUtils.put(cacheKeyEnum.getCache(), key, Objects.equals(result, 0) ? 0 : 1);
        } catch (Exception e) {
            logger.error("电池组系统参数响应结果解析异常：{}", e.getMessage());
        }
    }

    /**
     * 电池组测试响应结果
     */
    public void uploadBatteryResponse(Config config, DeviceData deviceData) {
        try {
            String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
            // 二进制字符串
            String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
            // 应答结果
            Integer result = CodingUtil.binaryToDecimal(binary.substring(0, 4));
            // 电池组编号
            Integer packNum = CodingUtil.binaryToDecimal(binary.substring(4, 8));

            // 请求结果放入缓存
            String key = String.format(cacheKeyEnum.getKey(), config.getConfigId(), packNum, deviceData.getC3());
            CacheUtils.put(cacheKeyEnum.getCache(), key, Objects.equals(result, 0) ? 0 : 1);
        } catch (Exception e) {
            logger.error("电池组测试响应结果解析异常：{}", e.getMessage());
        }
    }

    /**
     * 电池组设置响应结果
     */
    public void setBatteryResponse(Config config, DeviceData deviceData) {
        try {
            String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
            // 二进制字符串
            String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
            // 应答结果
            Integer result = CodingUtil.binaryToDecimal(binary.substring(0, 4));

            // 请求结果放入缓存
            String key = String.format(cacheKeyEnum.getKey(), config.getConfigId(), 0, deviceData.getC3());
            CacheUtils.put(cacheKeyEnum.getCache(), key, Objects.equals(result, 0) ? 0 : 1);
        } catch (Exception e) {
            logger.error("电池组设置响应结果解析异常：{}", e.getMessage());
        }
    }

    /**
     * 电池组工作模式
     */
    public void getModeStatus(Config config, DeviceData deviceData) {
        try {
            String info = deviceData.getInfo().substring(16, deviceData.getInfo().length() - 4);
            // 二进制字符串
            String binary = CodingUtil.hexString2binaryString(info.substring(0, 2));
            // 电池组编号
            Integer packNum = CodingUtil.binaryToDecimal(binary.substring(4, 8));
            // 应答结果
            BatteryModeInfo batteryModeInfo = new BatteryModeInfo();
            batteryModeInfo.setPackNum(packNum);
            batteryModeInfo.setResult(CodingUtil.binaryToDecimal(binary.substring(0, 4)));
            batteryModeInfo.setMode(CodingUtil.hexStringToInteger(info.substring(2, 4)));
            batteryModeInfo.setStatus(CodingUtil.hexStringToInteger(info.substring(4, 6)));
            batteryModeInfo.setAddress(CodingUtil.hexStringToInteger(info.substring(6, 8)));

            String key = String.format(cacheKeyEnum.getKey(), config.getConfigId(), null, deviceData.getC3());
            if (Objects.equals(batteryModeInfo.getStatus(), 0)) {
                Object result = CacheUtils.get(cacheKeyEnum.getCache(), key);
                if (result instanceof BatteryModeInfo) {
                    BatteryModeInfo oldBatteryModeInfo = (BatteryModeInfo) result;
                    batteryModeInfo.setLastPackNum(oldBatteryModeInfo.getLastPackNum());
                    batteryModeInfo.setLastMode(oldBatteryModeInfo.getLastMode());
                    batteryModeInfo.setLastAddress(oldBatteryModeInfo.getAddress());
                    //如果是启动内阻测试，且数据是无测试的，继续使用系统中的状态
                    if (Objects.equals(oldBatteryModeInfo.getAddress(), 1) && batteryModeInfo.getMode() == 0) {
                        batteryModeInfo.setResult(oldBatteryModeInfo.getResult());
                        batteryModeInfo.setStatus(oldBatteryModeInfo.getStatus());
                        batteryModeInfo.setAddress(oldBatteryModeInfo.getAddress());
                    }
                }
            }

            // 请求结果放入缓存
            CacheUtils.put(cacheKeyEnum.getCache(), key, batteryModeInfo);
        } catch (Exception e) {
            logger.error("电池组工作模式响应结果解析异常：{}", e.getMessage());
        }
    }
}
