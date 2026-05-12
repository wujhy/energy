package com.shanhe.project.iot.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.model.DeviceData;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.iot.CM03N.DataSwitchHandler;
import com.shanhe.project.iot.CM03N.DataUploadHandler;
import com.shanhe.project.iot.CM03N.DevResponseHandler;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Device service.
 */
@Service
public class DeviceService {
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    @Resource
    private IHostService hostService;
    @Resource
    private DevResponseHandler devResponseHandler;
    @Resource
    private DataUploadHandler dataUploadHandler;
    @Resource
    private DataSwitchHandler dataSwitchHandler;
    @Resource
    private ClientReportService clientReportService;

    public void tcpDeviceOnline(DeviceData deviceData) {
        hostService.online(deviceData);
    }

    public void tcpDeviceOffline() {
        hostService.offline();
    }

    public void tcpDevice(DeviceData deviceData) {
        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())) {
            CommServer.returnCmd("00");
        }

        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())
                || StrUtil.equals(deviceData.getCid(), TcpCidEnum._88.getDictValue())) {
            hostService.online(deviceData);
            return;
        }

        this.prefilter(deviceData);

        TcpCidEnum cidEnum = TcpCidEnum.find(deviceData.getCid());
        logger.debug("{}-{}-{}-{} => {}", deviceData.getC1(), deviceData.getC2(), deviceData.getC3(), deviceData.getCid(), cidEnum.getDictLabel(), deviceData.getInfo());
        switch (cidEnum) {
            case _D0:
                devResponseHandler.cmdD0(deviceData);
                break;
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
            case _DE:
            case _DF:
                devResponseHandler.responseResult(deviceData);
                break;
            case _D8:
                dataSwitchHandler.cmdD8(deviceData);
                break;
            case _E7:
                devResponseHandler.cmdE7(deviceData);
                break;
            case _B0:
                devResponseHandler.cmdB0(deviceData);
                break;
            case _B1:
                devResponseHandler.cmdB1(deviceData);
                break;
            case _B2:
                break;
            case _B3:
                devResponseHandler.cmdB3(deviceData);
                break;
            default:
                logger.info("Invalid command cid={}", deviceData.getCid());
                break;
        }

    }

    private void prefilter(DeviceData deviceData) {
        if (StrUtil.equals(deviceData.getC3(), "FF")) {
            clientReportService.updateCmdDebug(deviceData.getImei(), deviceData.getInfo());
        }
    }
}
