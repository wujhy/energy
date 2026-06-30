package com.shanhe.project.manage.config.controller;

import com.shanhe.framework.aspectj.lang.annotation.Log;
import com.shanhe.framework.enums.BusinessType;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.domain.BatteryPack;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 蓄电池组Controller
 *
 * @author wjh
 * @since 2025/10/17
 */
@RestController
@RequestMapping("/battery/pack")
public class BatteryPackController extends BaseController {
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;

    /**
     * 单体电池历史记录
     */
    @GetMapping("/list")
    @ResponseBody
    public AjaxResult list(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                           @RequestParam(required = false) Integer isEnabled) {
        return success(batteryPackService.selectBatteryPackList(isEnabled));
    }


    /**
     * 新增电池组
     */
    @Log(title = "新增电池组", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult addSave(@RequestBody @Validated(value = BatteryPack.add.class)  BatteryPack pack) {
        batteryPackService.insertBatteryPackNew(pack);
        return success();
    }

    /**
     * 编辑电池组
     */
    @Log(title = "编辑电池组", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult editSave(@RequestBody @Validated(value = BatteryPack.update.class) BatteryPack pack) {
        batteryPackService.updateNew(pack);
        return success();
    }

    /**
     * 删除电池组
     */
    @Log(title = "删除电池组", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult remove(Long id) {
        batteryPackService.deleteBatteryPackByBatPackId(id);
        return success();
    }

    /**
     * 获取电池组详情
     */
    @GetMapping("/detail/{configId}/{packNum}")
    public AjaxResult detail(@PathVariable("configId") Long ignoredConfigId, @PathVariable("packNum") Integer packNum) {
        return success(batteryPackService.selectBatteryInfoByPackNum(packNum));
    }

    /**
     * 获取电池组详情
     */
    @GetMapping("/detail/{packNum}")
    public AjaxResult detail(@PathVariable("packNum") Integer packNum) {
        return success(batteryPackService.selectBatteryInfoByPackNum(packNum));
    }

}
