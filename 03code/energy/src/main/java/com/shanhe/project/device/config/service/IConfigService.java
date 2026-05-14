package com.shanhe.project.device.config.service;

import com.shanhe.project.device.config.domain.Config;

import java.util.List;
import java.util.Map;

/**
 * 设备配置服务。
 *
 * @author wjh
 * @since 2024-12-23
 */
public interface IConfigService {

    Config selectDefaultConfig();

    Config getCache();

    List<Config> selectConfigList();

    List<Config> reportConfigList();

    List<Config> screenConfigList();

    Config screenConfig();

    void updatePack(Config config);

    void updateCache();

    void updateExtend(Map<String, Object> map);

    Map<String, Object> getExtend();
}
