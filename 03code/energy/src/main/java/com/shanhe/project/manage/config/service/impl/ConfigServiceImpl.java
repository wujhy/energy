package com.shanhe.project.manage.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.Config;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
import com.shanhe.project.manage.config.service.IConfigService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备配置Service业务层处理
 *
 * @author wjh
 * @since 2026-05-25
 */
@Service
public class ConfigServiceImpl implements IConfigService {

    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;

    private static final Config DEFAULT_CONFIG = buildDefaultConfig();

    /**
     * 查询默认设备配置
     *
     * @return 设备配置
     */
    @Override
    public Config selectDefaultConfig() {
        return copyDefaultConfig();
    }

    /**
     * 查询设备配置列表
     *
     * @return 设备配置列表
     */
    @Override
    public List<Config> selectConfigList() {
        Config config = copyDefaultConfig();
        config.setPackList(batteryPackService.selectBatteryPackListCache(null));
        return wrapConfig(config);
    }

    /**
     * 查询大屏设备配置列表（含告警状态）
     *
     * @return 设备配置列表
     */
    @Override
    public List<Config> screenConfigList() {
        Config config = copyDefaultConfig();
        List<BatteryPack> packList = new ArrayList<>();
        for (BatteryPack batteryPack : batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue())) {
            if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
                continue;
            }
            batteryPack.setAlarm(alarmLogService.isAlarmByCache(batteryPack.getPackNum()));
            packList.add(batteryPack);
        }
        config.setPackList(packList);
        config.setAlarm(alarmLogService.isAlarmByCache(null));
        return wrapConfig(config);
    }

    /**
     * 查询大屏设备配置（含告警数量）
     *
     * @return 设备配置
     */
    @Override
    public Config screenConfig() {
        Config config = copyDefaultConfig();
        config.setPackList(batteryPackService.selectBatteryPackListCache(null));
        config.setAlarm(alarmLogService.isAlarmByCache(null));
        config.setAlarmNum(alarmLogService.batteryAlarmNum());
        return config;
    }

    /**
     * 更新设备电池组配置
     *
     * @param config 设备配置
     */
    @Override
    public void updatePack(Config config) {
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }
        List<BatteryPack> oldPackList = batteryPackService.selectBatteryPackList(null);
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
