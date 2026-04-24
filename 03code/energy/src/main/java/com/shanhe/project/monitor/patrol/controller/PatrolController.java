package com.shanhe.project.monitor.patrol.controller;

import com.shanhe.common.utils.poi.ExcelUtil;
import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.monitor.patrol.domain.Patrol;
import com.shanhe.project.monitor.patrol.vo.PatrolVO;
import com.shanhe.project.monitor.patrol.service.IPatrolService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 巡检
 *
 * @author wjh
 * @since 2025/5/16
 */
@RestController
@RequestMapping("/patrol")
public class PatrolController extends BaseController {

    @Resource
    private IPatrolService patrolService;

    /**
     * 查询巡检列表
     */
    @PostMapping("/list")
    public TableDataInfo list(Patrol patrol) {
        startPage();
        List<Patrol> list = patrolService.selectList(patrol);
        return getDataTable(list);
    }

    /**
     * 导出巡检列表
     */
    @PostMapping("/export")
    public void export(HttpServletResponse response, Patrol patrol) {
        List<Patrol> list = patrolService.selectList(patrol);
        ExcelUtil<Patrol> util = new ExcelUtil<>(Patrol. class);
        util.exportExcel(response, list, "巡检数据");
    }

    /**
     * 获取巡检详细信息
     */
    @GetMapping(value = "/info/{pId}")
    public AjaxResult getInfo(@PathVariable("pId") Long pId) {
        return success(patrolService.selectByPid(pId));
    }

    @Log(title = "巡检添加", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(@RequestBody Patrol patrol)
    {
        patrolService.insert(patrol);
        return success();
    }

    @Log(title = "巡检更新", businessType = BusinessType.UPDATE)
    @PostMapping("/update")
    @ResponseBody
    public AjaxResult update(@RequestBody PatrolVO patrol)
    {
        patrolService.update(patrol);
        return success();
    }

    @Log(title = "巡检上报", businessType = BusinessType.UPDATE)
    @PostMapping(value = "/report")
    @ResponseBody
    public AjaxResult report(@RequestBody PatrolVO patrol)
    {
        patrolService.report(patrol);
        return success();
    }

    @Log(title = "巡检删除", businessType = BusinessType.DELETE)
    @GetMapping(value = "/del/{pId}")
    @ResponseBody
    public AjaxResult del(@PathVariable("pId") Long pId)
    {
        patrolService.deleteByPid(pId);
        return success();
    }
}
