package com.shanhe.project.device.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.DeviceTypeEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.opt.service.OptLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfigServiceImpl implements IConfigService {

    protected static Logger logger = LoggerFactory.getLogger(IConfigService.class);

    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private OptLogService optLogService;

    CacheKeyEnum configCache = CacheKeyEnum.CONFIG;
    private static final Config DEFAULT_CONFIG = buildDefaultConfig();

    @Override
    public Config selectDefaultConfig() {
        Config config = copyDefaultConfig();
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            config.setPackList(batteryPackService.selectBatteryPackListConfigId(null));
        }
        return config;
    }

    @Override
    public Config getCache() {
        try {
            Config config = (Config) CacheUtils.get(configCache.getCache(), configCache.getKey());
            return config == null ? this.selectDefaultConfig() : config;
        } catch (Exception e) {
            return this.selectDefaultConfig();
        }
    }

    @Override
    public List<Config> selectConfigList() {
        return wrapConfig(this.selectDefaultConfig());
    }

    @Override
    public List<Config> reportConfigList() {
        return wrapConfig(this.selectDefaultConfig());
    }

    @Override
    public List<Config> screenConfigList() {
        Config config = copyDefaultConfig();
        if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            List<BatteryPack> packList = new ArrayList<>();
            for (BatteryPack batteryPack : batteryPackService.selectBatteryPackListConfigId(YesNoEnum.YES.getDictValue())) {
                if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
                    continue;
                }
                batteryPack.setAlarm(alarmLogService.isBatteryAlarmByCache(batteryPack.getPackNum()));
                packList.add(batteryPack);
            }
            config.setPackList(packList);
        }
        config.setAlarm(alarmLogService.isBatteryAlarmByCache(null));
        return wrapConfig(config);
    }

    @Override
    public Config screenConfig() {
        Config config = this.selectDefaultConfig();
        config.setAlarm(alarmLogService.isBatteryAlarmByCache(null));
        config.setAlarmNum(alarmLogService.batteryAlarmNum());
        return config;
    }

    @Override
    public void updateCache() {
        try {
            Config config = this.selectDefaultConfig();
            CacheUtils.put(configCache.getCache(), configCache.getKey(), config);
            Set<String> oldKeys = CacheUtils.getCacheKeys(configCache.getCache());
            for (String key : oldKeys) {
                if (!Objects.equals(key, configCache.getKey())) {
                    CacheUtils.remove(configCache.getCache(), key);
                }
            }
        } catch (Exception e) {
            logger.error("更新设备缓存失败", e);
        }

        try {
            configAttributeService.updateCache();
        } catch (Exception e) {
            logger.error("更新设备属性缓存失败", e);
        }

        try {
            optLogService.updateCache();
        } catch (Exception e) {
            logger.error("更新设备操作日志缓存失败", e);
        }
    }

    @Override
    public void updateExtend(Map<String, Object> map) {
        Map<String, Object> mapAll = DEFAULT_CONFIG.getExtend3() != null && !DEFAULT_CONFIG.getExtend3().isEmpty()
                ? JSON.parseObject(DEFAULT_CONFIG.getExtend3())
                : new HashMap<>();
        mapAll.putAll(map);
        DEFAULT_CONFIG.setExtend3(JSON.toJSONString(mapAll));
    }

    @Override
    public Map<String, Object> getExtend() {
        return DEFAULT_CONFIG.getExtend3() != null && !DEFAULT_CONFIG.getExtend3().isEmpty()
                ? JSON.parseObject(DEFAULT_CONFIG.getExtend3())
                : null;
    }

    @Override
    public void updatePack(Config config) {
        this.updatePack(config, true);
    }

    public void updatePack(Config config, boolean syncAttribute) {
        if (!Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
            return;
        }
        List<BatteryPack> oldPackList = batteryPackService.selectBatteryPackListConfigId(null);
        List<Long> oldPackIds = oldPackList.stream().map(BatteryPack::getPackId).collect(Collectors.toList());
        List<BatteryPack> newPackList = config.getPackList();

        if (newPackList == null || newPackList.isEmpty()) {
            if (!oldPackList.isEmpty()) {
                batteryPackService.deleteBatteryPackByBatPackIds(oldPackIds);
                List<Integer> deletePackNums = new ArrayList<>();
                oldPackList.forEach(oldPack -> {
                    alarmLogService.alarmFix(oldPack.getPackNum(), false, null, null);
                    deletePackNums.add(oldPack.getPackNum());
                });

                if (!deletePackNums.isEmpty()) {
                    configAttributeService.deleteConfigAttributeByPackNums(deletePackNums);
                }
            }
            return;
        }

        Set<Integer> deletePackNums = new HashSet<>();
        List<Long> deletePackIds = new ArrayList<>();
        for (BatteryPack oldPack : oldPackList) {
            boolean needDel = true;
            for (BatteryPack newPack : newPackList) {
                if (Objects.equals(oldPack.getPackNum(), newPack.getPackNum())) {
                    newPack.setPackId(oldPack.getPackId());
                    needDel = false;
                }
            }
            if (needDel) {
                deletePackNums.add(oldPack.getPackNum());
                deletePackIds.add(oldPack.getPackId());
            }
        }

        batteryPackService.deleteBatteryPackByBatPackIds(deletePackIds);
        if (!deletePackNums.isEmpty()) {
            configAttributeService.deleteConfigAttributeByPackNums(new ArrayList<>(deletePackNums));
            deletePackNums.forEach(packNum -> alarmLogService.alarmFix(packNum, false, null, null));
        }

        for (BatteryPack batteryPack : newPackList) {
            if (batteryPack.getPackId() == null || !oldPackIds.contains(batteryPack.getPackId())) {
                batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
                batteryPack.setPackId(IdUtils.getSnowflakeId());
                batteryPackService.insertBatteryPack(batteryPack);
                if (syncAttribute) {
                    configAttributeService.insertByTemplateAttribute(batteryPack.getPackNum(), batteryPack.getBatSinModel());
                }
            } else {
                batteryPackService.update(batteryPack);
            }
        }
    }

    private void updateCache(Config config) {
        CacheUtils.put(configCache.getCache(), configCache.getKey(), config);
        configAttributeService.updateCache(YesNoEnum.YES.getDictValue());
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
