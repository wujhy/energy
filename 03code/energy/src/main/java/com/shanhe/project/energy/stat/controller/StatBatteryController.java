package com.shanhe.project.energy.stat.controller;

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
 * 电池统计控制器
 *
 * @author wjh
 * @since 2026-05-25
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

    /**
     * 获取内阻报表
     *
     * @param ignoredConfigId 配置ID（忽略）
     * @param packNum 电池组编号
     * @return 内阻报表数据
     */
    @GetMapping("/getResistanceReport")
    public AjaxResult getResistanceReport(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                                          @RequestParam Integer packNum) {
        return success(statBatteryResService.getResistanceReport(packNum));
    }

    /**
     * 获取单体内阻列表
     *
     * @param ignoredConfigId 配置ID（忽略）
     * @param packNum 电池组编号
     * @param batNum 电池编号
     * @return 内阻列表数据
     */
    @GetMapping("/listResistance")
    public AjaxResult listResistance(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                                     @RequestParam Integer packNum,
                                     @RequestParam Integer batNum) {
        return success(statBatteryResService.listResistance(packNum, batNum));
    }

    /**
     * 查询电池组统计数据
     *
     * @param params 查询参数
     * @return 分页数据
     */
    @PostMapping("/listPackStat")
    public TableDataInfo listPackStat(StatBatteryPack params) {
        startPage();
        return getDataTable(statBatteryPackService.selectList(params));
    }

    /**
     * 查询单体电池统计数据
     *
     * @param params 查询参数
     * @return 分页数据
     */
    @PostMapping("/listBatStat")
    public TableDataInfo listBatStat(StatBatteryBat params) {
        startPage();
        return getDataTable(statBatteryBatService.selectList(params));
    }

    /**
     * 更新单体电池数据
     *
     * @param ignoredConfigId 配置ID（忽略）
     * @param packNum 电池组编号
     * @return 操作结果
     */
    @GetMapping("/updateMonomer")
    public AjaxResult updateMonomer(@RequestParam(name = "configId", required = false) Long ignoredConfigId,
                                    @RequestParam Integer packNum) {
        devBatteryMonomerService.init(packNum);
        return success();
    }

    /**
     * 导出电池组统计数据
     *
     * @param params 导出参数
     * @return 操作结果
     */
    @PostMapping("/export")
    public AjaxResult export(StatBatteryPack params) {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        statBatteryPackService.export(params);
        return success();
    }

    /**
     * 导出内阻统计数据
     *
     * @param params 导出参数
     * @return 操作结果
     */
    @PostMapping("/exportResistance")
    public AjaxResult exportResistance(StatBatteryPack params) {
        if (SystemService.isWin()) {
            return error("WINDOWS 暂不支持");
        }
        statBatteryResService.export(params.getPackNum(), params.getExportPath());
        return success();
    }
}
