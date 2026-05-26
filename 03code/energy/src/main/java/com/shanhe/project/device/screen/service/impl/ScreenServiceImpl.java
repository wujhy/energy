package com.shanhe.project.device.screen.service.impl;

import cn.hutool.core.date.DateUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.*;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IConfigAttributeService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.device.screen.service.ScreenService;
import com.shanhe.project.system.user.domain.Index;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 首页Service接口
 *
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class ScreenServiceImpl implements ScreenService {

    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;
    @Resource
    private IConfigAttributeService configAttributeService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private BatteryReportLogService batteryReportLogService;

    /**
     * 获取首页主数据
     *
     * @return 首页数据
     */
    @Override
    public Index main() {
        Index index = new Index();
        // 主机信息
        Host host = hostService.getDetail();
        index.setName(host != null ? host.getName() : "");
        index.setVersion(host != null ? host.getSoftVersion() : "");

        // 巡检功能已精简
        // 安全天数
        if (host != null && host.getCreateTime() != null) {
            index.setSafeDays(DateUtil.betweenDay(host.getCreateTime(), new Date(), true));
        } else {
            index.setSafeDays(0L);
        }

        // 报警数
        index.setAlarmDeviceNum(alarmLogService.alarmDeviceNum());
        index.setAlarmNum(alarmLogService.alarmAllNum());

        return index;
    }

    /**
     * 获取主机信息
     *
     * @return 主机信息
     */
    @Override
    public Host host() {
        return hostService.getDetail();
    }

    /**
     * 获取大屏设备配置列表
     *
     * @return 设备配置列表
     */
    @Override
    public List<Config> configList() {
        return configService.screenConfigList();
    }

    /**
     * 获取大屏设备配置
     *
     * @return 设备配置
     */
    @Override
    public Config config() {
        return configService.screenConfig();
    }

    /**
     * 获取大屏设备属性列表
     *
     * @param packNum 电池组编号
     * @param screen 大屏显示标志
     * @return 属性列表
     */
    @Override
    public List<ConfigAttributeVO> attribute(Integer packNum, Integer screen) {
        ConfigAttribute configAttribute = new ConfigAttribute();
        configAttribute.setConfigId(Constants.DEFAULT_CONFIG_ID);
        configAttribute.setPackNum(packNum);
        configAttribute.setStatus(YesNoEnum.YES.getDictValue());
        configAttribute.setScreenDisplay(screen);
        return configAttributeService.viewList(configAttribute);
    }

    /**
     * 获取大屏设备属性选择列表
     *
     * @param packNum 电池组编号
     * @param screen 大屏显示标志
     * @param track 跟踪标志
     * @return 属性列表
     */
    @Override
    public List<ConfigAttributeListVO> attributeSelect(Integer packNum, Integer screen, Integer track) {
        ConfigAttribute configAttribute = new ConfigAttribute();
        configAttribute.setConfigId(Constants.DEFAULT_CONFIG_ID);
        configAttribute.setPackNum(packNum);
        configAttribute.setScreenDisplay(screen);
        configAttribute.setTrack(track);
        configAttribute.setStatus(YesNoEnum.YES.getDictValue());
        return configAttributeService.selectList(configAttribute);
    }

    /**
     * 获取电池组列表索引
     *
     * @return 电池组索引列表
     */
    @Override
    public List<BatteryReportLogIndex> batteryList() {
        return batteryReportLogService.batteryList();
    }

    /**
     * 获取告警数量
     *
     * @return 告警数量
     */
    @Override
    public Long alarmCount() {
        return alarmLogService.alarmNum();
    }

}
