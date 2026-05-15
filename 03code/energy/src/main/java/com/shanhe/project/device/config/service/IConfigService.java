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

    Config selectDefaultConfig();

    List<Config> selectConfigList();

    List<Config> screenConfigList();

    Config screenConfig();

    void updatePack(Config config);
}
