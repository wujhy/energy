package com.shanhe.project.device.alarm.controller;

import java.util.List;

import com.shanhe.common.constant.Constants;
import com.shanhe.common.utils.text.Convert;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.shanhe.project.device.alarm.domain.AlarmLog;
import com.shanhe.project.device.alarm.service.IAlarmLogService;

import javax.annotation.Resource;

/**
 * 设备历史记录Controller
 * 
 * @author wjh
 * @since 2024-12-31
 */
@Controller
@RequestMapping("/device/alarm")
public class AlarmLogController extends BaseController
{
    @Resource
    private IAlarmLogService alarmLogService;

    /**
     * 查询设备历史记录列表
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(AlarmLog alarmLog)
    {
        alarmLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        startPage();
        List<AlarmLog> list = alarmLogService.selectAlarmLogList(alarmLog);
        return getDataTable(list);
    }

    /**
     * 导出设备历史记录列表
     */
    @Log(title = "设备告警记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(AlarmLog alarmLog)
    {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        alarmLog.setConfigId(Constants.DEFAULT_CONFIG_ID);
        alarmLogService.export(alarmLog);
        return success();
    }

    /**
     * 新增保存设备历史记录
     */
    @Log(title = "设备告警记录", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@RequestBody AlarmLog alarmLog)
    {
        alarmLogService.insertAlarmLog(alarmLog);
        return success();
    }

    /**
     * 修改设备告警记录
     */
    @GetMapping("/info/{alarmId}")
    @ResponseBody
    public AjaxResult info(@PathVariable("alarmId") Long alarmId)
    {
        return success(alarmLogService.selectAlarmLogByAlarmId(alarmId));
    }

    /**
     * 修改保存设备告警记录
     */
    @Log(title = "设备告警记录", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(AlarmLog alarmLog)
    {
        alarmLogService.shiedAlarmLog(alarmLog);
        return success();
    }

    /**
     * 删除设备告警记录
     */
    @Log(title = "设备告警记录", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        alarmLogService.deleteAlarmLogByAlarmIds(ids);
        return success();
    }

    /**
     * 查询设备列表
     */
    @GetMapping("/getCurrentAlarmCount")
    @ResponseBody
    public AjaxResult list(@RequestParam String itemCode) {
        return success(alarmLogService.getCurrentIsAlarm(itemCode));
    }

    @GetMapping("/clear")
    @ResponseBody
    public AjaxResult clear(@RequestParam(name = "configId", required = false) Long ignoredConfigId) {
        String[] configIdArr = Convert.toStrArray(String.valueOf(Constants.DEFAULT_CONFIG_ID));
        alarmLogService.deleteAlarmLogByConfigIds(configIdArr);
        alarmLogService.updateCache();
        return success();
    }

    /**
     * 删除设备告警记录
     */
    @Log(title = "设备告警记录", businessType = BusinessType.DELETE)
    @PostMapping( "/removeAll")
    @ResponseBody
    public AjaxResult removeAll()
    {
        alarmLogService.deleteALL();
        return success();
    }
}
