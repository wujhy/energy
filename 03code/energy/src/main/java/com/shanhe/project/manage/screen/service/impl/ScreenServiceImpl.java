package com.shanhe.project.manage.screen.service.impl;

import cn.hutool.core.date.DateUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.YesNoEnum;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.*;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    /** 电池模组实时快照服务。 */
    @Resource
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;
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
        return realtimeBatteryList();
    }

    private List<BatteryReportLogIndex> realtimeBatteryList() {
        List<BatteryPack> packs = batteryPackService.selectBatteryPackListCache(YesNoEnum.YES.getDictValue());
        if (packs == null || packs.isEmpty()) {
            return new ArrayList<>();
        }

        List<BatteryReportLogIndex> result = new ArrayList<>();
        for (BatteryPack pack : packs) {
            if (pack == null || pack.getPackNum() == null) {
                continue;
            }
            BatteryReportLogIndex index = buildRealtimeIndex(pack);
            if (index != null) {
                result.add(index);
            }
        }
        result.sort(Comparator.comparingInt(BatteryReportLogIndex::getPackNum));
        return result;
    }

    private BatteryReportLogIndex buildRealtimeIndex(BatteryPack pack) {
        BatteryModuleRealtimeSnapshot snapshot = realtimeSnapshotService == null
                ? null : realtimeSnapshotService.getCachedSnapshot(pack.getPackNum());
        BatteryModuleGroupRealtime group = snapshot == null ? null : snapshot.getGroup();
        Map<String, Object> packParam = buildPackParam(group);
        if (group == null || packParam.isEmpty()) {
            return null;
        }
        BatteryReportLogIndex index = new BatteryReportLogIndex();
        index.setPackNum(pack.getPackNum());
        index.setConfigId(pack.getConfigId() == null ? Constants.DEFAULT_CONFIG_ID : pack.getConfigId());
        index.setAlarm(alarmLogService.isAlarmByCache(pack.getPackNum()));
        index.setCreateTime(resolveRealtimeCreateTime(group));
        fillRealtimeFields(index, group);
        index.setPackParam(packParam);
        return index;
    }

    private void fillRealtimeFields(BatteryReportLogIndex index, BatteryModuleGroupRealtime group) {
        index.setPackVoltage(group.getPackVoltage());
        index.setExternalVoltage(group.getExternalVoltage());
        index.setChargeDischargeCurrent(group.getChargeDischargeCurrent());
        index.setFloatCurrent(group.getFloatCurrent());
        index.setEnvironmentTemperature1(group.getEnvironmentTemperature1());
        index.setEnvironmentTemperature2(group.getEnvironmentTemperature2());
        index.setBatteryAvgTemperature(group.getAvgCellTemperature());
        index.setBatteryPackSoc(group.getBatteryPackSoc());
        index.setBatteryPackSoh(group.getBatteryPackSoh());
        index.setBatteryPackStatus(group.getBatteryPackStatus());
        index.setResistanceTestStatus(group.getResistanceTestStatus());
        index.setDeviceWorkStatus(group.getDeviceWorkStatus());
        index.setDeviceWorkIoStatus(group.getDeviceWorkIoStatus());
        index.setDisChargeCapacity(group.getDisChargeCapacity());
        index.setDisChargeDuration(group.getDisChargeDuration());
        index.setResidualDischargeDuration(group.getResidualDischargeDuration());
        index.setBackupDuration(group.getBackupDuration());
        index.setBcapacity(group.getBcapacity());
        index.setCapacity(group.getCapacity());
    }

    private Map<String, Object> buildPackParam(BatteryModuleGroupRealtime group) {
        Map<String, Object> packParam = new LinkedHashMap<>();
        if (group == null) {
            return packParam;
        }
        putIfNotNull(packParam, "packVoltage", group.getPackVoltage());
        putIfNotNull(packParam, "batteryPackOuterVoltage", group.getExternalVoltage());
        putIfNotNull(packParam, "packCurrent", group.getChargeDischargeCurrent());
        putIfNotNull(packParam, "batteryPackFloatCurrent", group.getFloatCurrent());
        putIfNotNull(packParam, "environmentTemperature1", group.getEnvironmentTemperature1());
        putIfNotNull(packParam, "environmentTemperature2", group.getEnvironmentTemperature2());
        putIfNotNull(packParam, "batteryAvgTemperature", group.getAvgCellTemperature());
        putIfNotNull(packParam, "batteryPackSoc", group.getBatteryPackSoc());
        putIfNotNull(packParam, "batteryPackSoh", group.getBatteryPackSoh());
        putIfNotNull(packParam, "batteryPackStatus", group.getBatteryPackStatus());
        putIfNotNull(packParam, "resistanceTestStatus", group.getResistanceTestStatus());
        putIfNotNull(packParam, "deviceWorkStatus", group.getDeviceWorkStatus());
        putIfNotNull(packParam, "deviceWorkIOStatus", group.getDeviceWorkIoStatus());
        putIfNotNull(packParam, "disChargeCapacity", group.getDisChargeCapacity());
        putIfNotNull(packParam, "disChargeDuration", group.getDisChargeDuration());
        putIfNotNull(packParam, "residualDischargeDuration", group.getResidualDischargeDuration());
        putIfNotNull(packParam, "backupDuration", group.getBackupDuration());
        putIfNotNull(packParam, "bcapacity", group.getBcapacity());
        putIfNotNull(packParam, "capacity", group.getCapacity());
        return packParam;
    }

    private Date resolveRealtimeCreateTime(BatteryModuleGroupRealtime group) {
        if (group.getCreateTime() != null) {
            return group.getCreateTime();
        }
        if (group.getLatestGroupUpdateTime() != null) {
            return group.getLatestGroupUpdateTime();
        }
        if (group.getLatestCellUpdateTime() != null) {
            return group.getLatestCellUpdateTime();
        }
        return group.getPollStartedAt();
    }

    private void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
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
