package com.shanhe.project.monitor.operlog.controller;

import java.util.List;

import com.shanhe.project.monitor.operlog.domain.UpgradeSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.monitor.operlog.domain.OperLog;
import com.shanhe.project.monitor.operlog.service.IOperLogService;

/**
 * 操作日志记录
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/operlog")
public class OperlogController extends BaseController
{
    @Autowired
    private IOperLogService operLogService;

    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OperLog operLog)
    {
        startPage();
        List<OperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(OperLog operLog)
    {
        List<OperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<OperLog> util = new ExcelUtil<>(OperLog.class);
        return util.exportExcel(list, "操作日志");
    }

    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        operLogService.deleteOperLogByIds(ids);
        return success();
    }

    @GetMapping("/detail/{operId}")
    public AjaxResult detail(@PathVariable("operId") Long operId)
    {
        return success(operLogService.selectOperLogById(operId));
    }
    
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return success();
    }

    @PostMapping("/sql")
    public AjaxResult sql(@RequestBody UpgradeSql sql)
    {
        return success(operLogService.executeSql(sql.getSql()));
    }

    @GetMapping("/initSql")
    public AjaxResult initSql()
    {
        operLogService.initSql();
        return success();
    }
}
