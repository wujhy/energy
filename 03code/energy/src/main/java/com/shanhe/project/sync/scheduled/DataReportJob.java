package com.shanhe.project.sync.scheduled;

import cn.hutool.core.util.StrUtil;
import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleRealtimeSnapshot;
import com.shanhe.project.collector.battery.service.BatteryModuleRealtimeSnapshotService;
import com.shanhe.project.manage.alarm.domain.AlarmLog;
import com.shanhe.project.manage.alarm.service.IAlarmLogService;
import com.shanhe.project.manage.config.domain.BatteryPack;
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
    private BatteryModuleRealtimeSnapshotService realtimeSnapshotService;

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
            BatteryModuleRealtimeSnapshot snapshot = resolveRealtimeSnapshot(pack.getPackNum());
            if (!isUsableRealtimeSnapshot(snapshot)) {
                continue;
            }

            ConfigHistoryVo history = new ConfigHistoryVo();
            history.setDevId(Constants.DEFAULT_CONFIG_ID);
            history.setPackNum(pack.getPackNum());
            history.setListData(buildGroupItems(snapshot.getGroup()));
            history.setListData2(snapshot.getCells());

            clientReportService.uploadData(history, imei);
        }
    }

    BatteryModuleRealtimeSnapshot resolveRealtimeSnapshot(Integer packNum) {
        return realtimeSnapshotService == null ? null : realtimeSnapshotService.getCachedSnapshot(packNum);
    }

    boolean isUsableRealtimeSnapshot(BatteryModuleRealtimeSnapshot snapshot) {
        return snapshot != null
                && snapshot.getGroup() != null
                && snapshot.getCells() != null
                && !snapshot.getCells().isEmpty();
    }

    List<ConfigHistoryItemVo> buildGroupItems(BatteryModuleGroupRealtime group) {
        List<ConfigHistoryItemVo> items = new ArrayList<>();
        addItem(items, "packVoltage", groupVoltage(group));
        addItem(items, "packCurrent", group.getChargeDischargeCurrent());
        addItem(items, "batteryPackFloatCurrent", group.getFloatCurrent());
        addItem(items, "batteryPackOuterVoltage", group.getExternalVoltage());
        addItem(items, "environmentTemperature1", group.getEnvironmentTemperature1());
        addItem(items, "environmentTemperature2", group.getEnvironmentTemperature2());
        addItem(items, "maxVoltageBatteryNumber", group.getMaxVoltageBatNum());
        addItem(items, "batteryMaxVoltage", group.getMaxCellVoltage());
        addItem(items, "minVoltageBatteryNumber", group.getMinVoltageBatNum());
        addItem(items, "batteryMinVoltage", group.getMinCellVoltage());
        addItem(items, "batteryAvgVoltage", group.getAvgCellVoltage());
        addItem(items, "batteryVoltageDeviation", group.getBatteryVoltageDeviation());
        addItem(items, "batteryVoltageRange", group.getVoltageRange());
        addItem(items, "maxResistanceBatteryNumber", group.getMaxResistanceBatNum());
        addItem(items, "batteryMaxResistance", group.getMaxInternalResistance());
        addItem(items, "minResistanceBatteryNumber", group.getMinResistanceBatNum());
        addItem(items, "batteryMinEsistance", group.getMinInternalResistance());
        addItem(items, "batteryAvgResistance", group.getAvgInternalResistance());
        addItem(items, "maxTemperatureBatteryNumber", group.getMaxTemperatureBatNum());
        addItem(items, "batteryMaxTemperature", group.getMaxCellTemperature());
        addItem(items, "minTemperatureBatteryNumber", group.getMinTemperatureBatNum());
        addItem(items, "batteryMinTemperature", group.getMinCellTemperature());
        addItem(items, "batteryAvgTemperature", group.getAvgCellTemperature());
        addItem(items, "batteryPackSoc", group.getBatteryPackSoc());
        addItem(items, "batteryPackSoh", group.getBatteryPackSoh());
        addItem(items, "residualDischargeDuration", group.getResidualDischargeDuration());
        addItem(items, "batteryPackStatus", group.getBatteryPackStatus());
        addItem(items, "resistanceTestStatus", group.getResistanceTestStatus());
        addItem(items, "deviceWorkStatus", group.getDeviceWorkStatus());
        addItem(items, "deviceWorkIOStatus", group.getDeviceWorkIoStatus());
        return items;
    }

    private Double groupVoltage(BatteryModuleGroupRealtime group) {
        return group.getPackVoltage() != null ? group.getPackVoltage() : group.getExternalVoltage();
    }

    private void addItem(List<ConfigHistoryItemVo> items, String itemCode, Object value) {
        if (value != null) {
            items.add(new ConfigHistoryItemVo(itemCode, String.valueOf(value)));
        }
    }

}
