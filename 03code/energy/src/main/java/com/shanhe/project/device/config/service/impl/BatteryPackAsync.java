package com.shanhe.project.device.config.service.impl;

import com.shanhe.framework.comm.CommServer;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.opt.cmd.CmdBatteryService;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.service.DeviceCmdService;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * 电池组Service业务层处理
 *
 * @author wjh
 * @since 2024-12-23
 */
@Slf4j
@Service
public class BatteryPackAsync {

    @Resource
    private ControlBatterySet controlBatterySet;
    @Resource
    private CmdBatteryService cmdBatteryService;
    @Resource
    private DeviceCmdService deviceCmdService;
    @Resource
    private ControlBattery controlBattery;


    @Async
    public void del(BatteryPack batteryPack, Config config) {
        delSendCmd(config, batteryPack.getPackNum());
    }

    @Async
    public void updateBatteryCmd(Config config, BatteryPack batteryPack) {

        // 通道未连接，不下发指令
        if (!CommServer.isOpen()) {
            return;
        }

        // 只要通道开启，均下发串口配置
        deviceCmdService.cmdPort(config);

        // 设备未开启时
        if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
            delSendCmd(config, batteryPack.getPackNum());
            return;
        }

        // 蓄电池，下发蓄电池组配置
        sendBatteryPack(batteryPack);

        // ------------------ 设备开启时 -----------------------
        // 同步蓄电池信息
        controlBattery.doUploadBattery(config);
        // 同步蓄电池时间
        controlBattery.doSynBatteryDate(config);

        // 已开启电池组
        try {
            // 同步告警配置
            controlBattery.doSynBatteryAlarm(config, batteryPack.getPackNum(), false);
        } catch (Exception e) {
            log.error("同步蓄电池设备 {} 参数失败：{}", config.getName(), e.getMessage());
        }

        // 存储指令
        cmdBatteryService.sendCmdByPackNum(config, batteryPack);
    }

    public void delSendCmd(Config config, Integer packNum) {
        // 删除存储指令
        cmdBatteryService.delBatteryCmd(config, packNum);

        BatterySetVO batterySetVO = new BatterySetVO();
        batterySetVO.setPackNum(packNum);
        batterySetVO.setConfigId(config.getConfigId());
        batterySetVO.setBatCapacity(0D);
        batterySetVO.setBatSinSize(0);
        batterySetVO.setBatSinModel(0);
        controlBatterySet.doSet(batterySetVO, BatteryCidEnum._09);
    }

    private void sendBatteryPack(BatteryPack batteryPack) {
        BatterySetVO batterySetVO = new BatterySetVO();
        batterySetVO.setPackNum(batteryPack.getPackNum());
        batterySetVO.setConfigId(batteryPack.getConfigId());
        batterySetVO.setBatCapacity(batteryPack.getBatCapacity());
        batterySetVO.setBatSinSize(batteryPack.getBatSinSize());
        batterySetVO.setBatSinModel(batteryPack.getBatSinModel());
        controlBatterySet.doSet(batterySetVO, BatteryCidEnum._09);
    }
}
