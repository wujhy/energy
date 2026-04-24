package com.shanhe.project.device.opt.cmd;

import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.consts.DeviceCommConst;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.HostTypeEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Calendar;

/**
 * 主机指令
 *
 * @author wjh
 * @since 2025/4/25
 */
@Service
public class CmdHostService {

    @Resource
    private DeviceCommConst deviceCommConst;

    /**
     * 读取开关量
     *
     * @param host 主机
     * @param type 1：输入开关量 2：输出开关量
     */
    public void cmd57(Host host, int type) {
        // 指令内容
        String bInfo = null;
        // 协议解析类型
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        switch (hostTypeEnum){
            case _2CM03N:
                // 串口类型
                bInfo = CodingUtil.integerToHexString(type, 2);
                break;
            default:
                break;
        }
        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, bInfo, TcpCidEnum._57.getDictValue(), TcpCidEnum._D7.getDictValue()));
    }

    /**
     * 更新配置
     */
    public void cmd60(Host host, Config config) {
        // 指令内容
        String bInfo = null;
        // 协议解析类型
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        switch (hostTypeEnum){
            case _2CM03N:
                // 串口类型
                bInfo = CodingUtil.integerToHexString(config.getPortType(), 2);
                break;
            default:
                break;
        }
        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, bInfo, TcpCidEnum._60.getDictValue(), TcpCidEnum._B0.getDictValue()));
    }

    /**
     * 修改主机日期时间
     *
     * @param host 主机
     */
    public void cmd37(Host host) {
        // 指令内容
        StringBuilder info = new StringBuilder();
        Calendar calendar = Calendar.getInstance();
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.YEAR), 4));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MONTH) + 1, 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.DAY_OF_MONTH), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.HOUR_OF_DAY), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.MINUTE), 2));
        info.append(CodingUtil.integerToHexString(calendar.get(Calendar.SECOND), 2));

        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, info.toString(), TcpCidEnum._37.getDictValue(), BatteryCidEnum._E7.getDictValue()));
    }

    /**
     * 读取设备IP
     */
    public void cmd61(Host host) {
        this.syncCmd(host, TcpCidEnum._61.getDictValue(), TcpCidEnum._B1.getDictValue());
    }

    /**
     * 读取云服务器IP
     */
    public void cmd62(Host host) {
        this.syncCmd(host, TcpCidEnum._62.getDictValue(), TcpCidEnum._B2.getDictValue());
    }

    /**
     * 读取系统数据上报时间
     */
    public void cmd63(Host host) {
        this.syncCmd(host, TcpCidEnum._63.getDictValue(), TcpCidEnum._B3.getDictValue());
    }

    /**
     * 同步指令
     */
    private void syncCmd(Host host, String cid, String dynCid) {
        // 指令内容
        String bInfo;
        // 协议解析类型
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        switch (hostTypeEnum){
            case _2CM03N:
                // 内容
                bInfo = "";
                break;
            default:
                throw new ServiceException("暂不支持该设备类型");
        }
        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, bInfo, cid, dynCid));
    }

    /**
     * 主机下发云服务IP指令
     */
    public void cmd5F(Host host) {
        // 本机ip为设备云服务ip，端口号未tcp服务端端口号
        String[] ip = host.getIp().split("\\.");
        String bInfo = CodingUtil.stringToHexString(ip[0], 2)
                + CodingUtil.stringToHexString(ip[1], 2)
                + CodingUtil.stringToHexString(ip[2], 2)
                + CodingUtil.stringToHexString(ip[3], 2)
                + CodingUtil.integerToHexString(deviceCommConst.getTcpPost(), 4);

        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, bInfo, TcpCidEnum._5F.getDictValue(), TcpCidEnum._DF.getDictValue()));
    }

    /**
     * 主机下发上报时间间隔指令
     */
    public void cmd50(Host host) {
        // 指令内容
        String bInfo;
        // 协议解析类型
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        switch (hostTypeEnum){
            case _2CM03N:
                bInfo = CodingUtil.integerToHexString(host.getDeviceSpaceTime(), 8);
                break;
            default:
                throw new ServiceException("暂不支持该设备类型");
        }
        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, bInfo, TcpCidEnum._50.getDictValue(), TcpCidEnum._D0.getDictValue()));
    }

    /**
     * 主机下发IP指令
     */
    public void cmd5E(Host host) {
        // 指令内容
        String bInfo;
        // 协议解析类型
        HostTypeEnum hostTypeEnum = HostTypeEnum.fromCode(host.getType());
        switch (hostTypeEnum){
            case _2CM03N:
                // 内容
                String[] ips = host.getIp().split("\\.");
                String[] subIps = host.getSubIp().split("\\.");
                String[] netIps = host.getNetIp().split("\\.");
                bInfo = CodingUtil.stringToHexString(ips[0], 2)
                        + CodingUtil.stringToHexString(ips[1], 2)
                        + CodingUtil.stringToHexString(ips[2], 2)
                        + CodingUtil.stringToHexString(ips[3], 2)

                        + CodingUtil.stringToHexString(subIps[0], 2)
                        + CodingUtil.stringToHexString(subIps[1], 2)
                        + CodingUtil.stringToHexString(subIps[2], 2)
                        + CodingUtil.stringToHexString(subIps[3], 2)

                        + CodingUtil.stringToHexString(netIps[0], 2)
                        + CodingUtil.stringToHexString(netIps[1], 2)
                        + CodingUtil.stringToHexString(netIps[2], 2)
                        + CodingUtil.stringToHexString(netIps[3], 2)

                        + CodingUtil.integerToHexString(host.getPort(), 4);
                break;
            default:
                throw new ServiceException("暂不支持该设备类型");
        }
        // 发送指令
        CommServer.returnCmd(DeviceModel.getCmd(host, bInfo, TcpCidEnum._5E.getDictValue(), TcpCidEnum._DE.getDictValue()));
    }
}
