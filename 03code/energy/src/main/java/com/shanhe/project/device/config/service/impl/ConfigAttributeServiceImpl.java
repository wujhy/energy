package com.shanhe.project.device.config.service.impl;

import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.shanhe.common.exception.ServiceException;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.common.utils.LinearCalculator;
import com.shanhe.framework.enums.CacheKeyEnum;
import com.shanhe.framework.enums.DataTypeEnum;
import com.shanhe.framework.enums.HostAlarmItemEnum;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.framework.manager.AsyncTaskManager;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.ConfigAttributeListVO;
import com.shanhe.project.device.config.domain.ConfigAttributeVO;
import com.shanhe.project.device.opt.service.ControlBattery;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.config.mapper.ConfigAttributeMapper;
import com.shanhe.project.device.config.domain.ConfigAttribute;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.common.utils.text.Convert;

import javax.annotation.Resource;

/**
 * 设备属性Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Slf4j
@Service
public class ConfigAttributeServiceImpl implements IConfigAttributeService 
{
    @Resource
    private ConfigAttributeMapper configAttributeMapper;
    private IAlarmLogService alarmLogService;
    @Resource
    private ControlBattery controlBattery;
    @Resource
    private ClientReportService clientReportService;

    CacheKeyEnum attributeCache = CacheKeyEnum.ATTRIBUTE;

    /**
     * 查询设备属性
     * 
     * @param configAttrId 设备属性主键
     * @return 设备属性
     */
    @Override
    public ConfigAttribute selectConfigAttributeByConfigAttrId(Long configAttrId)
    {
        return configAttributeMapper.selectConfigAttributeByConfigAttrId(configAttrId);
    }

    @Override
    public List<ConfigAttribute> selectByConfigId(Long configId) {
        return configAttributeMapper.selectByConfigId(configId);
    }

    /**
     * 查询设备属性列表
     * 
     * @param configAttribute 设备属性
     * @return 设备属性
     */
    @Override
    public List<ConfigAttribute> selectConfigAttributeList(ConfigAttribute configAttribute)
    {
        return configAttributeMapper.selectConfigAttributeList(configAttribute);
    }

    @Override
    public List<ConfigAttributeVO> viewList(ConfigAttribute configAttribute) {
        List<ConfigAttribute> list = configAttributeMapper.selectConfigAttributeList(configAttribute);
        if (list == null || list.isEmpty()) {
            return null;
        }

        // 告警、值
        return list.stream().map(attribute -> {
            ConfigAttributeVO vo = BeanUtil.copyProperties(attribute, ConfigAttributeVO.class);
            // 告警状态
            vo.setAlarm(alarmLogService.isAlarm(attribute));
            // 通讯状态没有历史值
            if (StrUtil.equals(vo.getCode(), HostAlarmItemEnum._6.getCode())) {
                return vo;
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ConfigAttributeListVO> selectList(ConfigAttribute configAttribute) {
        List<ConfigAttribute> list = configAttributeMapper.selectConfigAttributeList(configAttribute);
        return BeanUtil.copyToList(list, ConfigAttributeListVO.class);
    }

    @Override
    public int insertConfigAttribute(ConfigAttribute configAttribute, boolean isValid)
    {
        if (isValid) {
            Long result = configAttributeMapper.hasName(configAttribute);
            if (result > 0) {
                throw new ServiceException("属性名或编码已存在！");
            }
            // 补充参数
            this.setParam(configAttribute);
        }
        return configAttributeMapper.insertConfigAttribute(configAttribute);
    }

    @Override
    public void insertBatchConfigAttribute(List<ConfigAttribute> configAttributeList) {
        configAttributeMapper.insertBatchConfigAttribute(configAttributeList);
    }

    @Override
    public void insertByTemplateAttribute(Long configId, Integer packNum, Integer model) {
        configAttributeMapper.insertByTemplateAttribute(configId, packNum, model);
    }

    @Override
    public int updateConfigAttribute(ConfigAttribute configAttributeVO)
    {
        ConfigAttribute configAttribute = selectConfigAttributeByConfigAttrId(configAttributeVO.getConfigAttrId());
        if (configAttribute == null || configAttribute.getConfigId() == null) {
            throw new ServiceException("属性不存在！");
        }
        if (!StrUtil.isBlank(configAttributeVO.getCode())) {
            // 名称或编码修改，校验
            if (!StrUtil.equals(configAttribute.getCode(), configAttributeVO.getCode())
                    || !StrUtil.equals(configAttribute.getName(), configAttributeVO.getName())) {
                Long result = configAttributeMapper.hasName(configAttributeVO);
                if (result > 0) {
                    throw new ServiceException("属性名或编码已存在！");
                }
            }
            // 补充参数
            this.setParam(configAttributeVO);
            if (configAttributeVO.getUnit() == null && configAttribute.getUnit() != null) {
                configAttributeVO.setUnit("");
            }
            if (configAttributeVO.getPoint() == null && configAttribute.getPoint() != null) {
                configAttributeVO.setPoint(0);
            }
            if (configAttributeVO.getProtValue() == null && configAttribute.getProtValue() != null) {
                configAttributeVO.setProtValue(0D);
            }
            BeanUtil.copyProperties(configAttributeVO, configAttribute);
        } else {
            configAttribute.setStatus(configAttributeVO.getStatus());
        }

        // 更新属性
        configAttributeMapper.updateConfigAttribute(configAttribute);
        // 更新缓存（主动更新：下发设备、上报服务）
        this.updateCache(configAttribute.getConfigAttrId(), true, true);

        return 1;
    }

    @Override
    public void updateConfigAttributeBySyn(ConfigAttribute configAttribute) {
        // 更新属性
        configAttributeMapper.updateConfigAttribute(configAttribute);
        // 更新缓存（服务同步：下发设备）
        this.updateCache(configAttribute.getConfigAttrId(), true, false);
    }

    @Override
    public void updateConfigAttributeAlarm(ConfigAttribute configAttribute)
    {
        configAttributeMapper.updateConfigAttribute(configAttribute);
        // 更新缓存（设备同步：上报服务）
        this.updateCache(configAttribute.getConfigAttrId(), false, true);
    }

    /**
     * 批量删除设备属性
     * 
     * @param configAttrIds 需要删除的设备属性主键
     * @return 结果
     */
    @Override
    public int deleteConfigAttributeByConfigAttrIds(String configAttrIds)
    {
        return configAttributeMapper.deleteConfigAttributeByConfigAttrIds(Convert.toStrArray(configAttrIds));
    }

    @Override
    public void deleteConfigAttribute(ConfigAttribute attribute)
    {
        CacheUtils.remove(attributeCache.getCache(), String.format(attributeCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode()));
        configAttributeMapper.deleteConfigAttributeByConfigAttrId(attribute.getConfigAttrId());
    }

    @Override
    public void deleteConfigAttributeByConfigIds(String[] configIds) {
        configAttributeMapper.deleteConfigAttributeByConfigIds(configIds);
    }

    @Override
    public void deleteConfigAttributeByPackNums(Long configId, List<Integer> packNums) {
        configAttributeMapper.deleteConfigAttributeByPackNums(configId, packNums);
    }

    @Override
    public void importAttribute(List<ConfigAttribute> attributeList) {
        configAttributeMapper.importAttribute(attributeList);
    }

    @Override
    public ConfigAttribute getBy(Long configId, Integer packNum, String code) {
        return configAttributeMapper.getBy(configId, packNum, code);
    }

    @Override
    public ConfigAttribute getCacheBy(Long configId, Integer packNum, String code) {
        String key = String.format(attributeCache.getKey(), configId, packNum, code);
        return (ConfigAttribute) CacheUtils.get(attributeCache.getCache(), key);
    }

    @Override
    public ConfigAttribute getCacheBy(Long configId, String code) {
        return this.getCacheBy(configId, null, code);
    }

    @Override
    public String getNameByCache(Long configId, Integer packNum, String code) {
        ConfigAttribute configAttribute = this.getCacheBy(configId, packNum, code);
        return configAttribute != null ? configAttribute.getName() : null;
    }

    @Override
    public void updateCache() {
        // 属性键
        List<String> startKeys = new ArrayList<>();
        Set<String> oldKeys = CacheUtils.getCacheKeys(attributeCache.getCache());

        // 所有启用的配置属性
        List<ConfigAttribute> list = configAttributeMapper.configAttributeList();
        for (ConfigAttribute attribute : list) {
            String key = String.format(attributeCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
            CacheUtils.put(attributeCache.getCache(), key, attribute);
            startKeys.add(key);
        }

        // 删除
        for (String key : oldKeys) {
            if (!startKeys.contains(key)) {
                CacheUtils.remove(attributeCache.getCache(), key);
            }
        }
    }

    @Override
    public void updateCache(Long configId, Integer isUpdate) {
        ConfigAttribute configAttribute = new ConfigAttribute();
        configAttribute.setConfigId(configId);
        configAttribute.setStatus(YesNoEnum.YES.getDictValue());
        List<ConfigAttribute> list = this.selectConfigAttributeList(configAttribute);
        for (ConfigAttribute attribute : list) {
            String key = String.format(attributeCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
            if (Objects.equals(isUpdate, YesNoEnum.YES.getDictValue())) {
                CacheUtils.put(attributeCache.getCache(), key, attribute);
            } else {
                CacheUtils.remove(attributeCache.getCache(), key);
            }
        }
    }

    @Override
    public List<ConfigAttribute> cacheAttributeList() {
        List<ConfigAttribute> configList = new ArrayList<>();
        Set<String> keys = CacheUtils.getCacheKeys(attributeCache.getCache());
        for (String key : keys) {
            configList.add((ConfigAttribute) CacheUtils.get(attributeCache.getCache(), key));
        }
        return configList;
    }

    /**
     * 更新缓存
     */
    private void updateCache(Long configAttrId, boolean needUpdate, boolean needReport) {
        // 更新缓存
        ConfigAttribute attribute = configAttributeMapper.selectConfigAttributeByConfigAttrId(configAttrId);
        String key = String.format(attributeCache.getKey(), attribute.getConfigId(), attribute.getPackNum(), attribute.getCode());
        if (Objects.equals(attribute.getStatus(), YesNoEnum.YES.getDictValue())) {
            CacheUtils.put(attributeCache.getCache(), key, attribute);
        } else {
            CacheUtils.remove(attributeCache.getCache(), key);
        }

        // 异步处理
        AsyncTaskManager.me().execute(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            // 属性不启用不告警时，屏蔽告警记录处理
                            if (Objects.equals(attribute.getStatus(), YesNoEnum.NO.getDictValue())
                                    || Objects.equals(attribute.getAlarmConfig(), YesNoEnum.NO.getDictValue())) {
                                alarmLogService.closeAlarmLog(attribute);
                            }

                            // 下发指令到设备
                            if (needUpdate) {
                                try {
                                    controlBattery.doUpdateParameter(attribute);
                                } catch (Exception e) {
                                    log.error("下发更新属性指令失败：{}", e.getMessage());
                                }
                            }

                            // 上报设备属性信息至服务端
                            if (needReport) {
                                try {
                                    clientReportService.uploadAlarmConfigItem(attribute, null);
                                } catch (Exception e) {
                                    log.error("上报属性失败：{}", e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.error("更新缓存出错：{}", e.getMessage());
                        }
                    }
                }
        );
    }

    /**
     * 补充参数
     */
    private void setParam(ConfigAttribute configAttribute) {
        // 模拟量、补充线性值
        if (Objects.equals(configAttribute.getType(), DataTypeEnum._2.getDictValue())
                && Objects.equals(configAttribute.getStatus(), YesNoEnum.YES.getDictValue())
                && Objects.equals(configAttribute.getIsLinear(), YesNoEnum.YES.getDictValue())) {
            double[] section = LinearCalculator.findIntersection(
                    configAttribute.getMinOrigRange(),configAttribute.getMinTargetRange(),
                    configAttribute.getMaxOrigRange(),configAttribute.getMaxTargetRange());
            if(section != null){
                configAttribute.setSpk(section[0]);
                configAttribute.setSpb(section[1]);
            }
        }
    }
}
