package com.shanhe.project.device.config.service;

import java.util.List;

import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.domain.ConfigProtocol;

/**
 * 设备协议Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigProtocolService 
{
    /**
     * 查询设备协议
     *
     * @param protocolId 设备协议主键
     * @return 设备协议
     */
    ConfigProtocol findByProtocolId(Long protocolId);

    /**
     * 查询设备协议
     * 
     * @param protocolId 设备协议主键
     * @return 设备协议
     */
    ConfigProtocol selectConfigProtocolByProtocolId(Long protocolId);

    /**
     * 查询设备协议
     *
     * @param configId 设备
     * @param protocolCode 协议编号
     * @return 设备协议
     */
    ConfigProtocol selectBy(Long configId, String protocolCode);

    /**
     * 查询设备协议列表
     * 
     * @param configProtocol 设备协议
     * @return 设备协议集合
     */
    List<ConfigProtocol> selectConfigProtocolList(ConfigProtocol configProtocol);

    /**
     * 导出配置
     *
     * @param configId 设备id
     * @return 列表
     */
    List<ConfigProtocol> exportByConfigId(Long configId);

    /**
     * 所有开启的存储协议
     *
     * @param config 设备
     */
    void cmdStorageSendByConfig(Config config);

    /**
     * 删除存储协议
     *
     * @param config 设备
     */
    void cmdStorageDelByConfig(Config config);

    /**
     * 新增设备协议
     * 
     * @param configProtocol 设备协议
     * @return 结果
     */
    int insertConfigProtocol(ConfigProtocol configProtocol);

    /**
     * 同步设备协议
     *
     * @param configProtocol 设备协议
     */
    void insertBySync(Config config, ConfigProtocol configProtocol);

    /**
     * 修改设备协议
     * 
     * @param configProtocol 设备协议
     * @return 结果
     */
    int updateConfigProtocol(ConfigProtocol configProtocol);

    /**
     * 批量删除设备协议
     * 
     * @param protocolIds 需要删除的设备协议主键集合
     * @return 结果
     */
    int deleteConfigProtocolByProtocolIds(String protocolIds);

    /**
     * 删除设备协议
     *
     * @param protocolId 协议ID
     */
    void deleteBySync(Long protocolId);

    /**
     * 批量删除设备协议
     *
     * @param configIds 设备id
     */
    void deleteConfigProtocolByConfigIds(String configIds);

    /**
     * 批量删除设备协议
     *
     * @param configIds 设备id
     */
    void deleteConfigProtocolByConfigIds(String[] configIds);

    /**
     * 通过缓存获取协议
     *
     * @param configId 设备id
     * @param code 协议编码
     * @return 属性信息
     */
    ConfigProtocol getCacheBy(Long configId, String code);

    /**
     * 更新全部缓存
     */
    void updateCache();

    /**
     * 更新指定设备下
     *
     * @param configId 设备id
     * @param isUpdate 是否更新
     */
    void updateCache(Long configId, Integer isUpdate);
}
