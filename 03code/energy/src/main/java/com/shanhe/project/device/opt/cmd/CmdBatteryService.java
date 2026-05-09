package com.shanhe.project.device.opt.cmd;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.comm.tcp.utils.CodingUtil;
import com.shanhe.framework.enums.TcpCharEnum;
import com.shanhe.framework.enums.TcpCidEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 蓄电池存储指令
 */
@Service
public class CmdBatteryService {

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
     * @param isDel 是否删除存储
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
                continue;
            }
            getRealData(host, config, batteryPack.getPackNum());
            getAlarmData(host, config, batteryPack.getPackNum());
            getFaultData(host, config, batteryPack.getPackNum());
        }
    }

    /**
     * 删除蓄电池存储指令
     *
     * @param config 设备配置
     * @param packNum 组号
     */
    public void delBatteryCmd(Config config, Integer packNum) {
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(100 + packNum, 2));
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(110 + packNum, 2));
        deviceCmdService.cmdDel(config, CodingUtil.integerToHexString(120 + packNum, 2));
    }

    /**
     * 获取实时数据
     */
    private void getRealData(Host host, Config config, Integer packNum) {
        sendPackCommand(host, config, packNum, 100, 0x02);
    }

    /**
     * 获取告警数据
     */
    private void getAlarmData(Host host, Config config, Integer packNum) {
        sendPackCommand(host, config, packNum, 110, 0x07);
    }

    /**
     * 获取故障数据
     */
    private void getFaultData(Host host, Config config, Integer packNum) {
        sendPackCommand(host, config, packNum, 120, 0x0D);
    }

    private void sendPackCommand(Host host, Config config, Integer packNum, int dynBase, int dataCode) {
        String dynCid = CodingUtil.integerToHexString(dynBase + packNum, 2);
        StringBuilder info = new StringBuilder();
        info.append(dynCid);
        info.append(TcpCharEnum.HEAD_53.getDictValue());
        info.append("01").append(CodingUtil.integerToHexString(dataCode, 2)).append("01");
        info.append(CodingUtil.integerToHexString(packNum, 2));
        info.append(CodingUtil.energyCheckSum(info.substring(2 + TcpCharEnum.HEAD_53.getDictValue().length())));
        info.append(TcpCharEnum.END_0D.getDictValue());
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
        getRealData(host, config, batteryPack.getPackNum());
        getAlarmData(host, config, batteryPack.getPackNum());
        getFaultData(host, config, batteryPack.getPackNum());
    }
}