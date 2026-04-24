package com.shanhe.project.iot.CM03N;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 设备响应服务类
 */
@Service
public class DevResponseHandler {

    protected static Logger logger = LoggerFactory.getLogger(DevResponseHandler.class);

    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;

    /**
     * 响应设置系统数据上报时间
     */
    public void cmdD0(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 响应设置配置参数
     */
    public void cmdD1(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 响应修改设备日期时间
     */
    public void cmdE7(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 响应设置串口存储指令包
     */
    public void cmdD2(DeviceData deviceData) {
        this.responseResult(deviceData);
    }

    /**
     * 设置响应结果（通用缓存响应结果）
     *
     * @param deviceData 上报信息
     */
    public int responseResult(DeviceData deviceData){
        if (StrUtil.isBlank(deviceData.getImei()) || StrUtil.isBlank(deviceData.getInfo())) {
            return 1;
        }
        try {
            // 响应结果 0正常 1异常
            int result = CodingUtil.hexParseInt(deviceData.getInfo().substring(0, 2));

            String key = String.format(CacheKeyEnum.RESULT_CX.getKey(), deviceData.getC0(), deviceData.getC1(), deviceData.getC2(), deviceData.getC3());
            // 先删除，重新计时
            CacheUtils.remove(CacheKeyEnum.RESULT_CX.getCache(), key);
            // 缓存结果
            CacheUtils.put(CacheKeyEnum.RESULT_CX.getCache(), key, result == 0 ? 0 : 1);
            if (result != 0) {
                logger.info("imei：{} 返回的结果{}：{}", deviceData.getImei(), key, result);
            }
            return result;
        } catch (Exception e) {
            logger.error("imei：{} 返回结果异常：{}", deviceData.getImei(), deviceData.getInfo());
            return 1;
        }
    }

    /**
     * 响应读取串口配置参数
     */
    public void cmdB0(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            logger.error("B0：响应读取配置参数 => {}", deviceData.getInfo());
            return;
        }

        // 保存设备数据
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

    /**
     * 响应读取串口配置参数
     */
    public void cmdB1(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            logger.error("B1：响应读取配置参数 => {}", deviceData.getInfo());
            return;
        }

        String str = deviceData.getInfo().substring(2);
        String ip = CodingUtil.hexParseInt(str.substring(0, 2))
                + "." + CodingUtil.hexParseInt(str.substring(2, 4))
                + "." + CodingUtil.hexParseInt(str.substring(4, 6))
                + "." + CodingUtil.hexParseInt(str.substring(6, 8));

        String subIp = CodingUtil.hexParseInt(str.substring(8, 10))
                + "." + CodingUtil.hexParseInt(str.substring(10, 12))
                + "." + CodingUtil.hexParseInt(str.substring(12, 14))
                + "." + CodingUtil.hexParseInt(str.substring(14, 16));

        String netIp = CodingUtil.hexParseInt(str.substring(16, 18))
                + "." + CodingUtil.hexParseInt(str.substring(18, 20))
                + "." + CodingUtil.hexParseInt(str.substring(20, 22))
                + "." + CodingUtil.hexParseInt(str.substring(22, 24));

        Host host = hostService.getDetail();
        host.setDeviceIp(ip);
//        host.setSubIp(subIp);
//        host.setNetIp(netIp);
        host.setDevicePort(CodingUtil.hexParseInt(str.substring(24, 28)));
        hostService.updateHost(host);
    }

    /**
     * 响应读取云服务器配置参数（本项目）
     */
    public void cmdB2(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            logger.error("B2：响应读取配置参数 => {}", deviceData.getInfo());
            return;
        }

        // 保存（暂不保存，服务IP端口不应取自设备）
        Host host = hostService.getDetail();
        String ip = CodingUtil.hexParseInt(deviceData.getInfo().substring(2, 4))
                + "." + CodingUtil.hexParseInt(deviceData.getInfo().substring(4, 6))
                + "." + CodingUtil.hexParseInt(deviceData.getInfo().substring(6, 8))
                + "." + CodingUtil.hexParseInt(deviceData.getInfo().substring(8, 10));
        host.setIp(ip);
        host.setPort(CodingUtil.hexParseInt(deviceData.getInfo().substring(10, 14)));
        hostService.updateHost(host);
    }

    /**
     * 响应读取系统数据上报时间
     */
    public void cmdB3(DeviceData deviceData) {
        int resResult = this.responseResult(deviceData);
        if (1 == resResult) {
            logger.error("B3：响应读取配置参数 => {}", deviceData.getInfo());
            return;
        }

        // 保存
        Host host = hostService.getDetail();
        host.setDeviceSpaceTime(CodingUtil.hexParseInt(deviceData.getInfo().substring(2, 10)));
        hostService.updateHost(host);
    }
}
