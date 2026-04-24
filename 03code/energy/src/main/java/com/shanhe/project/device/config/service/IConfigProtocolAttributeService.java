package com.shanhe.project.device.config.service;

import java.util.List;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;

/**
 * 设备协议属性映射Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigProtocolAttributeService 
{
    /**
     * 查询设备协议属性映射
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 设备协议属性映射
     */
    ConfigProtocolAttribute selectConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId);

    /**
     * 查询设备协议属性映射列表
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 设备协议属性映射集合
     */
    List<ConfigProtocolAttribute> selectConfigProtocolAttributeList(ConfigProtocolAttribute configProtocolAttribute);

    /**
     * 新增设备协议属性映射
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 结果
     */
    int insertConfigProtocolAttribute(ConfigProtocolAttribute configProtocolAttribute);

    /**
     * 修改设备协议属性映射
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 结果
     */
    int updateConfigProtocolAttribute(ConfigProtocolAttribute configProtocolAttribute);

    /**
     * 批量删除设备协议属性映射
     * 
     * @param protocolAttrIds 需要删除的设备协议属性映射主键集合
     * @return 结果
     */
    int deleteConfigProtocolAttributeByProtocolAttrIds(String protocolAttrIds);

    /**
     * 删除设备协议属性映射信息
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 结果
     */
    int deleteConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId);
}
