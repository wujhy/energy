package com.shanhe.project.device.config.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.collector.battery.config.BatteryCollectorProperties;
import com.shanhe.project.collector.battery.service.BatteryModuleReportLogAdapterService;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单体电池记录
 *
 * @author wjh
 * @since 2025/7/9
 */
@RestController
@RequestMapping("/battery/log")
public class BatteryReportLogController extends BaseController
{
    @Resource
    private BatteryReportLogService batteryReportLogService;
    @Resource
    private BatteryModuleReportLogAdapterService batteryModuleReportLogAdapterService;
    @Resource
    private BatteryCollectorProperties batteryCollectorProperties;
    @Resource
    private IAlarmLogService alarmLogService;

    /**
     * 单体电池历史记录
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(BatteryReportLog batteryReportLog)
    {
        batteryReportLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        startPage();
        List<BatteryReportLog> list = batteryReportLogService.selectBatteryReportLog(batteryReportLog);
        return getDataTable(list);
    }

    /**
     * 单体电池最新记录
     */
    @GetMapping("/{configId}/{packNum}")
    @ResponseBody
    public AjaxResult detailList(@PathVariable("configId") Long ignoredConfigId, @PathVariable Integer packNum)
    {
        BatteryReportLog log = selectCurrentHasAlarm(packNum);
        if(log!=null){
            //置空数据，实体中已经解析好结构
            log.setPackData(null);
            log.setMonitorData(null);
        }
        return success(log);
    }

    /**
     * 查询当前态详情，并在实时切源开启时优先使用标准实时快照。
     */
    private BatteryReportLog selectCurrentHasAlarm(Integer packNum) {
        if (Boolean.TRUE.equals(batteryCollectorProperties.getJsonTcpRealtimeSourceEnabled())) {
            try {
                BatteryReportLog realtimeLog = batteryModuleReportLogAdapterService.buildReportLog(packNum);
                if (hasRealtimeData(realtimeLog)) {
                    fillAlarmInfo(packNum, realtimeLog);
                    return realtimeLog;
                }
            } catch (Exception e) {
                // 页面详情查询不能因实时适配失败而中断，继续回退旧上报缓存。
            }
        }
        return batteryReportLogService.selectLastHasAlarm(packNum);
    }

    /** 判断标准实时报告是否携带有效组数据或单体数据。 */
    private boolean hasRealtimeData(BatteryReportLog log) {
        if (log == null) {
            return false;
        }
        return (log.getPackParam() != null && !log.getPackParam().isEmpty())
                || (log.getBatteryList() != null && !log.getBatteryList().isEmpty());
    }

    /** 按旧详情接口语义补齐组级和单体告警列表。 */
    private void fillAlarmInfo(Integer packNum, BatteryReportLog log) {
        List<AlarmLog> alarmLogs = alarmLogService.selectBatteryAlarmLogListCache(packNum);
        if (alarmLogs == null) {
            alarmLogs = new ArrayList<>();
        }
        if (log.getBatteryList() != null && !log.getBatteryList().isEmpty()) {
            Map<Integer, List<AlarmLog>> batAlarmMap = alarmLogs.stream()
                    .filter(item -> item.getModelNum() != null)
                    .collect(Collectors.groupingBy(AlarmLog::getModelNum));
            for (BatteryMonitor battery : log.getBatteryList()) {
                if (battery != null) {
                    battery.setAlarmList(batAlarmMap.getOrDefault(battery.getBatNum(), new ArrayList<>()));
                }
            }
        }
        log.setAlarmList(alarmLogs);
        log.setAlarm(alarmLogs.isEmpty() ? 1 : 0);
    }

    /**
     * 删除记录
     */
    @Log(title = "单体电池记录", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        batteryReportLogService.deleteByIds(ids);
        return success();
    }

    /** 导出单体数据。 */
    @PostMapping("/export")
    public AjaxResult export(BatteryReportLog params) {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        batteryReportLogService.export(params);
        return success();
    }

}
