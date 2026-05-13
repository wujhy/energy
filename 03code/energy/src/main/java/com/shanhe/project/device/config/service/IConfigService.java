package com.shanhe.project.device.config.service;

import java.util.List;
import java.util.Map;

import com.shanhe.project.device.config.domain.Config;

/**
 * 设备Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigService 
{
    /**
     * 查询设备
     *
     * @param type 设备类型
     * @param port 串口号
     * @param channel 通道号
     * @return 设备
     */
    Config selectConfigBy(Integer type, Integer port, Integer channel);

    /**
     * 查询设备
     * 
     * @param configId 设备主键
     * @return 设备
     */
    Config selectConfigByConfigId(Long configId);

    /**
     * 取得在线设备
     *
     * @param configId 设备id
     * @return 设备
     */
    Config getOnlineConfig(Long configId);

    /**
     * 取缓存已启用设备
     *
     * @param type 设备类型
     * @param port 端口号
     * @param channel 通道号
     * @return 设备
     */
    Config getCacheBy(String type, String port, String channel);

    /**
     * 取缓存已启用设备
     *
     * @param type 设备类型
     * @param port 端口号
     * @param channel 通道号
     * @return 设备
     */
    Config getCacheBy(Integer type, Integer port, Integer channel);

    /**
     * 查询设备列表
     * 
     * @param config 设备
     * @return 设备集合
     */
    List<Config> selectConfigList(Config config);

    /**
     * 查询设备列表
     *
     * @return 设备集合
     */
    List<Config> reportConfigList();

    /**
     * 查询启用设备列表
     *
     * @return 设备集合
     */
    List<Config> screenConfigList();

    /**
     * 查询缓存中启用设备列表
     *
     * @return 设备集合
     */
    List<Config> cacheConfigList();


    /**
     * 查设备详情
     *
     * @param configId 设备id
     * @return 设备信息
     */
    Config screenConfig(Long configId);

    /**
     * 导出设备列表
     *
     * @param config 设备
     * @return 设备集合
     */
    List<Config> export(Config config);

    /**
     * 导入设备列表
     *
     * @param list 设备列表
     */
    void importConfig(List<Config> list);

    /**
     * 新增设备
     * 
     * @param config 设备
     */
    void insertConfig(Config config);

    /**
     * 新增设备（同步）
     *
     * @param config 设备
     */
    void insertConfigBySync(Config config);

    /**
     * 修改设备
     * 
     * @param config 设备
     * @return 结果
     */
    int updateConfig(Config config);

    /**
     * 修改设备（同步）
     *
     * @param config 设备
     */
    void updateConfigBySync(Config config);

    /**
     * 修改设备串口设置
     *
     * @param config 设备
     */
    void updatePost(Config config);

    /**
     * 更新设备状态
     *
     * @param configId 设备id
     * @param status 状态
     * @return 结果
     */
    int updateStatus(Long configId, Integer status);

    /**
     * 更新配置包
     *
     * @param config 配置
     */
    void updatePack(Config config);

    /**
     * 删除设备
     *
     * @param config 设备
     */
    void deleteConfig(Config config);

    /**
     * 更新缓存
     */
    void updateCache();

    /**
     * 设备上线
     */
    void online(Config config);

    /**
     * 设备下线
     */
    void offline(Config config);

    /**
     * 全部设备下线
     */
    void offlineAll();

    /**
     * 更新扩展字段
     */
    void updateExtend(Map<String, Object> map);

    void updateExtend(Long configId, Map<String, Object> map);

    /**
     * 扩展字段
     */
    Map<String, Object> getExtend();

    Map<String, Object> getExtend(Long configId);

}
