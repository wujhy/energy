package com.shanhe.project.device.config.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.spring.SpringUtils;
import com.shanhe.common.utils.uuid.IdUtils;
import com.shanhe.framework.enums.BatteryCidEnum;
import com.shanhe.framework.enums.BatteryModelEnum;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.mapper.BatteryPackMapper;
import com.shanhe.project.device.config.service.IBatteryPackService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.opt.service.ControlBatterySet;
import com.shanhe.project.device.opt.vo.BatterySetVO;
import com.shanhe.project.device.template.domain.TemplateAttribute;
import com.shanhe.project.device.template.mapper.TemplateAttributeMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    private ControlBatterySet controlBatterySet;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private TemplateAttributeMapper templateAttributeMapper;
    @Resource
    private BatteryPackAsync batteryPackAsync;

    CacheKeyEnum packInfoCache = CacheKeyEnum.BATTERY_PACK_INFO;

    @Override
    public BatteryPack selectBatteryPackByPackId(Long packId) {
        return batteryPackMapper.selectBatteryPackByPackId(packId);
    }

    @Override
    public List<BatteryPack> selectBatteryPackListConfigId(Long configId, Integer isEnabled) {
        return batteryPackMapper.selectBatteryPackListConfigId(configId, isEnabled);
    }

    @Override
    public BatteryPack selectBatteryInfoByPackNum(Long configId, Integer packNum) {
        String key = String.format(packInfoCache.getKey(), configId, packNum);
        Object log = CacheUtils.get(packInfoCache.getCache(), key);
        if (log != null) {
            return (BatteryPack) log;
        }
        BatteryPack batteryPack = batteryPackMapper.selectBatteryInfoByPackNum(configId, packNum);
        if (batteryPack != null) {
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
        return batteryPack;
    }

    @Override
    public void insertBatteryPack(BatteryPack batteryPack) {
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

        String key = String.format(packInfoCache.getKey(), batteryPack.getConfigId(), batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
    }

    @Override
    public void importBatteryPack(List<BatteryPack> list) {
        batteryPackMapper.importBatteryPack(list);
        for (BatteryPack batteryPack : list) {
            String key = String.format(packInfoCache.getKey(), batteryPack.getConfigId(), batteryPack.getPackNum());
            CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
        }
    }

    @Override
    public void update(BatteryPack batteryPack) {
        batteryPackMapper.update(batteryPack);

        String key = String.format(packInfoCache.getKey(), batteryPack.getConfigId(), batteryPack.getPackNum());
        CacheUtils.put(packInfoCache.getCache(), key, batteryPack);
    }

    @Override
    public void deleteByConfigIds(String[] configIds) {
        batteryPackMapper.deleteByConfigIds(configIds);
    }

    @Override
    public void deleteBatteryPackByBatPackIds(List<Long> packIds) {
        if (packIds == null || packIds.isEmpty()) {
            return;
        }
        batteryPackMapper.deleteBatteryPackByBatPackIds(packIds);
    }

    @Override
    public void cmdBatteryPack(Long configId, List<BatteryPack> packList) {
        for (int i = 1; i <= 4; i++) {
            try {
                BatterySetVO batterySetVO = new BatterySetVO();
                batterySetVO.setPackNum(i);
                batterySetVO.setConfigId(configId);
                int finalI = i;
                BatteryPack batteryPack = packList.stream().filter(pack -> Objects.equals(pack.getPackNum(), finalI)).findFirst().orElse(null);
                if (batteryPack != null) {
                    batterySetVO.setBatCapacity(batteryPack.getBatCapacity());
                    batterySetVO.setBatSinSize(batteryPack.getBatSinSize());
                    batterySetVO.setBatSinModel(batteryPack.getBatSinModel());
                } else {
                    batterySetVO.setBatCapacity(0D);
                    batterySetVO.setBatSinSize(0);
                    batterySetVO.setBatSinModel(0);
                }
                controlBatterySet.doSet(batterySetVO, BatteryCidEnum._09);
            } catch (Exception e) {
                log.error("设置电池组 {} 失败：{}", i, e.getMessage());
            }
        }
    }

    @Override
    public void updateCache() {
        // 属性键
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(packInfoCache.getCache());

        // 所有启用的配置属性
        List<BatteryPack> list = batteryPackMapper.selectAllBattery();
        for (BatteryPack attribute : list) {
            String key = String.format(packInfoCache.getKey(), attribute.getConfigId(), attribute.getPackNum());
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
    public Integer getVoltageBalance(Long configId, Integer packNum) {
        BatteryPack batteryPack = selectBatteryInfoByPackNum(configId, packNum);
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
        IConfigService configService = SpringUtils.getBean(IConfigService.class);
        Config config = configService.selectConfigByConfigId(batteryPack.getConfigId());
        if (config == null) {
            return;
        }
        // 报警修复
        alarmLogService.alarmFix(batteryPack.getConfigId(), batteryPack.getPackNum(), false, null, null);

        // 删除属性
        configAttributeService.deleteConfigAttributeByPackNums(config.getConfigId(), Lists.newArrayList(batteryPack.getPackNum()));

        // 删除包
        batteryPackMapper.deleteBatteryPackByBatPackIds(Lists.newArrayList(id));
        // 更新缓存
        updateCache();

        // 属性缓存
        if (Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
            // 更新属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
        } else {
            // 删除属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.NO.getDictValue());
        }

        batteryPackAsync.del(batteryPack, config);
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
        IConfigService configService = SpringUtils.getBean(IConfigService.class);
        Config config = configService.selectConfigByConfigId(batteryPack.getConfigId());
        if (config == null) {
            return;
        }
        update(batteryPack);

        // 属性缓存
        if (Objects.equals(config.getStatus(), YesNoEnum.YES.getDictValue())) {
            // 更新属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.YES.getDictValue());
        } else {
            // 删除属性
            configAttributeService.updateCache(config.getConfigId(), YesNoEnum.NO.getDictValue());
        }

        batteryPackAsync.updateBatteryCmd(config, batteryPack);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertBatteryPackNew(BatteryPack batteryPack) {
        IConfigService configService = SpringUtils.getBean(IConfigService.class);
        Config config = configService.selectConfigByConfigId(batteryPack.getConfigId());
        if (config == null) {
            throw new ServiceException("设备数据不存在！");
        }
        if (batteryPack.getPackNum() > 4) {
            throw new ServiceException("请选择正确的蓄电池组！");
        }

        batteryPack.setPackId(IdUtils.getSnowflakeId());
        List<BatteryPack> batteryPacks = selectBatteryPackListConfigId(batteryPack.getConfigId(), null);
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
        this.insertAttribute(config, batteryPack.getPackNum(), batteryPack.getBatSinModel());
        // 指令
        batteryPackAsync.updateBatteryCmd(config, batteryPack);
    }

    @Override
    public Integer getBatteryMaxNumber(Long configId, Integer packNum) {
        return batteryPackMapper.getBatteryMaxNumber(configId, packNum);
    }


    /**
     * 同步更新属性
     *
     * @param config 配置id
     */
    private void insertAttribute(Config config, Integer packNum, Integer model) {
        if (config.getTmplId() == null) {
            return;
        }
        // 同步创建属性
        TemplateAttribute query = new TemplateAttribute();
        query.setTmplId(config.getTmplId());
        query.setModel(model);
        List<TemplateAttribute> templateAttributeList = templateAttributeMapper.selectTemplateAttributeList(query);
        if (templateAttributeList == null || templateAttributeList.isEmpty()) {
            return;
        }

        List<ConfigAttribute> configAttributeList = new ArrayList<>(templateAttributeList.size());
        for (TemplateAttribute templateAttribute : templateAttributeList) {
            ConfigAttribute configAttribute = BeanUtil.copyProperties(templateAttribute, ConfigAttribute.class);
            configAttribute.setConfigAttrId(IdUtils.getSnowflakeId());
            configAttribute.setConfigId(config.getConfigId());
            configAttribute.setPackNum(packNum);
            configAttributeList.add(configAttribute);
        }
        configAttributeService.insertBatchConfigAttribute(configAttributeList);
    }

}
