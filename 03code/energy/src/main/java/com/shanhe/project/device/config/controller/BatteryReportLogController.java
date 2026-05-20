package com.shanhe.project.device.config.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.config.domain.BatteryReportLog;
import com.shanhe.project.device.config.service.BatteryReportLogService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

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
        BatteryReportLog log = batteryReportLogService.selectLastHasAlarm(packNum);
        if(log!=null){
            //置空数据，实体中已经解析好结构
            log.setPackData(null);
            log.setMonitorData(null);
        }
        return success(log);
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

    // 单体数据
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
