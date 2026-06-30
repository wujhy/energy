package com.shanhe.project.manage.screen.service.impl;

import cn.hutool.core.date.DateUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.*;
import com.shanhe.project.manage.config.service.BatteryReportLogService;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.config.service.IConfigAttributeService;
import com.shanhe.project.manage.config.service.IConfigService;
import com.shanhe.project.manage.host.domain.Host;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.manage.screen.service.ScreenService;
import com.shanhe.project.manage.user.domain.Index;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 首页Service接口
 *
 * @author wjh
 * @since 2024-12-23
 */
@Service
public class ScreenServiceImpl implements ScreenService {

    /** 网点服务。 */
    @Resource
    private IHostService hostService;
    /** 配置服务。 */
    @Resource
    private IConfigService configService;
    /** 配置属性服务。 */
    @Resource
    private IConfigAttributeService configAttributeService;
    /** 告警日志服务。 */
    @Resource
    private IAlarmLogService alarmLogService;
    /** 电池上报日志服务。 */
    @Resource
    private BatteryReportLogService batteryReportLogService;
    /** 电池采集器配置。 */
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    /** 电池模组上报日志适配服务。 */
    @Resource
    private BatteryModuleReportLogAdapterService batteryModuleReportLogAdapterService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;

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
        if (batteryCollectorProperties != null
                && Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpRealtimeSourceEnabled())) {
            return realtimeBatteryList();
        }
        return batteryReportLogService.batteryList();
    }

    private List<BatteryReportLogIndex> realtimeBatteryList() {
        List<BatteryReportLogIndex> legacyList = batteryReportLogService.batteryList();
        Map<Integer, BatteryReportLogIndex> legacyMap = legacyList == null ? java.util.Collections.emptyMap()
                : legacyList.stream()
                .filter(item -> item != null && item.getPackNum() != null)
                .collect(Collectors.toMap(BatteryReportLogIndex::getPackNum, Function.identity(), (v1, v2) -> v2));

        List<BatteryPack> packs = batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue());
        if (packs == null || packs.isEmpty()) {
            return legacyList == null ? new ArrayList<>() : legacyList;
        }

        List<BatteryReportLogIndex> result = new ArrayList<>();
        for (BatteryPack pack : packs) {
            if (pack == null || pack.getPackNum() == null) {
                continue;
            }
            BatteryReportLogIndex index = buildRealtimeIndex(pack, legacyMap.get(pack.getPackNum()));
            if (index != null) {
                result.add(index);
            }
        }
        result.sort(Comparator.comparingInt(BatteryReportLogIndex::getPackNum));
        return result;
    }

    private BatteryReportLogIndex buildRealtimeIndex(BatteryPack pack, BatteryReportLogIndex fallback) {
        try {
            BatteryReportLog reportLog = batteryModuleReportLogAdapterService.buildReportLog(pack.getPackNum());
            if (reportLog == null || reportLog.getPackParam() == null || reportLog.getPackParam().isEmpty()) {
                return fallback;
            }
            BatteryReportLogIndex index = new BatteryReportLogIndex();
            index.setPackNum(pack.getPackNum());
            index.setConfigId(pack.getConfigId() == null ? Constants.DEFAULT_CONFIG_ID : pack.getConfigId());
            index.setAlarm(alarmLogService.isAlarmByCache(pack.getPackNum()));
            index.setCreateTime(reportLog.getCreateTime());
            index.setPackParam(reportLog.getPackParam());
            return index;
        } catch (Exception ignored) {
            return fallback;
        }
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
