package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.utils.CacheUtils;
import com.shanhe.framework.enums.*;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.domain.Config;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IConfigService;
import com.shanhe.project.device.history.domain.HistoryLog;
import com.shanhe.project.device.host.domain.Host;
import com.shanhe.project.device.host.service.IHostService;
import com.shanhe.project.sync.domain.ConfigHistoryItemVo;
import com.shanhe.project.sync.domain.ConfigHistoryVo;
import com.shanhe.project.sync.service.ClientReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 上报定时任务
 *
 * @author wjh
 * @since 2025/5/17
 */
@Component
@EnableScheduling
public class DataReportJob {

    protected static Logger logger = LoggerFactory.getLogger(DataReportJob.class);

    @Resource
    private IHostService hostService;
    @Resource
    private IConfigService configService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private BatteryReportLogService batteryReportLogService;

    /** 是否上报 **/
    private boolean isReport = false;

    @Scheduled(cron = "${report.dataReport}")
    public void dataReport() {
        try {
            // 当前上报状态、是否同步上报、是否已建立通道
            if (isReport || !clientReportService.canSend()) {
                return;
            }
            logger.debug("上报平台数据，开始同步");
            // 设置当前为上报状态
            isReport = true;

            Host host = hostService.getDetail();
            // 设置数据上报间隔时间
            int spaceTime = (host.getSpaceTime() != null && host.getSpaceTime() > 10 ? host.getSpaceTime() : 60) - 20;
            Thread.sleep(spaceTime * 1000L);

            // 主机连接状态、已注册
            if (StrUtil.isBlank(host.getImei())) {
                logger.debug("上报平台数据，主机未在线不执行");
                return;
            }

            // 所有缓存的已开启设备
            List<Config> configList = configService.cacheConfigList();
            if (configList.isEmpty()) {
                logger.debug("上报平台数据，无启用的设备");
                return;
            }

            // 上报告警数据
            this.alarmReport(host.getImei(), configList);
            // 上报历史数据
            this.historyReport(host.getImei(), configList);
        } catch (Exception e) {
            logger.error("上报平台数据，同步异常：{}", e.getMessage());
        } finally {
            // 退出上报状态
            isReport = false;
            logger.debug("上报平台数据，同步完成");
        }
    }

    /**
     * 主机告警
     */
    private void hostAlarm(Host host) {
        try {
            AlarmLog alarmLog = alarmLogService.getByCache(host.getHostId(), null, null, HostAlarmItemEnum._1.getCode());
            if (alarmLog == null) {
                return;
            }
            clientReportService.uploadAlarm(alarmLog, host.getImei());
        } catch (Exception e) {
            logger.error("上报平台主机告警数据，同步异常：{}", e.getMessage());
        }
    }

    /**
     * 上报告警数据
     */
    private void alarmReport(String imei, List<Config> configList) {
        try {
            logger.debug("上报平台告警数据，开始同步");
            List<AlarmLog> alarmLogList = alarmLogService.cacheAlarmList();
            if (alarmLogList.isEmpty()) {
                logger.debug("上报平台告警数据，无告警数据");
                return;
            }
            List<Long> configIds = configList.stream().map(Config::getConfigId).collect(Collectors.toList());
            for (AlarmLog alarmLog : alarmLogList) {
                // 告警记录不在启动设备缓存内，则不上报
                if (!configIds.contains(alarmLog.getConfigId())) {
                    continue;
                }
                clientReportService.uploadAlarm(alarmLog, imei);
            }
        } catch (Exception e) {
            logger.error("上报平台告警数据，同步异常：{}", e.getMessage());
        } finally {
            logger.debug("上报平台告警数据，同步完成");
        }
    }

    /**
     * 上报设备历史数据
     */
    private void historyReport(String imei, List<Config> configList) {
        try {
            // 全部属性
            Set<String> keys = CacheUtils.getCacheKeys(CacheKeyEnum.HISTORY.getCache());
            // 循环设备
            for (Config config : configList) {
                // 如果子设备不在线，则不上报
                if (Objects.equals(config.getOnline(), YesNoEnum.NO.getDictValue())) {
                    continue;
                }
                // 处理上报数据
                if (Objects.equals(config.getType(), DeviceTypeEnum._1.getDictValue())) {
                    // 蓄电池
                    this.configPackHistory(imei, config.getConfigId(), config.getPackList(), keys);
                } else {
                    // 普通设备
                    this.configHistory(imei, config.getConfigId(), keys);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 蓄电池组设备历史
     *
     * @param imei 主机
     * @param configId 设备ID
     * @param packList 组
     * @param keys 缓存
     */
    private void configPackHistory(String imei, Long configId, List<BatteryPack> packList, Set<String> keys) {
        if (packList == null || packList.isEmpty()) {
            return;
        }
        for (BatteryPack pack : packList) {
            // 取缓存
            BatteryReportLog log = batteryReportLogService.lastCache(configId, pack.getPackNum());
            if (log == null || log.getPackParam() == null || log.getPackParam().isEmpty()) {
                continue;
            }

            // 上报VO
            ConfigHistoryVo history = new ConfigHistoryVo();
            history.setDevId(configId);
            history.setPackNum(pack.getPackNum());

            // 蓄电池组参数
            List<ConfigHistoryItemVo> items = new ArrayList<>();
            for (String key : log.getPackParam().keySet()) {
                items.add(new ConfigHistoryItemVo(key, (String) log.getPackParam().get(key)));
            }
            history.setListData(items);

            // 单体参数
            history.setListData2(log.getBatteryList());

            // 上报
            clientReportService.uploadData(history, imei);

            /* 组属性
            ConfigHistoryVo history = this.getHistory(configId, pack.getPackNum(), keys);
            if (history == null) {
                continue;
            }
            // 单体属性
            List<BatteryMonitor> listData2 = new ArrayList<>();
            for (int i = 1; i <= pack.getBatSinSize(); i++) {
                BatteryMonitor batteryMonitor = batteryMonitorService.lastCache(configId, pack.getPackNum(), i);
                if (batteryMonitor == null) {
                    continue;
                }
                listData2.add(batteryMonitor);
            }
            history.setListData2(listData2);
            clientReportService.uploadData(history, imei);
            */
        }
    }

    /**
     * 普通设备历史
     *
     * @param imei 主机
     * @param configId 设备ID
     * @param keys 缓存
     */
    private void configHistory(String imei, Long configId, Set<String> keys) {
        ConfigHistoryVo history = this.getHistory(configId, null, keys);
        if (history == null) {
            return;
        }
        clientReportService.uploadData(history, imei);
    }

    /**
     * 取缓存记录
     *
     * @param configId 设备ID
     * @param keys 缓存
     */
    private ConfigHistoryVo getHistory(Long configId, Integer packNum, Set<String> keys) {
        // 历史记录
        List<ConfigHistoryItemVo> items = new ArrayList<>();
        String historyKey = String.format("history:%s:%s:", configId, packNum);
        for (String key : keys) {
            if (StrUtil.startWith(key, historyKey)) {
                Object object = CacheUtils.get(CacheKeyEnum.HISTORY.getCache(), key);
                if (object == null) {
                    continue;
                }
                HistoryLog log = (HistoryLog) object;
                items.add(new ConfigHistoryItemVo(log.getItemCode(), log.getValueInfo()));
            }
        }
        if (items.isEmpty()) {
            return null;
        }
        ConfigHistoryVo history = new ConfigHistoryVo();
        history.setDevId(configId);
        history.setPackNum(packNum);
        history.setListData(items);
        return history;
    }
}
