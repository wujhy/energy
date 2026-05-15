package com.shanhe.project.device.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements IConfigService {

    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;

    private static final Config DEFAULT_CONFIG = buildDefaultConfig();

    @Override
    public Config selectDefaultConfig() {
        return copyDefaultConfig();
    }

    @Override
    public List<Config> selectConfigList() {
        Config config = copyDefaultConfig();
        config.setPackList(batteryPackService.selectBatteryPackListCache(null));
        return wrapConfig(config);
    }

    @Override
    public List<Config> screenConfigList() {
        Config config = copyDefaultConfig();
        List<BatteryPack> packList = new ArrayList<>();
        for (BatteryPack batteryPack : batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue())) {
            if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
                continue;
            }
            batteryPack.setAlarm(alarmLogService.isBatteryAlarmByCache(batteryPack.getPackNum()));
            packList.add(batteryPack);
        }
        config.setPackList(packList);
        config.setAlarm(alarmLogService.isBatteryAlarmByCache(null));
        return wrapConfig(config);
    }

    @Override
    public Config screenConfig() {
        Config config = copyDefaultConfig();
        config.setPackList(batteryPackService.selectBatteryPackListCache(null));
        config.setAlarm(alarmLogService.isBatteryAlarmByCache(null));
        config.setAlarmNum(alarmLogService.batteryAlarmNum());
        return config;
    }

    @Override
    public void updatePack(Config config) {
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }
        List<BatteryPack> oldPackList = batteryPackService.selectBatteryPackListConfigId(null);
        List<BatteryPack> newPackList = config.getPackList();

        this.deleteBatteryPacks(oldPackList, newPackList);
        if (newPackList == null || newPackList.isEmpty()) {
            return;
        }

        Map<Integer, BatteryPack> oldPackMap = oldPackList.stream()
                .filter(batteryPack -> batteryPack.getPackNum() != null)
                .collect(Collectors.toMap(BatteryPack::getPackNum, batteryPack -> batteryPack, (left, right) -> left, LinkedHashMap::new));
        for (BatteryPack batteryPack : newPackList) {
            if (batteryPack == null || batteryPack.getPackNum() == null) {
                continue;
            }
            BatteryPack oldPack = oldPackMap.get(batteryPack.getPackNum());
            batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
            if (oldPack == null) {
                batteryPack.setPackId(IdUtils.getSnowflakeId());
                batteryPackService.insertBatteryPack(batteryPack);
                configAttributeService.insertByTemplateAttribute(batteryPack.getPackNum(), batteryPack.getBatSinModel());
            } else {
                batteryPack.setPackId(oldPack.getPackId());
                batteryPackService.update(batteryPack);
            }
        }
    }

    private void deleteBatteryPacks(List<BatteryPack> oldPackList, List<BatteryPack> packList) {
        if (oldPackList == null || oldPackList.isEmpty()) {
            return;
        }
        List<Integer> packNums = packList == null ? new ArrayList<>() : packList.stream().map(BatteryPack::getPackNum).collect(Collectors.toList());
        List<Long> deletePackIds = new ArrayList<>(oldPackList.size());
        List<Integer> deletePackNums = new ArrayList<>(oldPackList.size());
        for (BatteryPack oldPack : oldPackList) {
            if (oldPack == null || oldPack.getPackNum() == null || packNums.contains(oldPack.getPackNum())) {
                continue;
            }
            deletePackNums.add(oldPack.getPackNum());
            deletePackIds.add(oldPack.getPackId());
        }
        if (!deletePackIds.isEmpty()) {
            batteryPackService.deleteBatteryPackByBatPackIds(deletePackIds);
        }
        if (!deletePackNums.isEmpty()) {
            configAttributeService.deleteConfigAttributeByPackNums(deletePackNums);
            deletePackNums.forEach(packNum -> alarmLogService.alarmFix(packNum, false, null, null));
        }
    }

    private List<Config> wrapConfig(Config config) {
        List<Config> list = new ArrayList<>(1);
        if (config != null) {
            list.add(config);
        }
        return list;
    }

    private static Config copyDefaultConfig() {
        return BeanUtil.copyProperties(DEFAULT_CONFIG, Config.class);
    }

    private static Config buildDefaultConfig() {
        Config config = new Config();
        config.setConfigId(Constants.DEFAULT_CONFIG_ID);
        config.setTmplId(Constants.DEFAULT_TEMPLATE_ID);
        config.setName("蓄电池");
        config.setType(DeviceTypeEnum._1.getDictValue());
        config.setSubType("0");
        config.setSort(1);
        config.setPort(10);
        config.setPortType(1);
        config.setChannel(1);
        config.setBaudRate(115200);
        config.setDataBits(3);
        config.setStopBits(0);
        config.setIntervalTime(5000);
        config.setParityBits(0);
        config.setStatus(YesNoEnum.YES.getDictValue());
        config.setOnline(YesNoEnum.NO.getDictValue());
        return config;
    }
}
