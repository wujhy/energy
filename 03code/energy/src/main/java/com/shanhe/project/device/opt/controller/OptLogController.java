package com.shanhe.project.device.opt.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.device.opt.domain.OptLog;
import com.shanhe.project.device.opt.service.OptLogService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 操作日志
 *
 * @author wjh
 * @since 2025/7/9
 */
@RestController
@RequestMapping("/opt/log")
public class OptLogController extends BaseController
{
    @Resource
    private OptLogService optLogService;

    /**
     * 操作记录
     */
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(OptLog optLog)
    {
        startPage();
        List<OptLog> list = optLogService.select(optLog);
        return getDataTable(list);
    }

    /**
     * 删除记录
     */
    @Log(title = "单体电池记录", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        optLogService.deleteByIds(ids);
        return success();
    }
}
