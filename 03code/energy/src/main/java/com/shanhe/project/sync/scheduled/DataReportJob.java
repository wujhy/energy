package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.framework.enums.*;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryPack;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.device.config.service.IBatteryPackService;
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
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private ClientReportService clientReportService;
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private BatteryModuleReportLogAdapterService batteryModuleReportLogAdapterService;

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

            // 上报告警数据
            this.alarmReport(host.getImei());
            // 上报蓄电池历史数据
            this.configPackHistory(host.getImei());
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
    private void alarmReport(String imei) {
        try {
            logger.debug("上报平台告警数据，开始同步");
            List<AlarmLog> alarmLogList = alarmLogService.cacheAlarmList();
            if (alarmLogList.isEmpty()) {
                logger.debug("上报平台告警数据，无告警数据");
                return;
            }
            for (AlarmLog alarmLog : alarmLogList) {
                clientReportService.uploadAlarm(alarmLog, imei);
            }
        } catch (Exception e) {
            logger.error("上报平台告警数据，同步异常：{}", e.getMessage());
        } finally {
            logger.debug("上报平台告警数据，同步完成");
        }
    }

    /**
     * 蓄电池组设备历史
     *
     * @param imei 主机
     */
    private void configPackHistory(String imei) {
        List<BatteryPack> packList = batteryPackService.selectBatteryPackListCache(null);
        if (packList == null || packList.isEmpty()) {
            return;
        }
        for (BatteryPack pack : packList) {
            // 取蓄电池上报数据
            BatteryReportLog log = resolveBatteryReportLog(pack.getPackNum());
            if (!isUsableBatteryReportLog(log)) {
                continue;
            }

            // 上报VO
            ConfigHistoryVo history = new ConfigHistoryVo();
            history.setDevId(Constants.DEFAULT_CONFIG_ID);
            history.setPackNum(pack.getPackNum());

            // 蓄电池组参数
            List<ConfigHistoryItemVo> items = new ArrayList<>();
            for (String key : log.getPackParam().keySet()) {
                items.add(new ConfigHistoryItemVo(key, String.valueOf(log.getPackParam().get(key))));
            }
            history.setListData(items);

            // 单体参数
            history.setListData2(log.getBatteryList());

            // 上报
            clientReportService.uploadData(history, imei);
        }
    }

    /**
     * 解析蓄电池 JSON/TCP 上报数据源。
     *
     * @param packNum 电池组编号
     * @return 蓄电池上报数据
     */
    BatteryReportLog resolveBatteryReportLog(Integer packNum) {
        if (Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpRealtimeSourceEnabled())) {
            BatteryReportLog realtimeLog = batteryModuleReportLogAdapterService.buildReportLog(Constants.DEFAULT_CONFIG_ID, packNum);
            if (isUsableBatteryReportLog(realtimeLog)) {
                return realtimeLog;
            }
        }
        return batteryReportLogService.lastCache(packNum);
    }

    /**
     * 判断蓄电池上报数据是否可用于 JSON/TCP 上报。
     *
     * @param log 蓄电池上报数据
     * @return true 表示可上报
     */
    boolean isUsableBatteryReportLog(BatteryReportLog log) {
        return log != null
                && log.getPackParam() != null
                && !log.getPackParam().isEmpty()
                && log.getBatteryList() != null
                && !log.getBatteryList().isEmpty();
    }

}
