package com.shanhe.project.device.screen.service;

import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.system.user.domain.Index;

import java.util.List;

/**
 * 首页Service接口
 * 
 * @author wjh
 * @since 2024-12-23
 */
public interface ScreenService  {
    /**
     * 首页基本信息
     *
     * @return 首页
     */
    Index main();

    /**
     * 主机信息
     *
     * @return 主机
     */
    Host host();

    /**
     * 设备列表
     *
     * @return 设备
     */
    List<Config> configList();

    /**
     * 设备详情
     *
     * @param configId 设备ID
     * @return 设备
     */
    Config config();

    /**
     * 设备属性列表
     *
     * @param configId 设备ID
     * @param packNum 包号
     * @param screen 是否显示
     * @return 设备属性
     */
    List<ConfigAttributeVO> attribute(Integer packNum, Integer screen);

    /**
     * 设备属性下拉列表
     *
     * @param configId 设备ID
     * @param packNum 包号
     * @param screen 是否显示
     * @param track 是否显示
     * @return 设备属性
     */
    List<ConfigAttributeListVO> attributeSelect(Integer packNum, Integer screen, Integer track);

    /**
     * 电池列表
     *
     * @return 电池
     */
    List<BatteryReportLogIndex> batteryList();

    /**
     * 告警数量
     *
     * @return 告警数量
     */
    Long alarmCount();

}
