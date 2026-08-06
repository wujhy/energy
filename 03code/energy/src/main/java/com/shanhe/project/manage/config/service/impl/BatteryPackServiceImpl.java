package com.shanhe.project.manage.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.shanhe.common.constant.Constants;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryModelEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.mapper.BatteryPackMapper;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
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

    /** 电池组映射。 */
    @Resource
    private BatteryPackMapper batteryPackMapper;
    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 配置属性服务。 */
    @Resource
    private IConfigAttributeService configAttributeService;
    /** 电池模块实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;

    CacheKeyEnum packInfoCache = CacheKeyEnum.BATTERY_PACK_INFO;

    /**
     * 根据电池组ID查询电池组
     *
     * @param packId 电池组主键
     * @return 电池组
     */
    @Override
    public BatteryPack selectBatteryPackByPackId(Long packId) {
        return batteryPackMapper.selectBatteryPackByPackId(packId);
    }

    /**
     * 查询电池组列表
     *
     * @param isEnabled 是否启用
     * @return 电池组列表
     */
    @Override
    public List<BatteryPack> selectBatteryPackList(Integer isEnabled) {
        return batteryPackMapper.selectDefaultDeviceBatteryPackList(isEnabled);
    }

    /**
     * 从缓存查询电池组列表
     *
     * @param isEnabled 是否启用
     * @return 电池组列表
     */
    @Override
    public List<BatteryPack> selectBatteryPackListCache(Integer isEnabled) {
        List<BatteryPack> list = collectFromCache(isEnabled);
        if (!list.isEmpty()) {
            return list;
        }
        updateCache();
        return collectFromCache(isEnabled);
    }

    private List<BatteryPack> collectFromCache(Integer isEnabled) {
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
        list.sort(Comparator.comparing(BatteryPack::getPackNum, Comparator.nullsLast(Integer::compareTo)));
        return list;
    }

    private BatteryPack copyPack(BatteryPack batteryPack) {
        return BeanUtil.copyProperties(batteryPack, BatteryPack.class);
    }

    /**
     * 根据电池组编号查询电池组信息
     *
     * @param packNum 电池组编号
     * @return 电池组
     */
    @Override
    public BatteryPack selectBatteryInfoByPackNum(Integer packNum) {
        String key = String.format(packInfoCache.getKey(), packNum);
        Object log = CacheUtils.get(packInfoCache.getCache(), key);
        if (log != null) {
            return (BatteryPack) log;
        }
        BatteryPack batteryPack = batteryPackMapper.selectBatteryInfoByPackNum(packNum);
        if (batteryPack != null) {
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
        return batteryPack;
    }

    /**
     * 新增电池组
     *
     * @param batteryPack 电池组
     */
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

        String key = String.format(packInfoCache.getKey(), batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
    }

    /**
     * 批量导入电池组
     *
     * @param list 电池组列表
     */
    @Override
    public void importBatteryPack(List<BatteryPack> list) {
        list.forEach(batteryPack -> batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID));
        batteryPackMapper.importBatteryPack(list);
        for (BatteryPack batteryPack : list) {
            String key = String.format(packInfoCache.getKey(), batteryPack.getPackNum());
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
    }

    /**
     * 更新电池组
     *
     * @param batteryPack 电池组
     */
    @Override
    public void update(BatteryPack batteryPack) {
        batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        batteryPackMapper.update(batteryPack);

        String key = String.format(packInfoCache.getKey(), batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        if (Objects.equals(batteryPack.getIsEnabled(), YesNoEnum.NO.getDictValue())) {
            alarmLogService.alarmFix(batteryPack.getPackNum(), false, null, null);
            String prefix = String.format("alarm:%s:", batteryPack.getPackNum());
            for (String alarmKey : CacheUtils.getCacheKeys(CacheKeyEnum.ALARM.getCache())) {
                if (alarmKey.startsWith(prefix)) {
                    CacheUtils.remove(CacheKeyEnum.ALARM.getCache(), alarmKey);
                }
            }
        }
    }
    /** 删除默认设备所有电池组 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDefaultDevicePacks() {
        List<BatteryPack> batteryPacks = batteryPackMapper.selectDefaultDeviceBatteryPackList(null);
        if (batteryPacks == null || batteryPacks.isEmpty()) {
            return;
        }
        List<Long> packIds = new ArrayList<>();
        for (BatteryPack batteryPack : batteryPacks) {
            if (batteryPack != null && batteryPack.getPackId() != null) {
                packIds.add(batteryPack.getPackId());
            }
        }
        deleteBatteryPackByBatPackIds(packIds);
    }
    /**
     * 根据电池组ID批量删除
     *
     * @param packIds 电池组ID列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatteryPackByBatPackIds(List<Long> packIds) {
        if (packIds == null || packIds.isEmpty()) {
            return;
        }
        List<Integer> packNums = new ArrayList<>();
        List<BatteryPack> batteryPacks = batteryPackMapper.selectBatteryPackByPackIds(packIds);
        if (batteryPacks != null) {
            for (BatteryPack batteryPack : batteryPacks) {
                if (batteryPack != null && batteryPack.getPackNum() != null) {
                    packNums.add(batteryPack.getPackNum());
                }
            }
        }
        for (Integer packNum : packNums) {
            alarmLogService.deleteBatteryAlarmLogByPackNum(packNum);
            configAttributeService.deleteConfigAttributeByPackNums(Lists.newArrayList(packNum));
            realtimeSnapshotService.evict(packNum);
        }
        batteryPackMapper.deleteBatteryPackByBatPackIds(packIds);
        updateCache();
        configAttributeService.updateCache(YesNoEnum.YES.getDictValue());
    }
    /** 更新电池组缓存 */
    @Override
    public void updateCache() {
        // 属性键
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(packInfoCache.getCache());

        // 所有启用的配置属性
        List<BatteryPack> list = batteryPackMapper.selectAllBattery();
        for (BatteryPack attribute : list) {
            attribute.setConfigId(Constants.DEFAULT_CONFIG_ID);
            String key = String.format(packInfoCache.getKey(), attribute.getPackNum());
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

    /**
     * 获取电池组浮充电压平衡值
     *
     * @param packNum 电池组编号
     * @return 浮充电压值
     */
    @Override
    public Integer getVoltageBalance(Integer packNum) {
        BatteryPack batteryPack = selectBatteryInfoByPackNum(packNum);
        BatteryModelEnum batteryModelEnum = BatteryModelEnum.find(batteryPack.getBatSinModel());

        return batteryPack.getBatSinSize() <= 24 ? batteryModelEnum.getFloatingVoltage24Below() : batteryModelEnum.getFloatingVoltage24Above();
    }

    /**
     * 根据电池组ID删除电池组（含关联数据清理）
     *
     * @param id 电池组主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatteryPackByBatPackId(Long id) {
        if (id == null) {
            return;
        }
        this.deleteBatteryPackByBatPackIds(Lists.newArrayList(id));
    }
    /**
     * 更新电池组（含校验）
     *
     * @param batteryPack 电池组
     */
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

    /**
     * 新增电池组（含校验）
     *
     * @param batteryPack 电池组
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatteryPackNew(BatteryPack batteryPack) {
        batteryPack.setConfigId(Constants.DEFAULT_CONFIG_ID);
        if (batteryPack.getPackNum() > 4) {
            throw new ServiceException("请选择正确的蓄电池组！");
        }

        batteryPack.setPackId(IdUtils.getSnowflakeId());
        List<BatteryPack> batteryPacks = selectBatteryPackList(null);
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

    /**
     * 获取电池组最大单体数量
     *
     * @param packNum 电池组编号
     * @return 最大单体数量
     */
    @Override
    public Integer getBatteryMaxNumber(Integer packNum) {
        return batteryPackMapper.getBatteryMaxNumber(packNum);
    }
}
