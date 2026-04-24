package com.shanhe.project.device.config.mapper;

import java.util.List;
import com.shanhe.project.device.config.domain.Config;
import org.apache.ibatis.annotations.Param;

/**
 * 设备Mapper接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ConfigMapper 
{
    /**
     * 查询设备
     * 
     * @param configId 设备主键
     * @return 设备
     */
    Config selectConfigByConfigId(Long configId);

    /**
     * 查询设备（通过设备类型、串口、通道号唯一定位设备）
     *
     * @param type 设备类型
     * @param port 设备串口
     * @param channel 设备通道
     * @return 设备
     */
    Config selectConfigBy(@Param("type") Integer type, @Param("port") Integer port, @Param("channel") Integer channel);

    /**
     * 校验是否存在
     *
     * @return 数量
     */
    Long hasName(@Param("configId") Long configId, @Param("port") Integer port, @Param("channel") Integer channel, @Param("type") Integer type);

    /**
     * 查询设备列表
     * 
     * @param config 设备
     * @return 设备集合
     */
    List<Config> selectConfigList(Config config);

    /**
     * 新增设备
     * 
     * @param config 设备
     * @return 结果
     */
    void insertConfig(Config config);

    /**
     * 导入设备
     *
     * @param list 设备
     * @return 结果
     */
    int importList(List<Config> list);

    /**
     * 修改设备
     * 
     * @param config 设备
     * @return 结果
     */
    int updateConfig(Config config);

    /**
     * 修改设备
     *
     * @param config 设备
     * @return 结果
     */
    int updatePost(Config config);

    /**
     * 删除设备
     * 
     * @param configId 设备主键
     * @return 结果
     */
    int deleteConfigByConfigId(Long configId);

    /**
     * 批量删除设备
     * 
     * @param configIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteConfigByConfigIds(String[] configIds);

    /**
     * 设备上线
     */
    void online(Long configId);

    /**
     * 设备下线
     */
    void offline(Long configId);
}
