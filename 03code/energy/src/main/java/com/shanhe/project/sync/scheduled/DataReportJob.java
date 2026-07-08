package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.domain.BatteryReportLog;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.host.domain.Host;
import com.shanhe.project.manage.host.service.IHostService;
import com.shanhe.project.sync.domain.ConfigHistoryItemVo;
import com.shanhe.project.sync.domain.ConfigHistoryVo;
import com.shanhe.project.sync.service.ClientReportService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@EnableScheduling
public class DataReportJob {

    @Resource
    private IHostService hostService;
    @Resource
    private IBatteryPackService batteryPackService;
    @Resource
    private IAlarmLogService alarmLogService;
    @Resource
    private ClientReportService clientReportService;
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
            log.debug("上报平台数据，开始同步");
            // 设置当前为上报状态
            isReport = true;

            Host host = hostService.getDetail();
            // 设置数据上报间隔时间
            int spaceTime = (host.getSpaceTime() != null && host.getSpaceTime() > 10 ? host.getSpaceTime() : 60) - 20;
            Thread.sleep(spaceTime * 1000L);

            // 主机连接状态、已注册
            if (StrUtil.isBlank(host.getImei())) {
                log.debug("上报平台数据，主机未在线不执行");
                return;
            }

            // 上报告警数据
            this.alarmReport(host.getImei());
            // 上报蓄电池历史数据
            this.configPackHistory(host.getImei());
        } catch (Exception e) {
            log.error("上报平台数据，同步异常：{}", e.getMessage());
        } finally {
            // 退出上报状态
            isReport = false;
            log.debug("上报平台数据，同步完成");
        }
    }

    /** 上报告警数据 */
    private void alarmReport(String imei) {
        try {
            log.debug("上报平台告警数据，开始同步");
            List<AlarmLog> alarmLogList = alarmLogService.cacheAlarmList();
            if (alarmLogList.isEmpty()) {
                log.debug("上报平台告警数据，无告警数据");
                return;
            }
            for (AlarmLog alarmLog : alarmLogList) {
                clientReportService.uploadAlarm(alarmLog, imei);
            }
        } catch (Exception e) {
            log.error("上报平台告警数据，同步异常：{}", e.getMessage());
        } finally {
            log.debug("上报平台告警数据，同步完成");
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
            Map<String, Object> packParam = log.getPackParam();
            if (packParam != null) {
                for (String key : packParam.keySet()) {
                    items.add(new ConfigHistoryItemVo(key, String.valueOf(packParam.get(key))));
                }
            }
            history.setListData(items);

            // 单体参数
            history.setListData2(log.getBatteryList());

            // 上报
            clientReportService.uploadData(history, imei);
        }
    }

    /** 解析蓄电池 JSON/TCP 上报数据源。 */
    BatteryReportLog resolveBatteryReportLog(Integer packNum) {
        return batteryModuleReportLogAdapterService.currentOrLastCache(packNum, true);
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
