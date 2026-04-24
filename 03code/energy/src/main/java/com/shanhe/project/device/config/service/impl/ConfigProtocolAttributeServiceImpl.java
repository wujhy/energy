package com.shanhe.project.device.config.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.shanhe.project.device.config.mapper.ConfigProtocolAttributeMapper;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import com.shanhe.project.device.config.service.IConfigProtocolAttributeService;
import com.shanhe.common.utils.text.Convert;

import javax.annotation.Resource;

/**
 * 设备协议属性映射Service业务层处理
 * 
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class ConfigProtocolAttributeServiceImpl implements IConfigProtocolAttributeService 
{
    @Resource
    private ConfigProtocolAttributeMapper configProtocolAttributeMapper;

    /**
     * 查询设备协议属性映射
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 设备协议属性映射
     */
    @Override
    public ConfigProtocolAttribute selectConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId)
    {
        return configProtocolAttributeMapper.selectConfigProtocolAttributeByProtocolAttrId(protocolAttrId);
    }

    /**
     * 查询设备协议属性映射列表
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 设备协议属性映射
     */
    @Override
    public List<ConfigProtocolAttribute> selectConfigProtocolAttributeList(ConfigProtocolAttribute configProtocolAttribute)
    {
        return configProtocolAttributeMapper.selectConfigProtocolAttributeList(configProtocolAttribute);
    }

    /**
     * 新增设备协议属性映射
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 结果
     */
    @Override
    public int insertConfigProtocolAttribute(ConfigProtocolAttribute configProtocolAttribute)
    {
        return configProtocolAttributeMapper.insertConfigProtocolAttribute(configProtocolAttribute);
    }

    /**
     * 修改设备协议属性映射
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 结果
     */
    @Override
    public int updateConfigProtocolAttribute(ConfigProtocolAttribute configProtocolAttribute)
    {
        return configProtocolAttributeMapper.updateConfigProtocolAttribute(configProtocolAttribute);
    }

    /**
     * 批量删除设备协议属性映射
     * 
     * @param protocolAttrIds 需要删除的设备协议属性映射主键
     * @return 结果
     */
    @Override
    public int deleteConfigProtocolAttributeByProtocolAttrIds(String protocolAttrIds)
    {
        return configProtocolAttributeMapper.deleteConfigProtocolAttributeByProtocolAttrIds(Convert.toStrArray(protocolAttrIds));
    }

    /**
     * 删除设备协议属性映射信息
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 结果
     */
    @Override
    public int deleteConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId)
    {
        return configProtocolAttributeMapper.deleteConfigProtocolAttributeByProtocolAttrId(protocolAttrId);
    }
}
