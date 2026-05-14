package com.shanhe.project.collector.battery.service;

import com.shanhe.common.constant.Constants;
import com.shanhe.project.collector.battery.model.BatteryCollectorChannelConfig;
import com.shanhe.project.collector.battery.model.BatteryModuleCellRealtime;
import com.shanhe.project.collector.battery.model.BatteryModuleGroupRealtime;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.iot.service.DataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 独立采集模块到旧历史记录的兼容同步服务。
 *
 * @author wjh
 * @since 2026-04-30
 */
@Slf4j
@Service
public class BatteryModuleCompatReportLogSyncService {

    /**
     * 旧上报模型适配服务。
     */
    @Resource
    private BatteryModuleReportLogAdapterService adapterService;

    /**
     * 旧电池上报历史服务。
     */
    @Resource
    private BatteryReportLogService batteryReportLogService;

    /**
     * 旧历史写入间隔判断服务。
     */
    @Resource
    private DataService dataService;

    /**
     * 同步本轮采集结果到旧 dev_battery_report_log 历史链路。
     *
     * @param channelConfig 采集通道配置
     * @param group 轮询结束后的组实时计算结果
     * @param cells 本轮单体实时数据
     */
    public void sync(BatteryCollectorChannelConfig channelConfig,
                     BatteryModuleGroupRealtime group,
                     List<BatteryModuleCellRealtime> cells) {
        if (channelConfig == null || channelConfig.getBatteryGroup() == null) {
            return;
        }
        BatteryReportLog reportLog = adapterService.buildReportLog(
                Constants.DEFAULT_CONFIG_ID, channelConfig.getBatteryGroup(), group, cells);
        if (reportLog == null) {
            return;
        }
        List<BatteryMonitor> batteryList = reportLog.getBatteryList();
        if (batteryList == null || batteryList.isEmpty() || reportLog.getPackParam() == null) {
            return;
        }
        boolean isInsert = dataService.isInsert(channelConfig.getBatteryGroup() + "");
        batteryReportLogService.insert(channelConfig.getBatteryGroup(),
                reportLog.getPackParam(), batteryList, isInsert);
        log.debug("sync battery module realtime to dev_battery_report_log, packNum={}, insert={}",
                channelConfig.getBatteryGroup(), isInsert);
    }
}
