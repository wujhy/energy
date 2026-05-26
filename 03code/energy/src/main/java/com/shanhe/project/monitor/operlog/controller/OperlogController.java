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
 * @author wjh
 * @since 2026-05-25
 */
@RestController
@RequestMapping("/monitor/operlog")
public class OperlogController extends BaseController
{
    @Autowired
    private IOperLogService operLogService;

    /**
     * 查询操作日志列表
     *
     * @param operLog 查询参数
     * @return 分页数据
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OperLog operLog)
    {
        startPage();
        List<OperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    /**
     * 导出操作日志
     *
     * @param operLog 查询参数
     * @return 导出结果
     */
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(OperLog operLog)
    {
        List<OperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<OperLog> util = new ExcelUtil<>(OperLog.class);
        return util.exportExcel(list, "操作日志");
    }

    /**
     * 批量删除操作日志
     *
     * @param ids 日志ID串
     * @return 操作结果
     */
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        operLogService.deleteOperLogByIds(ids);
        return success();
    }

    /**
     * 查询操作日志详情
     *
     * @param operId 日志ID
     * @return 操作日志详情
     */
    @GetMapping("/detail/{operId}")
    public AjaxResult detail(@PathVariable("operId") Long operId)
    {
        return success(operLogService.selectOperLogById(operId));
    }
    
    /**
     * 清空操作日志
     *
     * @return 操作结果
     */
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return success();
    }

    /**
     * 执行SQL语句
     *
     * @param sql SQL升级对象
     * @return 执行结果
     */
    @PostMapping("/sql")
    public AjaxResult sql(@RequestBody UpgradeSql sql)
    {
        return success(operLogService.executeSql(sql.getSql()));
    }

    /**
     * 执行初始化SQL脚本
     *
     * @return 操作结果
     */
    @GetMapping("/initSql")
    public AjaxResult initSql()
    {
        operLogService.initSql();
        return success();
    }
}
