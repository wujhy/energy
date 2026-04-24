package com.shanhe.project.device.config.mapper;

import java.util.List;
import com.shanhe.project.device.config.domain.ConfigProtocolAttribute;
import org.apache.ibatis.annotations.Param;

/**
 * 设备协议属性映射Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ConfigProtocolAttributeMapper 
{
    /**
     * 查询设备协议属性映射
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 设备协议属性映射
     */
    ConfigProtocolAttribute selectConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId);

    /**
     * 查询设备协议属性映射
     *
     * @param protocolId 设备协议主键
     * @return 设备协议属性映射
     */
    List<ConfigProtocolAttribute> selectConfigProtocolAttributeByProtocolId(Long protocolId);

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
     * 新增设备协议属性映射
     *
     * @param protocolAttributeList 设备协议属性映射
     * @return 结果
     */
    int insertList(@Param("protocolAttributeList") List<ConfigProtocolAttribute> protocolAttributeList);

    /**
     * 修改设备协议属性映射
     * 
     * @param configProtocolAttribute 设备协议属性映射
     * @return 结果
     */
    int updateConfigProtocolAttribute(ConfigProtocolAttribute configProtocolAttribute);

    /**
     * 删除设备协议属性映射
     * 
     * @param protocolAttrId 设备协议属性映射主键
     * @return 结果
     */
    int deleteConfigProtocolAttributeByProtocolAttrId(Long protocolAttrId);

    /**
     * 批量删除设备协议属性映射
     * 
     * @param protocolAttrIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteConfigProtocolAttributeByProtocolAttrIds(String[] protocolAttrIds);

    /**
     * 批量删除设备协议属性映射
     *
     * @param protocolIds 协议id
     */
    void deleteByProtocolIds(String[] protocolIds);

    /**
     * 批量删除设备协议属性映射
     *
     * @param protocolId 协议id
     */
    void deleteByProtocolId(Long protocolId);
}
