package com.shanhe.project.device.config.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.config.domain.BatteryMonitor;
import com.shanhe.project.device.config.service.IBatteryMonitorService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 单体电池记录
 *
 * @author wjh
 * @since 2025/3/5
 */
@RestController
@RequestMapping("/battery/monitor")
public class BatteryMonitorController extends BaseController
{
    @Resource
    private IBatteryMonitorService batteryMonitorService;

    /**
     * 单体电池历史记录
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(BatteryMonitor batteryMonitor)
    {
        startPage();
        List<BatteryMonitor> list = batteryMonitorService.selectBatteryMonitor(batteryMonitor);
        return getDataTable(list);
    }

    /**
     * 单体电池最新记录
     */
    @GetMapping("/{configId}/{packNum}")
    @ResponseBody
    public TableDataInfo detailList(@PathVariable Long configId, @PathVariable Integer packNum)
    {
        List<BatteryMonitor> list = batteryMonitorService.selectLast(configId, packNum);
        return getDataTable(list);
    }

    /**
     * 删除记录
     */
    @Log(title = "设备", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        batteryMonitorService.deleteByIds(ids);
        return success();
    }
}
