package com.shanhe.project.iot.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.model.DeviceData;
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
 * 设备Service业务层处理
 *
 * @author wjh
 * @since 2025/2/5
 */
@Service
public class DeviceService
{
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

    /**
     * 设备上线操作
     *
     * @param deviceData 消息内容
     */
    public void tcpDeviceOnline(DeviceData deviceData) {
        // 主机上线
        hostService.online(deviceData);
    }

    /**
     * 设备全部下线操作
     */
    public void tcpDeviceOffline() {
        // 主机下线，全部设备下线
        hostService.offline();
    }

    /**
     * 解析后消息处理
     */
    public void tcpDevice(DeviceData deviceData) {
        // 心跳包响应
        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())) {
            CommServer.returnCmd("00");
        }

        // 心跳包或注册包
        if (StrUtil.equals(deviceData.getCid(), TcpCidEnum._80.getDictValue())
                || StrUtil.equals(deviceData.getCid(), TcpCidEnum._88.getDictValue())) {
            // 主机上线处理
            hostService.online(deviceData);
            return;
        }

        // 前置处理
        this.prefilter(deviceData);

        // 指令解析（只有设备实际响应数据的才做设备上线处理）
        TcpCidEnum cidEnum = TcpCidEnum.find(deviceData.getCid());
        logger.debug("{}-{}-{}-{}：{} => {}", deviceData.getC1(), deviceData.getC2(), deviceData.getC3(), deviceData.getCid(), cidEnum.getDictLabel(), deviceData.getInfo());
        switch (cidEnum) {
            case _D0: //响应系统数据上报时间
                devResponseHandler.cmdD0(deviceData);
                break;
            case _D1: //响应设置配置参数
                devResponseHandler.cmdD1(deviceData);
                break;
            case _D2: //响应设置串口存储指令包
                devResponseHandler.cmdD2(deviceData);
                break;
            case _D3: //串口存储指令包应答,主动上报数据
                dataUploadHandler.cmdD3(deviceData);
                break;
            case _D4: //串口存储指令包应答,手动下发指令，上报数据
                dataUploadHandler.cmdD4(deviceData);
                break;
            case _D5: //响应读取模拟量
                dataSwitchHandler.cmdD5(deviceData);
                break;
            case _D6: //响应设置输出模拟量
                dataSwitchHandler.cmdD6(deviceData);
                break;
            case _D7: //响应读取开关量
                dataSwitchHandler.cmdD7(deviceData);
                break;
            case _D8: //响应设置输出开关量
                dataSwitchHandler.cmdD8(deviceData);
                break;
            case _E0: //响应设置空调工作模式
            case _D9: //响应红外学习
            case _DA: //响应删除全部存储指令
            case _DB: //响应删除单条存储指令
            case _DD: //响应读取全部存储指令
            case _DE: //响应设置设备IP地址
            case _DF: //响应设置云服务器IP地址
                devResponseHandler.responseResult(deviceData);
                break;
            case _E7: //响应修改日期时间
                devResponseHandler.cmdE7(deviceData);
                break;
            case _B0: //响应读取配置参数
                devResponseHandler.cmdB0(deviceData);
                break;
            case _B1: //响应读取设备IP地址
                devResponseHandler.cmdB1(deviceData);
                break;
            case _B2: //响应读取云服务器IP地址
//                devResponseHandler.cmdB2(deviceData);
                break;
            case _B3: //响应读取系统数据上报时间
                devResponseHandler.cmdB3(deviceData);
                break;
            default:
                logger.info("指令错误：{}", deviceData.getCid());
                break;
        }

        // 后置处理
        this.postfilter(deviceData);
    }

    /**
     * 前置处理
     */
    private void prefilter(DeviceData deviceData) {
        // 测试指令结果
        if (StrUtil.equals(deviceData.getC3(), "FF")) {
            clientReportService.updateCmdDebug(deviceData.getImei(), deviceData.getInfo());
        }
    }

    /**
     * 后置处理
     */
    private void postfilter(DeviceData deviceData) {}
}
