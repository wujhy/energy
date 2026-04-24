package com.shanhe.project.sync.common;

import cn.hutool.core.bean.BeanUtil;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.sync.domain.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 设备转换工具类
 *
 * @author wjh
 * @since 2025/5/30
 */
public class ConfigUtil {

    /**
     * 设置设备参数
     *
     * @param config 本地
     * @param device 远程
     */
    public static void setConfigParam(Config config, DeviceVo device) {
        int type = Integer.parseInt(device.getClassId());
        // 设备信息
        config.setConfigId(device.getDevId());
        config.setName(device.getDevName());
        config.setTmplId((long) type);
        config.setType(type);
        config.setSubType(device.getSubClassId());
        config.setTypeCode(device.getTypeCode());
        config.setPort(device.getParentSn());
        config.setChannel(device.getSonSn());
        config.setStatus(YesNoEnum.YES.getDictValue());

        // 串口信息
        config.setPortType(device.getPortType());
        config.setBaudRate(device.getBaudRate());
        config.setDataBits(device.getBitData());
        config.setStopBits(device.getBitStop());
        config.setParityBits(device.getParityCheck());
        config.setIntervalTime(device.getPollTime());

        // 蓄电池组
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())
                || device.getChildDev() == null || device.getChildDev().isEmpty()) {
            return;
        }

        // 蓄电池
        List<BatteryPack> packList = new ArrayList<>(device.getChildDev().size());
        for (BatteryVo battery : device.getChildDev()) {
            BatteryPack batteryPack = BeanUtil.copyProperties(battery, BatteryPack.class);
            batteryPack.setPackId(battery.getBatPackId());
            batteryPack.setConfigId(config.getConfigId());
            // 状态相反
            batteryPack.setIsEnabled(Objects.equals(battery.getIsEnabled(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            batteryPack.setIsAllowPower(Objects.equals(battery.getIsAllowPower(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            batteryPack.setIsShowConnect(Objects.equals(battery.getIsShowConnect(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            packList.add(batteryPack);
        }
        config.setPackList(packList);
    }

    /**
     * 上报设备参数
     *
     * @param config 本地
     */
    public static DeviceVo uploadConfig(Config config) {
        DeviceVo device = new DeviceVo();
        device.setDevId(config.getConfigId());
        device.setDevName(config.getName());
        device.setClassId(String.valueOf(config.getType()));
        device.setSubClassId(config.getSubType());
        device.setTypeCode(config.getTypeCode());
        device.setParentSn(config.getPort());
        device.setSonSn(config.getChannel());
        device.setStatus(Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())
                ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
        device.setIsDelete(YesNoEnum.YES.getDictValue());

        // 串口信息
        device.setPortType(config.getPortType());
        device.setBaudRate(config.getBaudRate());
        device.setBitData(config.getDataBits());
        device.setBitStop(config.getStopBits());
        device.setParityCheck(config.getParityBits());
        device.setPollTime(config.getIntervalTime());

        // 蓄电池组
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())
                || config.getPackList() == null || config.getPackList().isEmpty()) {
            return device;
        }

        // 蓄电池
        List<BatteryVo> childDev = new ArrayList<>(config.getPackList().size());
        for (BatteryPack batteryPack : config.getPackList()) {
            BatteryVo batteryVo = BeanUtil.copyProperties(batteryPack, BatteryVo.class);
            batteryVo.setBatPackId(batteryPack.getPackId());
            batteryVo.setDevId(config.getConfigId());
            // 状态相反
            batteryVo.setIsEnabled(Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            batteryVo.setIsAllowPower(Objects.equals(batteryPack.getIsAllowPower(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            batteryVo.setIsShowConnect(Objects.equals(batteryPack.getIsShowConnect(), YesNoEnum.YES.getDictValue())
                    ? YesNoEnum.NO.getDictValue() : YesNoEnum.YES.getDictValue());
            batteryVo.setIsDelete(YesNoEnum.YES.getDictValue());
            childDev.add(batteryVo);
        }
        device.setChildDev(childDev);

        return device;
    }
}
