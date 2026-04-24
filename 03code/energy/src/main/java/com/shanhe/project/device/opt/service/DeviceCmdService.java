package com.shanhe.project.device.opt.service;

import cn.hutool.core.util.StrUtil;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.HostTypeEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigProtocol;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.cmd.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

/**
 * 设备指令
 *
 * @author wjh
 * @since 2025/4/18
 */
@Component
public class DeviceCmdService {
    protected static Logger logger = LoggerFactory.getLogger(DeviceCmdService.class);
    @Resource
    private IHostService hostService;

    /** 可以自动定时下发的指令类型 **/
    private static final List<String> AUTO_SEND_CID = Arrays.asList(TcpCidEnum._52.getDictValue(), TcpCidEnum._55.getDictValue(), TcpCidEnum._57.getDictValue());

    /**
     * 下发指令
     *
     * @param config 设备
     * @param configProtocol 指令
     */
    public void cmd(Config config, ConfigProtocol configProtocol) {
        // 协议内容为空
        if (StrUtil.isBlank(configProtocol.getCmdContent())) {
            return;
        }
        // 是否允许定时下发
        if (!AUTO_SEND_CID.contains(configProtocol.getCmdType())) {
            return;
        }
        Host host = hostService.onlineHost();
        if (host == null || !CommServer.isOpen()) {
            logger.debug("主机未注册初始化，不能下发指令");
            return;
        }

        String cmdStr;
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        if (hostTypeEnum == HostTypeEnum._2CM03N) {
            cmdStr = this.doCmd(host, config, configProtocol);
        } else {
            logger.debug("暂不支持该设备");
            return;
        }

        CommServer.returnCmd(cmdStr);
    }

    /**
     * 读取配置参数
     */
    private String doCmd(Host host, Config config, ConfigProtocol configProtocol) {
        String info;
        if (StrUtil.equals(TcpCidEnum._52.getDictValue(), configProtocol.getCmdType())) {
            // 设置串口存储指令包，内容(指令号 + 指令包)
            info = configProtocol.getProtocolCode() + configProtocol.getCmdContent();
        } else {
            info = configProtocol.getCmdContent();
        }

        // 生成完整指令
        return DeviceModel.getCmd(host, config, info, configProtocol.getCmdType(), configProtocol.getProtocolCode());
    }

    /**
     * 下发删除指令
     * @param config 设备
     * @param protocolCode 指令编号
     */
    public void cmdDel(Config config, String protocolCode) {
        Host host = hostService.getDetail();
        String cmdStr;
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        if (hostTypeEnum == HostTypeEnum._2CM03N) {
            cmdStr = DeviceModel.getCmd(host, config, protocolCode, TcpCidEnum._5B.getDictValue(), protocolCode);
        } else {
            logger.debug("暂不支持该设备");
            return;
        }

        CommServer.returnCmd(cmdStr);
    }

    /**
     * 下发全部清除指令
     */
    public void cmdDelAll() {
        Host host = hostService.getDetail();
        String cmdStr;
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        if (hostTypeEnum == HostTypeEnum._2CM03N) {
            cmdStr = DeviceModel.getCmd(host, "AA", TcpCidEnum._5A.getDictValue(), "00");
        } else {
            logger.debug("暂不支持该设备");
            return;
        }

        CommServer.returnCmd(cmdStr);
    }

    /**
     * 下发串口信息
     */
    public void cmdPort(Config config) {
        // 串口类型不是RS485、RS232的不需设置
//        if (!Objects.equals(PortTypeEnum._1.getDictValue(), config.getPortType())
//                && !Objects.equals(PortTypeEnum._2.getDictValue(), config.getPortType())) {
//            return;
//        }

        Host host = hostService.getDetail();
        // 内容：串口类型、波特率、数据位、停止位、奇偶校验位、间隔时间ms
        String info = CodingUtil.integerToHexString(config.getPortType() == null ? 0 : config.getPortType(), 2)
                + CodingUtil.integerToHexString(config.getBaudRate() == null ? 0 : config.getBaudRate(), 8)
                + CodingUtil.integerToHexString(config.getDataBits() == null ? 0 : config.getDataBits(), 2)
                + CodingUtil.integerToHexString(config.getStopBits() == null ? 0 : config.getStopBits(), 2)
                + CodingUtil.integerToHexString(config.getParityBits() == null ? 0 : config.getParityBits(), 2)
                + CodingUtil.integerToHexString(config.getIntervalTime() == null ? 0 : config.getIntervalTime(), 4);
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info, TcpCidEnum._51.getDictValue(), "00"));
    }
}
