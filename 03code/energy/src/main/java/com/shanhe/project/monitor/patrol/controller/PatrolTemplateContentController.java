package com.shanhe.project.monitor.patrol.controller;

import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.monitor.patrol.domain.PatrolTemplateContent;
import com.shanhe.project.monitor.patrol.service.IPatrolTemplateContentService;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;

import javax.annotation.Resource;
import java.util.List;

/**
 * 巡检模板内容
 *
 * @author wjh
 * @since 2025/7/1
 */
@RestController
@RequestMapping("/patrol/template/content")
public class PatrolTemplateContentController extends BaseController {

    @Resource
    private IPatrolTemplateContentService patrolTemplateContentService;

    @PostMapping("/list")
    public TableDataInfo list(PatrolTemplateContent patrolTemplateContent) {
        startPage();
        List<PatrolTemplateContent> list = patrolTemplateContentService.selectList(patrolTemplateContent);
        return getDataTable(list);
    }

    @GetMapping("/view")
    public TableDataInfo view() {
        return getDataTable(patrolTemplateContentService.viewList());
    }

    @GetMapping(value = "/info/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(patrolTemplateContentService.selectById(id));
    }

    @Log(title = "新增巡检模板内容", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody PatrolTemplateContent patrolTemplateContent) {
        patrolTemplateContentService.insert(patrolTemplateContent);
        return success();
    }

    @Log(title = "修改巡检模板内容", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody PatrolTemplateContent patrolTemplateContent) {
        patrolTemplateContentService.update(patrolTemplateContent);
        return success();
    }

    @Log(title = "删除巡检模板内容", businessType = BusinessType.DELETE)
    @GetMapping("/delete/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        patrolTemplateContentService.deleteById(id);
        return success();
    }
}
