package com.shanhe.project.monitor.patrol.controller;

import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplate;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateService;
import com.shanhe.project.sync.service.ClientReportService;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;

import javax.annotation.Resource;
import java.util.List;

/**
 * 巡检模板Controller
 *
 * @author wjh
 * @since 2025/7/1
 */
@RestController
@RequestMapping("/patrol/template")
public class PatrolTemplateController extends BaseController {

    @Resource
    private IPatrolTemplateService patrolTemplateService;
    @Resource
    private ClientReportService clientReportService;

    @PostMapping("/list")
    public TableDataInfo list(PatrolTemplate patrolTemplate) {
        startPage();
        List<PatrolTemplate> list = patrolTemplateService.selectList(patrolTemplate);
        return getDataTable(list);
    }

    @GetMapping(value = "/info/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(patrolTemplateService.selectById(id));
    }

    @Log(title = "新增巡检模板", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody PatrolTemplate patrolTemplate) {
        patrolTemplateService.insert(patrolTemplate);
        return success();
    }

    @Log(title = "修改巡检模板", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody PatrolTemplate patrolTemplate) {
        patrolTemplateService.update(patrolTemplate);
        return success();
    }

    @Log(title = "删除巡检模板", businessType = BusinessType.DELETE)
    @GetMapping("/delete/{id}")
    public AjaxResult remove(@PathVariable("id") Long id) {
        patrolTemplateService.deleteById(id);
        return success();
    }

    @Log(title = "同步巡检模板", businessType = BusinessType.UPDATE)
    @GetMapping("/sync")
    public AjaxResult sync() {
        try {
            clientReportService.getPatrolTemplate();
        } catch (Exception e) {
            return error(String.format("同步巡检清单失败：%s", e.getMessage()));
        }
        return success();
    }
}
