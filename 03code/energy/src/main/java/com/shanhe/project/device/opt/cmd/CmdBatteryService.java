package com.shanhe.project.device.opt.cmd;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.ProtocolTypeEnum;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池存储指令
 */
@Service
public class CmdBatteryService {

    protected static Logger logger = LoggerFactory.getLogger(CmdBatteryService.class);

    @Resource
    private IHostService hostService;
    @Resource
    private DeviceCmdService deviceCmdService;

    /**
     * 下发蓄电池58删除存储指令
     *
     * @param config 设备配置
     */
    public void sendCmd58(Config config) {
        this.sendCmd(config, true);
    }

    /**
     * 下发蓄电池52存储指令
     *
     * @param config 设备配置
     */
    public void sendCmd52(Config config) {
        this.sendCmd(config, false);
    }

    /**
     * 下发蓄电池52指令
     *
     * @param config 设备配置
     */
    public void sendCmd(Config config, boolean isDel) {
        Host host = hostService.getDetail();
        if (host == null) {
            return;
        }

        List<BatteryPack> packList = config.getPackList();
        if (packList == null || packList.isEmpty()) {
            return;
        }

        for (BatteryPack batteryPack : packList) {
            if (batteryPack.getPackNum() == null || batteryPack.getPackNum() == 0) {
                continue;
            }
            if (isDel) {
                delBatteryCmd(config, batteryPack.getPackNum());
            } else {
                // 获取实时数据
                getRealData(host, config, batteryPack.getPackNum());
                // 获取告警数据
                getAlarmData(host, config, batteryPack.getPackNum());
                // 获取故障数据
                getFaultData(host, config, batteryPack.getPackNum());
            }
        }
    }

    public void delBatteryCmd(Config config, Integer packNum) {
        // 删除实时数据
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(100 + packNum, 2));
        // 删除告警数据
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(110 + packNum, 2));
        // 删除故障数据
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(120 + packNum, 2));
    }

    /**
     * 获取实时数据
     */
    private void getRealData(Host host, Config config, Integer packNum) {
        // 指令编号
        String dynCid = CodingUtil.integerToHexString(100 + packNum, 2);
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 存储指令编号
        info.append(dynCid);
        // 协议解析类型
        ProtocolTypeEnum protocolTypeEnum = ProtocolTypeEnum.find(config.getProtocolType());
        switch (protocolTypeEnum) {
            case _1:
            case _2:
            case _3:
                // 标准modbus协议配置
            case _99:
            default:
                // 蓄电池指令头
                info.append(TcpCharEnum.HEAD_53.getDictValue());
                // addr、cid、length
                info.append("01").append("02").append("01");
                // info 组编号
                info.append(CodingUtil.integerToHexString(packNum, 2));
                // 校验码（截去指令编号 + 指令头）
                info.append(CodingUtil.energyCheckSum(info.substring(2 + TcpCharEnum.HEAD_53.getDictValue().length())));
                // 蓄电池指令尾
                info.append(TcpCharEnum.END_0D.getDictValue());
                break;
        }

        // 生成完整指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._52.getDictValue(), dynCid));
    }

    /**
     * 获取告警数据
     */
    private void getAlarmData(Host host, Config config, Integer packNum) {
        String dynCid = CodingUtil.integerToHexString(110 + packNum, 2);
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 存储指令编号
        info.append(dynCid);
        // 协议解析类型
        ProtocolTypeEnum protocolTypeEnum = ProtocolTypeEnum.find(config.getProtocolType());
        switch (protocolTypeEnum) {
            case _1:
            case _2:
            case _3:
                // 标准modbus协议配置
            case _99:
            default:
                // 蓄电池指令头
                info.append(TcpCharEnum.HEAD_53.getDictValue());
                // addr、cid、length
                info.append("01").append("07").append("01");
                // info 组编号
                info.append(CodingUtil.integerToHexString(packNum, 2));
                // 校验码（截去指令编号 + 指令头）
                info.append(CodingUtil.energyCheckSum(info.substring(2 + TcpCharEnum.HEAD_53.getDictValue().length())));
                // 蓄电池指令尾
                info.append(TcpCharEnum.END_0D.getDictValue());
                break;
        }

        // 生成完整指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._52.getDictValue(), dynCid));
    }

    /**
     * 获取故障数据
     */
    private void getFaultData(Host host, Config config, Integer packNum) {
        String dynCid = CodingUtil.integerToHexString(120 + packNum, 2);
        // 指令内容
        StringBuilder info = new StringBuilder();
        // 存储指令编号
        info.append(dynCid);
        // 协议解析类型
        ProtocolTypeEnum protocolTypeEnum = ProtocolTypeEnum.find(config.getProtocolType());
        switch (protocolTypeEnum) {
            case _1:
            case _2:
            case _3:
                // 标准modbus协议配置
            case _99:
            default:
                // 蓄电池指令头
                info.append(TcpCharEnum.HEAD_53.getDictValue());
                // addr、cid、length
                info.append("01").append("0D").append("01");
                // info 组编号
                info.append(CodingUtil.integerToHexString(packNum, 2));
                // 校验码（截去指令编号 + 指令头）
                info.append(CodingUtil.energyCheckSum(info.substring(2 + TcpCharEnum.HEAD_53.getDictValue().length())));
                // 蓄电池指令尾
                info.append(TcpCharEnum.END_0D.getDictValue());
                break;
        }

        // 生成完整指令
        CommServer.returnCmd(DeviceModel.getCmd(host, config, info.toString(), TcpCidEnum._52.getDictValue(), dynCid));
    }

    public void sendCmdByPackNum(Config config, BatteryPack batteryPack) {
        Host host = hostService.getDetail();
        if (host == null) {
            return;
        }

        if (batteryPack.getPackNum() == null || batteryPack.getPackNum() == 0) {
            return;
        }
        // 获取实时数据
        getRealData(host, config, batteryPack.getPackNum());
        // 获取告警数据
        getAlarmData(host, config, batteryPack.getPackNum());
        // 获取故障数据
        getFaultData(host, config, batteryPack.getPackNum());

    }
}
