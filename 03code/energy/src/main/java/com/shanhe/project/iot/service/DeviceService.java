package com.shanhe.project.iot.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.iot.CM03N.DataSwitchHandler;
import com.shanhe.project.iot.CM03N.DataUploadHandler;
import com.shanhe.project.iot.CM03N.DevResponseHandler;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 设备消息处理服务。
 *
 * @author wjh
 * @since 2026-05-25
 */
@Slf4j
@Service
public class DeviceService {

    @Resource
    private DevResponseHandler devResponseHandler;
    @Resource
    private DataUploadHandler dataUploadHandler;
    @Resource
    private DataSwitchHandler dataSwitchHandler;
    @Resource
    private ClientReportService clientReportService;

    /**
     * 处理设备上线事件
     *
     * @param deviceData 设备数据
     */
    public void tcpDeviceOnline(DeviceData deviceData) {
        log.debug("TCP设备上线事件已忽略, 指令={}, 设备标识={}", deviceData.getCid(), deviceData.getImei());
    }

    /**
     * 处理设备离线事件
     */
    public void tcpDeviceOffline() {
        log.debug("TCP设备离线事件已忽略");
    }

    /**
     * 处理设备上报数据
     *
     * @param deviceData 设备数据
     */
    public void tcpDevice(DeviceData deviceData) {
        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())) {
            CommServer.returnCmd("00");
        }

        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())
                || StrUtil.equals(deviceData.getCid(), TcpCidEnum._88.getDictValue())) {
            return;
        }

        this.prefilter(deviceData);

        TcpCidEnum cidEnum = TcpCidEnum.find(deviceData.getCid());
        log.debug("{}-{}-{}-{} => {}", deviceData.getC1(), deviceData.getC2(), deviceData.getC3(), deviceData.getCid(), cidEnum.getDictLabel(), deviceData.getInfo());
        switch (cidEnum) {
            case _D1:
                devResponseHandler.cmdD1(deviceData);
                break;
            case _D2:
                devResponseHandler.cmdD2(deviceData);
                break;
            case _D3:
                dataUploadHandler.cmdD3(deviceData);
                break;
            case _D4:
                dataUploadHandler.cmdD4(deviceData);
                break;
            case _D5:
            case _D6:
            case _D7:
            case _DA:
            case _DB:
            case _DD:
                devResponseHandler.responseResult(deviceData);
                break;
            case _D8:
                dataSwitchHandler.cmdD8(deviceData);
                break;
            case _B0:
                devResponseHandler.cmdB0(deviceData);
                break;
            default:
                log.info("指令错误：{}", deviceData.getCid());
                break;
        }

    }

    /** 对设备数据进行前置过滤，调试报文更新缓存。 */
    private void prefilter(DeviceData deviceData) {
        if (StrUtil.equals(deviceData.getC3(), "FF")) {
            clientReportService.updateCmdDebug(deviceData.getImei(), deviceData.getInfo());
        }
    }
}
