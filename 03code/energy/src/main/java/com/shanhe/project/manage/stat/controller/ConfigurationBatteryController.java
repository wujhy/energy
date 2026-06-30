package com.shanhe.project.manage.stat.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.project.manage.config.service.IBatteryPackService;
import com.shanhe.project.manage.capacity.service.PreBatteryGroupService;
import com.shanhe.project.manage.stat.domain.DevBatteryMonomer;
import com.shanhe.project.manage.stat.service.IConfigurationBatteryService;
import com.shanhe.project.manage.stat.service.IDevBatteryMonomerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 电池配置控制器
 *
 * @author wjh
 * @since 2026-05-25
 */
@RestController
@RequestMapping("/configuration/battery")
public class ConfigurationBatteryController extends BaseController {
    /** 电池单体配置服务。 */
    @Resource
    private IDevBatteryMonomerService devBatteryMonomerService;
    /** 电池配置服务。 */
    @Resource
    private IConfigurationBatteryService configurationBatteryService;
    /** 电池组服务。 */
    @Resource
    private IBatteryPackService batteryPackService;
    /** 预估电池组服务。 */
    @Resource
    private PreBatteryGroupService preBatteryGroupService;

    /**
     * 查询设备列表
     */
    @GetMapping("/listMonomer")
    public AjaxResult list(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        List<DevBatteryMonomer> list = devBatteryMonomerService.selectList(packNum);
        return success(list);
    }

    /**
     * 内阻警戒线
     */
    @GetMapping("/getResWarnLine")
    public AjaxResult getResWarnLine(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getResWarnLine(packNum));
    }

    /**
     * 温度警戒线
     */
    @GetMapping("/getTempWarnLine")
    public AjaxResult getTempWarnLine(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getTempWarnLine(packNum));
    }

    /**
     * 健康报告
     */
    @GetMapping("/getBatteryHealthReport")
    public AjaxResult getBatteryHealthReport(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        return success(configurationBatteryService.getBatteryHealthReport(packNum));
    }

    /**
     * 电池信息
     */
    @GetMapping("/getBatteryPack")
    public AjaxResult getBatteryPack(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        return success(batteryPackService.selectBatteryInfoByPackNum(packNum));
    }

    /**
     * 电池信息
     */
    @GetMapping("/getVoltageBalance")
    public AjaxResult getVoltageBalance(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        return success(batteryPackService.getVoltageBalance(packNum));
    }
    /**
     * 清除预估容量
     */
    @GetMapping("/clearPreBatteryGroup")
    public AjaxResult clearPreBatteryGroup(@RequestParam(name = "configId", required = false) Long ignoredConfigId, @RequestParam Integer packNum) {
        preBatteryGroupService.deleteByPackNum(packNum);
        preBatteryGroupService.updateCache();
        return success();
    }
}
