package com.shanhe.project.device.alarm.controller;

import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.device.alarm.domain.AlarmLevel;
import com.shanhe.project.device.alarm.service.AlarmLevelService;
import org.springframework.web.bind.annotation.*;
import com.shanhe.framework.aspectj.lang.annotation.Log;

import javax.annotation.Resource;
import java.util.List;

/**
 * 告警级别Controller
 *
 * @author wjh
 * @since 2025/6/11
 */
@RestController
@RequestMapping("/alarm/level")
public class AlarmLevelController extends BaseController {

    @Resource
    private AlarmLevelService alarmLevelService;

    /**
     * 等级列表
     */
    @GetMapping("/list")
    public AjaxResult list(AlarmLevel alarmLevel) {
        List<AlarmLevel> list = alarmLevelService.selectAlarmLevelList(alarmLevel);
        return success(list);
    }

    /**
     * 查询告警级别
     */
    @GetMapping(value = "/info/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(alarmLevelService.selectAlarmLevelById(id));
    }

    /**
     * 新增告警级别
     */
    @Log(title = "新增告警级别", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody AlarmLevel alarmLevel) {
        alarmLevelService.insertAlarmLevel(alarmLevel);
        return success();
    }

    @Log(title = "修改告警级别", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody AlarmLevel alarmLevel) {
        alarmLevelService.updateAlarmLevel(alarmLevel);
        return success();
    }

    @Log(title = "删除告警级别", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        alarmLevelService.deleteByIds(ids);
        return success();
    }
}
