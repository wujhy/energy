package com.shanhe.project.device.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryModelEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.mapper.BatteryPackMapper;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 电池组Service业务层处理
 *
 * @author wjh
 * @since 2024-12-23
 */
@Slf4j
@Service
public class BatteryPackServiceImpl implements IBatteryPackService {

    @Resource
    private BatteryPackMapper batteryPackMapper;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IConfigAttributeService configAttributeService;

    CacheKeyEnum packInfoCache = CacheKeyEnum.BATTERY_PACK_INFO;

    @Override
    public BatteryPack selectBatteryPackByPackId(Long packId) {
        return batteryPackMapper.selectBatteryPackByPackId(packId);
    }

    @Override
    public List<BatteryPack> selectBatteryPackListConfigId(Integer isEnabled) {
        return batteryPackMapper.selectBatteryPackListConfigId(Constants.DEFAULT_CONFIG_ID, isEnabled);
    }

    @Override
    public List<BatteryPack> selectBatteryPackListCache(Integer isEnabled) {
        List<BatteryPack> list = new ArrayList<>();
        for (String key : CacheUtils.getCacheKeys(packInfoCache.getCache())) {
            Object object = CacheUtils.get(packInfoCache.getCache(), key);
            if (!(object instanceof BatteryPack)) {
                continue;
            }
            BatteryPack batteryPack = (BatteryPack) object;
            if (isEnabled != null && !Objects.equals(batteryPack.getIsEnabled(), isEnabled)) {
                continue;
            }
            list.add(copyPack(batteryPack));
        }
        if (!list.isEmpty()) {
            list.sort(Comparator.comparing(BatteryPack::getPackNum, Comparator.nullsLast(Integer::compareTo)));
            return list;
        }
        updateCache();
        for (String key : CacheUtils.getCacheKeys(packInfoCache.getCache())) {
            Object object = CacheUtils.get(packInfoCache.getCache(), key);
            if (!(object instanceof BatteryPack)) {
                continue;
            }
            BatteryPack batteryPack = (BatteryPack) object;
            if (isEnabled != null && !Objects.equals(batteryPack.getIsEnabled(), isEnabled)) {
                continue;
            }
            list.add(copyPack(batteryPack));
        }
        list.sort(Comparator.comparing(BatteryPack::getPackNum, Comparator.nullsLast(Integer::compareTo)));
        return list;
    }

    private BatteryPack copyPack(BatteryPack batteryPack) {
        return BeanUtil.copyProperties(batteryPack, BatteryPack.class);
    }

    @Override
    public BatteryPack selectBatteryInfoByPackNum(Integer packNum) {
        String key = String.format(packInfoCache.getKey(), Constants.DEFAULT_CONFIG_ID, packNum);
        Object log = CacheUtils.get(packInfoCache.getCache(), key);
        if (log != null) {
            return (BatteryPack) log;
        }
        BatteryPack batteryPack = batteryPackMapper.selectBatteryInfoByPackNum(Constants.DEFAULT_CONFIG_ID, packNum);
        if (batteryPack != null) {
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
        return batteryPack;
    }

    @Override
    public void insertBatteryPack(BatteryPack batteryPack) {
        batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        if (null == batteryPack.getIsShowConnect()) {
            batteryPack.setIsShowConnect(YesNoEnum.YES.getDictValue());
        }
        if (null == batteryPack.getIsAllowPower()) {
            batteryPack.setIsAllowPower(YesNoEnum.YES.getDictValue());
        }
        if (null == batteryPack.getIsEnabled()) {
            batteryPack.setIsEnabled(YesNoEnum.YES.getDictValue());
        }
        batteryPackMapper.insertBatteryPack(batteryPack);

        String key = String.format(packInfoCache.getKey(), Constants.DEFAULT_CONFIG_ID, batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
    }

    @Override
    public void importBatteryPack(List<BatteryPack> list) {
        list.forEach(batteryPack -> batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID));
        batteryPackMapper.importBatteryPack(list);
        for (BatteryPack batteryPack : list) {
            String key = String.format(packInfoCache.getKey(), Constants.DEFAULT_CONFIG_ID, batteryPack.getPackNum());
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
    }

    @Override
    public void update(BatteryPack batteryPack) {
        batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        batteryPackMapper.update(batteryPack);

        String key = String.format(packInfoCache.getKey(), Constants.DEFAULT_CONFIG_ID, batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
    }

    @Override
    public void deleteDefaultDevicePacks() {
        batteryPackMapper.deleteByConfigIds(new String[]{String.valueOf(Constants.DEFAULT_CONFIG_ID)});
    }

    @Override
    public void deleteBatteryPackByBatPackIds(List<Long> packIds) {
        if (packIds == null || packIds.isEmpty()) {
            return;
        }
        batteryPackMapper.deleteBatteryPackByBatPackIds(packIds);
    }

    @Override
    public void updateCache() {
        // 属性键
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(packInfoCache.getCache());

        // 所有启用的配置属性
        List<BatteryPack> list = batteryPackMapper.selectAllBattery();
        for (BatteryPack attribute : list) {
            attribute.setConfigId(Constants.DEFAULT_CONFIG_ID);
            String key = String.format(packInfoCache.getKey(), Constants.DEFAULT_CONFIG_ID, attribute.getPackNum());
            CacheUtils.put(packInfoCache.getCache(), key, attribute);
            startKeys.add(key);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(packInfoCache.getCache(), key);
            }
        }
    }

    @Override
    public Integer getVoltageBalance(Integer packNum) {
        BatteryPack batteryPack = selectBatteryInfoByPackNum(packNum);
        BatteryModelEnum batteryModelEnum = BatteryModelEnum.find(batteryPack.getBatSinModel());

        return batteryPack.getBatSinSize() <= 24 ? batteryModelEnum.getFloatingVoltage24Below() : batteryModelEnum.getFloatingVoltage24Above();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatteryPackByBatPackId(Long id) {
        BatteryPack batteryPack = batteryPackMapper.selectBatteryPackByPackId(id);
        if (batteryPack == null) {
            return;
        }
        // 报警修复
        alarmLogService.alarmFix(batteryPack.getPackNum(), false, null, null);

        // 删除属性
        configAttributeService.deleteConfigAttributeByPackNums(Lists.newArrayList(batteryPack.getPackNum()));

        // 删除包
        batteryPackMapper.deleteBatteryPackByBatPackIds(Lists.newArrayList(id));
        // 更新缓存
        updateCache();

        configAttributeService.updateCache(YesNoEnum.YES.getDictValue());

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNew(BatteryPack batteryPack) {
        BatteryPack old = selectBatteryPackByPackId(batteryPack.getPackId());
        if (old == null) {
            throw new ServiceException("数据不存在！");
        }
        if (!Objects.equals(old.getPackNum(), batteryPack.getPackNum())) {
            throw new ServiceException("电池组编号不允许修改！");
        }
        update(batteryPack);

        configAttributeService.updateCache(YesNoEnum.YES.getDictValue());

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatteryPackNew(BatteryPack batteryPack) {
        batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        if (batteryPack.getPackNum() > 4) {
            throw new ServiceException("请选择正确的蓄电池组！");
        }

        batteryPack.setPackId(IdUtils.getSnowflakeId());
        List<BatteryPack> batteryPacks = selectBatteryPackListConfigId(null);
        if (batteryPacks.size() >= 4) {
            throw new ServiceException("最多支持4个蓄电池组！");
        }
        if (batteryPacks.stream().anyMatch(pack -> Objects.equals(pack.getPackNum(), batteryPack.getPackNum()))) {
            throw new ServiceException("蓄电池组编号已存在！");
        }
        if (batteryPack.getIsEnabled() == null) {
            batteryPack.setIsEnabled(YesNoEnum.YES.getDictValue());
        }
        insertBatteryPack(batteryPack);
        // 属性挂电池组
        configAttributeService.insertByTemplateAttribute(batteryPack.getPackNum(), batteryPack.getBatSinModel());
    }

    @Override
    public Integer getBatteryMaxNumber(Integer packNum) {
        return batteryPackMapper.getBatteryMaxNumber(Constants.DEFAULT_CONFIG_ID, packNum);
    }
}
