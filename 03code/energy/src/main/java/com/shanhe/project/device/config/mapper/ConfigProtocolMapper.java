package com.shanhe.project.device.config.mapper;

import com.shanhe.project.device.config.domain.ConfigProtocol;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备协议Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ConfigProtocolMapper 
{
    /**
     * 查询设备协议
     * 
     * @param protocolId 设备协议主键
     * @return 设备协议
     */
    ConfigProtocol selectConfigProtocolByProtocolId(Long protocolId);

    /**
     * 查询设备协议列表
     * 
     * @param configProtocol 设备协议
     * @return 设备协议集合
     */
    List<ConfigProtocol> selectConfigProtocolList(ConfigProtocol configProtocol);

    /**
     * 查询设备协议列表
     *
     * @param configId 设备id
     * @return 设备协议集合
     */
    List<ConfigProtocol> selectByConfigId(Long configId);

    /**
     * 查询设备协议
     */
    ConfigProtocol selectBy(@Param("configId") Long configId, @Param("protocolCode") String protocolCode);

    /**
     * 校验是否存在
     *
     * @param configProtocol 设备信息
     * @return 数量
     */
    Long hasNum(ConfigProtocol configProtocol);

    /**
     * 新增设备协议
     * 
     * @param configProtocol 设备协议
     * @return 结果
     */
    int insertConfigProtocol(ConfigProtocol configProtocol);

    /**
     * 新增设备协议
     *
     * @param list 设备协议
     * @return 结果
     */
    int insertList(@Param("list") List<ConfigProtocol> list);

    /**
     * 修改设备协议
     * 
     * @param configProtocol 设备协议
     * @return 结果
     */
    int updateConfigProtocol(ConfigProtocol configProtocol);

    /**
     * 删除设备协议
     * 
     * @param protocolId 设备协议主键
     * @return 结果
     */
    int deleteConfigProtocolByProtocolId(Long protocolId);

    /**
     * 批量删除设备协议
     * 
     * @param protocolIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteConfigProtocolByProtocolIds(String[] protocolIds);

    /**
     * 查询设备协议列表
     *
     * @param configIds 设备id
     * @return 设备协议集合
     */
    List<ConfigProtocol> selectConfigProtocolByConfigIds(String[] configIds);
}
