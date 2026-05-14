package com.shanhe.project.energy.stat.controller;

import com.shanhe.common.constant.Constants;
import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.page.TableDataInfo;
import com.shanhe.project.energy.stat.domain.StatBatteryBat;
import com.shanhe.project.energy.stat.domain.StatBatteryPack;
import com.shanhe.project.energy.stat.service.IDevBatteryMonomerService;
import com.shanhe.project.energy.stat.service.IStatBatteryBatService;
import com.shanhe.project.energy.stat.service.IStatBatteryPackService;
import com.shanhe.project.energy.stat.service.IStatBatteryResService;
import com.shanhe.project.monitor.server.service.SystemService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author zhoubin
 * @date 2025/8/14
 */
@RestController
@RequestMapping("/stat/battery")
public class StatBatteryController extends BaseController {
    @Resource
    private IStatBatteryResService statBatteryResService;
    @Resource
    private IStatBatteryPackService statBatteryPackService;
    @Resource
    private IStatBatteryBatService statBatteryBatService;
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;

    // 获取内阻报表
    @GetMapping("/getResistanceReport")
    public AjaxResult getResistanceReport(@RequestParam(required = false) Long configId,
                                          @RequestParam Integer packNum) {
        return success(statBatteryResService.getResistanceReport(packNum));
    }

    // 获取内阻报表
    @GetMapping("/listResistance")
    public AjaxResult listResistance(@RequestParam(required = false) Long configId,
                                     @RequestParam Integer packNum,
                                     @RequestParam Integer batNum) {
        return success(statBatteryResService.listResistance(packNum, batNum));
    }

    // 组数据
    @PostMapping("/listPackStat")
    public TableDataInfo listPackStat(StatBatteryPack params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        startPage();
        return getDataTable(statBatteryPackService.selectList(params));
    }

    // 单体数据
    @PostMapping("/listBatStat")
    public TableDataInfo listPackStat(StatBatteryBat params) {
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        startPage();
        return getDataTable(statBatteryBatService.selectList(params));
    }

    // 单体数据
    @GetMapping("/updateMonomer")
    public AjaxResult updateMonomer(@RequestParam(required = false) Long configId,
                                    @RequestParam Integer packNum) {
        devBatteryMonomerService.init(packNum);
        return success();
    }

    // 单体数据
    @PostMapping("/export")
    public AjaxResult export(StatBatteryPack params) {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        params.setConfigId(Constants.DEFAULT_CONFIG_ID);
        statBatteryPackService.export(params);
        return success();
    }

    // 单体数据
    @PostMapping("/exportResistance")
    public AjaxResult exportResistance(StatBatteryPack params) {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        statBatteryResService.export(params.getPackNum(), params.getExportPath());
        return success();
    }
}
