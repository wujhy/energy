package com.shanhe.project.device.opt.service;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.HostTypeEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 旧主机基础指令服务。
 *
 * @author wjh
 * @since 2025/4/18
 */
@Component
public class DeviceCmdService {

    protected static final Logger logger = LoggerFactory.getLogger(DeviceCmdService.class);

    @Resource
    private IHostService hostService;

    /**
     * 删除单条存储指令。
     *
     * @param config 设备配置
     * @param protocolCode 指令编号
     */
    public void cmdDel(Config config, String protocolCode) {
        Host host = hostService.getDetail();
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        if (hostTypeEnum != HostTypeEnum._2CM03N) {
            logger.debug("暂不支持该设备");
            return;
        }

        CommServer.returnCmd(DeviceModel.getCmd(host, config, protocolCode, TcpCidEnum._5B.getDictValue(), protocolCode));
    }

    /**
     * 下发串口参数。
     *
     * @param config 设备配置
     */
    public void cmdPort(Config config) {
        Host host = hostService.getDetail();
        String info = CodingUtil.integerToHexString(config.getPortType() == null ? 0 : config.getPortType(), 2)
                + CodingUtil.integerToHexString(config.getBaudRate() == null ? 0 : config.getBaudRate(), 8)
                + CodingUtil.integerToHexString(config.getDataBits() == null ? 0 : config.getDataBits(), 2)
                + CodingUtil.integerToHexString(config.getStopBits() == null ? 0 : config.getStopBits(), 2)
                + CodingUtil.integerToHexString(config.getParityBits() == null ? 0 : config.getParityBits(), 2)
                + CodingUtil.integerToHexString(config.getIntervalTime() == null ? 0 : config.getIntervalTime(), 4);
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info, TcpCidEnum._51.getDictValue(), "00"));
    }
}
