package com.shanhe.project.device.config.service;

import com.shanhe.project.device.config.domain.Config;

import java.util.List;

/**
 * 设备配置服务。
 *
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigService {

    /**
     * 查询默认设备配置
     *
     * @return 默认设备配置
     */
    Config selectDefaultConfig();

    /**
     * 查询设备配置列表
     *
     * @return 设备配置列表
     */
    List<Config> selectConfigList();

    /**
     * 查询大屏设备配置列表
     *
     * @return 大屏设备配置列表
     */
    List<Config> screenConfigList();

    /**
     * 查询大屏设备配置
     *
     * @return 大屏设备配置
     */
    Config screenConfig();

    /**
     * 更新设备配置电池组信息
     *
     * @param config 设备配置
     */
    void updatePack(Config config);
}
