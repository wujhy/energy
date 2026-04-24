package com.shanhe.project.device.opt.cmd;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.HostTypeEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;

/**
 * 主机类型封装指令
 *
 * @author wjh
 * @since 2025/7/4
 */
public class DeviceModel {

    /**
     * 生成设备下发指令
     *
     * @param host 主机
     * @param config 设备配置
     * @param info 指令内容
     * @param cid 指令编号
     * @param dynCid 动态编号
     * @return 指令
     */
    public static String getCmd(Host host, Config config, String info, String cid, String dynCid) {
        // 主机未在线，下发指令失败
        if (host == null || !CommServer.isOpen()) {
            throw new ServiceException("主机未在线，下发指令失败！");
        }

        // 指令封装
        StringBuilder cmd = new StringBuilder();
        if (StrUtil.equals(host.getType(), HostTypeEnum._2CM03N.getDictValue())) {
            // 指令头
            cmd.append(TcpCharEnum._AA.getDictValue());
            // C0 设备类型
            cmd.append(config == null || config.getType() == null ? "00"
                    : CodingUtil.integerToHexString(config.getType(), 2));
            // C1 串口号
            cmd.append(config == null || config.getPort() == null ? "00"
                    : CodingUtil.integerToHexString(config.getPort(), 2));
            // C2 通道号
            cmd.append(config == null || config.getChannel() == null ? "00"
                    : CodingUtil.integerToHexString(config.getChannel(), 2));
            // C3 自定义协议编号
            cmd.append(dynCid);
            // 设备ID
            cmd.append(StrUtil.isNotBlank(CommServer.getImei()) ? CommServer.getImei() : "0000000000");
            // cid 设备指令
            cmd.append(cid);
            // 指令长度
            cmd.append(CodingUtil.integerToHexString(info.length() / 2, 4));
            // 指令内容
            cmd.append(info);
            // 校验码（截去指令头）
            cmd.append(CodingUtil.energyCheckSum(cmd.substring(2)));
            // 指令结束
            cmd.append(TcpCharEnum._55.getDictValue());
        } else {
            throw new ServiceException("主机类型异常，下发指令失败！");
        }
        // 返回大写字符串
        return cmd.toString().toUpperCase();
    }

    /**
     * 生成主机下发指令
     *
     * @param host 主机
     * @param info 指令内容
     * @param cid 指令编号
     * @param dynCid 动态编号
     * @return 指令
     */
    public static String getCmd(Host host, String info, String cid, String dynCid) {
        return getCmd(host, null, info, cid, dynCid);
    }
}
